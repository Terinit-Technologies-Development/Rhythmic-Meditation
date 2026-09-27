package com.terinit.rhythmicmeditation.domain.session

import com.terinit.rhythmicmeditation.data.repository.MeditationIntervalRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.SessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationInterval
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequestValidator
import com.terinit.rhythmicmeditation.domain.timing.ElapsedTimeCalculator
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.util.Ids

/**
 * Session lifecycle service.
 *
 * Owns the rules for creating, starting, pausing, resuming, completing, and
 * cancelling meditation sessions, and for accumulating qualified meditation
 * time from monotonic intervals.
 *
 * Design notes:
 * - All transitions are checked against [MeditationSessionStateMachine].
 * - Qualified time is only accumulated when an interval closes (pause /
 *   complete / cancel), so a crash mid-session can never lose more than the
 *   currently open interval, and never double-counts.
 * - Recovery requests are idempotent: re-issuing the same request does not
 *   reset recorded evidence.
 * - Policy (cooldowns, requirements, enforcement) stays in Rhythmic Routine.
 */
class MeditationSessionService(
    private val sessionRepository: MeditationSessionRepository,
    private val intervalRepository: MeditationIntervalRepository,
    private val interruptionRepository: SessionInterruptionEventRepository,
    private val timeProvider: TimeProvider
) {

    /**
     * Creates a session from a Routine recovery request, or replaces the
     * metadata of an existing session with the same id.
     *
     * Semantics:
     * - unknown id            -> new PENDING session
     * - identical request     -> no-op (idempotent), returns existing session
     * - changed request       -> metadata replaced; recorded evidence
     *   (qualified seconds, counts) and a live ACTIVE/PAUSED status survive
     */
    suspend fun createOrReplaceSession(request: MeditationRecoveryRequest): Result<MeditationSession> {
        MeditationRecoveryRequestValidator.validate(request)
            .getOrElse { return Result.failure(it) }

        val kind = MeditationSessionKind.fromWire(request.sessionKind)
            ?: return Result.failure(
                IllegalArgumentException("sessionKind '${request.sessionKind}' is not recognized")
            )

        val existing = sessionRepository.getSession(request.sessionId)
        return if (existing == null) {
            val session = MeditationSession(
                sessionId = request.sessionId,
                protocolVersion = request.protocolVersion,
                kind = kind,
                status = MeditationSessionStatus.PENDING,
                requiredSeconds = request.requiredQualifiedSeconds,
                completedQualifiedSeconds = 0,
                startedAtEpochMs = null,
                completedAtEpochMs = null,
                createdAtEpochMs = request.createdAtEpochMs,
                expiresAtEpochMs = request.expiresAtEpochMs,
                sourceCooldownId = request.sourceCooldownId,
                sourceRhythmicDayId = request.sourceRhythmicDayId,
                interruptionCount = 0,
                pauseCount = 0
            )
            sessionRepository.upsertSession(session)
            Result.success(session)
        } else {
            val isIdentical =
                existing.createdAtEpochMs == request.createdAtEpochMs &&
                    existing.requiredSeconds == request.requiredQualifiedSeconds &&
                    existing.protocolVersion == request.protocolVersion &&
                    existing.kind == kind &&
                    existing.expiresAtEpochMs == request.expiresAtEpochMs

            if (isIdentical) {
                Result.success(existing)
            } else {
                val liveStatus =
                    if (existing.status == MeditationSessionStatus.ACTIVE ||
                        existing.status == MeditationSessionStatus.PAUSED
                    ) {
                        existing.status
                    } else {
                        MeditationSessionStatus.PENDING
                    }
                val replaced = existing.copy(
                    protocolVersion = request.protocolVersion,
                    kind = kind,
                    status = liveStatus,
                    requiredSeconds = request.requiredQualifiedSeconds,
                    createdAtEpochMs = request.createdAtEpochMs,
                    expiresAtEpochMs = request.expiresAtEpochMs,
                    sourceCooldownId = request.sourceCooldownId,
                    sourceRhythmicDayId = request.sourceRhythmicDayId,
                    completedAtEpochMs = null
                )
                sessionRepository.upsertSession(replaced)
                Result.success(replaced)
            }
        }
    }

    /** Creates a locally-owned session (standalone practice, evening, previews). */
    suspend fun createLocalSession(
        kind: MeditationSessionKind,
        requiredSeconds: Int,
        sourceRhythmicDayId: String? = null
    ): Result<MeditationSession> {
        if (requiredSeconds <= 0) {
            return Result.failure(IllegalArgumentException("requiredSeconds must be > 0"))
        }
        val session = MeditationSession(
            sessionId = Ids.newSessionId(),
            protocolVersion = com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol.PROTOCOL_VERSION,
            kind = kind,
            status = MeditationSessionStatus.PENDING,
            requiredSeconds = requiredSeconds,
            completedQualifiedSeconds = 0,
            startedAtEpochMs = null,
            completedAtEpochMs = null,
            createdAtEpochMs = timeProvider.currentTimeMillis(),
            expiresAtEpochMs = null,
            sourceCooldownId = null,
            sourceRhythmicDayId = sourceRhythmicDayId,
            interruptionCount = 0,
            pauseCount = 0
        )
        sessionRepository.upsertSession(session)
        return Result.success(session)
    }

    /** PENDING -> ACTIVE. Idempotent when the session is already ACTIVE. */
    suspend fun startSession(sessionId: String): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.ACTIVE) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.ACTIVE)
            ?: return illegalTransition(session, MeditationSessionStatus.ACTIVE)

        val startedAt = transitioned.startedAtEpochMs ?: timeProvider.currentTimeMillis()
        val running = transitioned.copy(startedAtEpochMs = startedAt)
        sessionRepository.upsertSession(running)
        openInterval(sessionId)
        return Result.success(running)
    }

    /** ACTIVE -> PAUSED. Closes the interval and records the pause. */
    suspend fun pauseSession(
        sessionId: String,
        reason: InterruptionType = InterruptionType.MANUAL_PAUSE
    ): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.PAUSED) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.PAUSED)
            ?: return illegalTransition(session, MeditationSessionStatus.PAUSED)

        closeIntervalAndAccumulate(sessionId)
        sessionRepository.incrementPauseCount(sessionId)
        recordInterruption(sessionId, reason)
        return Result.success(reloadWithEvidence(transitioned))
    }

    /** PAUSED -> ACTIVE. Opens a fresh interval. */
    suspend fun resumeSession(sessionId: String): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.ACTIVE) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.ACTIVE)
            ?: return illegalTransition(session, MeditationSessionStatus.ACTIVE)
        openInterval(sessionId)
        return Result.success(transitioned)
    }

    /** ACTIVE -> COMPLETED. Records qualified time and completion timestamp. */
    suspend fun completeSession(sessionId: String): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.COMPLETED) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.COMPLETED)
            ?: return illegalTransition(session, MeditationSessionStatus.COMPLETED)

        closeIntervalAndAccumulate(sessionId)
        // Reload first: closing the interval just added qualified seconds and
        // the session row must not be overwritten with stale evidence.
        val refreshed = load(sessionId) ?: transitioned
        val completed = refreshed.copy(
            status = MeditationSessionStatus.COMPLETED,
            completedAtEpochMs = timeProvider.currentTimeMillis()
        )
        sessionRepository.upsertSession(completed)
        return Result.success(completed)
    }

    /** -> CANCELLED from any non-terminal state. */
    suspend fun cancelSession(sessionId: String): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.CANCELLED) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.CANCELLED)
            ?: return illegalTransition(session, MeditationSessionStatus.CANCELLED)

        if (session.status == MeditationSessionStatus.ACTIVE) {
            closeIntervalAndAccumulate(sessionId)
        }
        return Result.success(reloadWithEvidence(transitioned))
    }

    /** -> EXPIRED (e.g. a restorative window closed before completion). */
    suspend fun expireSession(sessionId: String): Result<MeditationSession> {
        val session = load(sessionId) ?: return notFound(sessionId)
        if (session.status == MeditationSessionStatus.EXPIRED) {
            return Result.success(session)
        }
        val transitioned = transition(session, MeditationSessionStatus.EXPIRED)
            ?: return illegalTransition(session, MeditationSessionStatus.EXPIRED)

        if (session.status == MeditationSessionStatus.ACTIVE) {
            closeIntervalAndAccumulate(sessionId)
        }
        return Result.success(reloadWithEvidence(transitioned))
    }

    /** Records an interruption event (e.g. call, screen-off, essential access). */
    suspend fun recordInterruption(
        sessionId: String,
        type: InterruptionType,
        note: String? = null
    ): Result<Unit> {
        sessionRepository.incrementInterruptionCount(sessionId)
        interruptionRepository.recordEvent(
            SessionInterruptionEvent(
                sessionId = sessionId,
                type = type,
                occurredAtEpochMs = timeProvider.currentTimeMillis(),
                note = note
            )
        )
        return Result.success(Unit)
    }

    /**
     * Qualified seconds for a session right now: stored evidence plus the
     * still-open interval (monotonic clock).
     */
    suspend fun currentQualifiedSeconds(sessionId: String): Int {
        val session = load(sessionId) ?: return 0
        val open = intervalRepository.getOpenInterval(sessionId)
            ?: return session.completedQualifiedSeconds
        val now = timeProvider.elapsedRealtimeMillis()
        val openSeconds = ElapsedTimeCalculator.closedIntervalSeconds(
            startedElapsedMs = open.startedElapsedRealtimeMs,
            endedElapsedMs = now
        )
        return session.completedQualifiedSeconds + openSeconds
    }

    /** All recorded intervals for a session (evidence / recovery view). */
    suspend fun intervalsFor(sessionId: String): List<MeditationInterval> =
        intervalRepository.getIntervalsForSession(sessionId)

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun load(sessionId: String): MeditationSession? =
        sessionRepository.getSession(sessionId)

    private suspend fun transition(
        session: MeditationSession,
        to: MeditationSessionStatus
    ): MeditationSession? {
        if (!MeditationSessionStateMachine.canTransition(session.status, to)) return null
        sessionRepository.updateStatus(session.sessionId, to)
        return session.copy(status = to)
    }

    private suspend fun openInterval(sessionId: String) {
        intervalRepository.startInterval(
            sessionId = sessionId,
            startedElapsedMs = timeProvider.elapsedRealtimeMillis(),
            startedWallClockMs = timeProvider.currentTimeMillis()
        )
    }

    private suspend fun closeIntervalAndAccumulate(sessionId: String) {
        val open = intervalRepository.getOpenInterval(sessionId) ?: return
        val endedElapsedMs = timeProvider.elapsedRealtimeMillis()
        intervalRepository.closeOpenInterval(
            sessionId = sessionId,
            endedElapsedMs = endedElapsedMs,
            endedWallClockMs = timeProvider.currentTimeMillis()
        )
        val qualified = ElapsedTimeCalculator.closedIntervalSeconds(
            startedElapsedMs = open.startedElapsedRealtimeMs,
            endedElapsedMs = endedElapsedMs
        )
        if (qualified > 0) {
            sessionRepository.addQualifiedSeconds(sessionId, qualified)
        }
    }

    private suspend fun reloadWithEvidence(session: MeditationSession): MeditationSession =
        sessionRepository.getSession(session.sessionId) ?: session

    private fun notFound(sessionId: String): Result<MeditationSession> =
        Result.failure(NoSuchElementException("No session with id '$sessionId'"))

    private fun illegalTransition(
        session: MeditationSession,
        to: MeditationSessionStatus
    ): Result<MeditationSession> =
        Result.failure(
            IllegalStateException(
                "Illegal session transition ${session.status} -> $to for '${session.sessionId}'"
            )
        )
}
