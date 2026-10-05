package com.terinit.rhythmicmeditation.runtime

import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.SessionCheckpointRepository
import com.terinit.rhythmicmeditation.data.repository.SessionTimeCheckpoint
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.domain.session.MorningSessionPolicy
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The meditation runtime: translates Android lifecycle/device events into
 * Pass 1 domain operations and owns qualified-time accounting.
 *
 * Trust rules (the reason this class exists):
 * - Qualified time is DERIVED from monotonic intervals. No UI ticks mutate the
 *   ledger.
 * - Screen-off keeps qualifying; interactive use of another app closes the
 *   interval and pauses ("putting the phone down counts; using the phone
 *   doesn't").
 * - Configuration changes never pause the session.
 * - Process death and reboot can only ever lose time, never invent it: on an
 *   uncertain gap the runtime credits the last persisted checkpoint, closes
 *   the unsafe open tail, marks PAUSED, and requires Resume.
 * - Completion is atomic-ish, idempotent, credited at the exact crossing of
 *   the requirement, and capped at the requirement.
 *
 * Policy (cooldowns, requirements, enforcement) still belongs to Rhythmic
 * Routine. This class owns meditation evidence only.
 */
class MeditationRuntimeController(
    private val sessionService: MeditationSessionService,
    private val sessionRepository: MeditationSessionRepository,
    private val checkpointRepository: SessionCheckpointRepository,
    private val timeProvider: TimeProvider,
    private val screenStateReader: ScreenStateReader,
    private val bootIdentityReader: BootIdentityReader,
    private val scope: CoroutineScope,
    private val checkpointIntervalMs: Long = DEFAULT_CHECKPOINT_INTERVAL_MS
) {
    private val _state = MutableStateFlow(MeditationRuntimeState())
    val state: StateFlow<MeditationRuntimeState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<MeditationRuntimeEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<MeditationRuntimeEvent> = _events.asSharedFlow()

    private val mutex = Mutex()
    private var tickerJob: Job? = null
    private var completionEmittedFor: String? = null

    // ------------------------------------------------------------------
    // Process-start recovery
    // ------------------------------------------------------------------

    /**
     * Called once per process start. If a session was left ACTIVE by a process
     * that no longer exists, only checkpointed time survives: the unsafe open
     * tail is discarded, the session is marked PAUSED, PROCESS_RESTORE is
     * recorded, and the user must Resume.
     */
    suspend fun initializeAfterProcessStart(): RecoveryReport = mutex.withLock {
        val current = sessionRepository.observeActiveSession().first()
        if (current == null) {
            _state.value = MeditationRuntimeState(mode = _state.value.mode)
            return@withLock RecoveryReport(null, restoredToPaused = false, rebootDetected = false)
        }

        if (current.status != MeditationSessionStatus.ACTIVE) {
            adoptSession(current.sessionId)
            return@withLock RecoveryReport(
                sessionId = current.sessionId,
                restoredToPaused = false,
                rebootDetected = false
            )
        }

        // The process died (or the device rebooted) while ACTIVE. We cannot
        // know what happened since the last checkpoint — so we do not guess.
        val checkpoint = checkpointRepository.get(current.sessionId)
        val nowElapsed = timeProvider.elapsedRealtimeMillis()
        val openStart = sessionService.openIntervalStart(current.sessionId)
        val rebootDetected = isRebootDetected(checkpoint?.atElapsedRealtimeMs, openStart, checkpoint?.bootCount, nowElapsed)

        val safeEnd = checkpoint?.atElapsedRealtimeMs
            ?: openStart
            ?: nowElapsed

        val restored = sessionService.conservativelyRestoreToPaused(
            sessionId = current.sessionId,
            safeEndElapsedMs = safeEnd,
            reason = InterruptionType.PROCESS_RESTORE,
            note = if (rebootDetected) "reboot detected; gap not credited" else "process restore; unverified tail discarded"
        ).getOrNull() ?: current

        adoptSession(restored.sessionId)
        _state.value = _state.value.copy(
            recoveryNotice = RecoveryNotice.RestoredToPaused(rebootDetected)
        )
        _events.tryEmit(MeditationRuntimeEvent.RestoredToPaused(restored, rebootDetected))
        return@withLock RecoveryReport(
            sessionId = restored.sessionId,
            restoredToPaused = true,
            rebootDetected = rebootDetected
        )
    }

    /**
     * Reboot guard: elapsedRealtime resets after reboot, so a monotonic value
     * below the persisted baseline means the old interval cannot be compared
     * with the current clock. The boot counter corroborates when available.
     */
    private fun isRebootDetected(
        checkpointElapsedMs: Long?,
        openIntervalStartMs: Long?,
        checkpointBootCount: Int?,
        nowElapsedMs: Long
    ): Boolean {
        val baseline = maxOfNullable(checkpointElapsedMs, openIntervalStartMs)
        val elapsedRegression = baseline != null && nowElapsedMs < baseline
        val bootMismatch = checkpointBootCount != null &&
            bootIdentityReader.bootCount()?.let { it != checkpointBootCount } == true
        return elapsedRegression || bootMismatch
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    /**
     * Starts (or resumes) today's morning meditation.
     *
     * Idempotent per day and per attempt: re-opening the same id never creates
     * a second morning session, and a cancelled attempt starts a fresh session
     * with zero inherited progress.
     */
    suspend fun startMorningSession(): Result<MeditationSession> = mutex.withLock {
        val dayId = rhythmicDayId()
        val prefix = MorningSessionPolicy.idPrefixForDay(dayId)
        val existing = sessionRepository.getSessionsWithIdPrefix(prefix)
        when (MorningSessionPolicy.resolve(existing, dayId)) {
            MorningStatus.COMPLETE -> {
                val done = existing.first { it.status == MeditationSessionStatus.COMPLETED }
                adoptSession(done.sessionId)
                Result.success(done)
            }
            MorningStatus.IN_PROGRESS, MorningStatus.PAUSED -> {
                val current = existing
                    .filter {
                        it.status == MeditationSessionStatus.ACTIVE ||
                            it.status == MeditationSessionStatus.PAUSED ||
                            it.status == MeditationSessionStatus.PENDING
                    }
                    .maxByOrNull { it.createdAtEpochMs }!!
                val started = if (current.status == MeditationSessionStatus.PENDING) {
                    sessionService.startSession(current.sessionId).getOrThrow()
                } else {
                    current
                }
                adoptSession(started.sessionId)
                Result.success(started)
            }
            MorningStatus.REQUIRED -> {
                val sessionId = MorningSessionPolicy.nextSessionId(existing, dayId)
                createAndStart(
                    sessionId = sessionId,
                    kind = MeditationSessionKind.MORNING_REQUIRED,
                    requiredSeconds = MORNING_REQUIRED_SECONDS,
                    rhythmicDayId = dayId
                )
            }
        }
    }

    /**
     * Starts a session with an explicit kind (restorative, evening, free).
     *
     * When [sessionId] is supplied the start is idempotent per id: a live
     * session is re-adopted (or started when still PENDING) instead of
     * creating a duplicate, and a COMPLETED session is returned unchanged. A
     * terminal CANCELLED/EXPIRED/INVALID session is never revived — callers
     * pass a fresh id for a new attempt.
     */
    suspend fun startSession(
        kind: MeditationSessionKind,
        requiredSeconds: Int = MORNING_REQUIRED_SECONDS,
        rhythmicDayId: String? = null,
        sessionId: String? = null
    ): Result<MeditationSession> = mutex.withLock {
        val id = sessionId ?: com.terinit.rhythmicmeditation.util.Ids.newSessionId()
        val existing = sessionRepository.getSession(id)
        if (existing != null) {
            return@withLock when (existing.status) {
                MeditationSessionStatus.PENDING -> {
                    val started = sessionService.startSession(id)
                        .getOrElse { return@withLock Result.failure(it) }
                    adoptSession(started.sessionId)
                    // A fresh session supersedes any previous recovery notice.
                    _state.value = _state.value.copy(recoveryNotice = null)
                    ensureTicker()
                    Result.success(started)
                }
                MeditationSessionStatus.ACTIVE,
                MeditationSessionStatus.PAUSED,
                MeditationSessionStatus.COMPLETED -> {
                    adoptSession(id)
                    Result.success(existing)
                }
                else -> Result.failure(
                    IllegalStateException(
                        "Session '$id' is ${existing.status}; a fresh session id is required"
                    )
                )
            }
        }
        createAndStart(
            sessionId = id,
            kind = kind,
            requiredSeconds = requiredSeconds,
            rhythmicDayId = rhythmicDayId
        )
    }

    private suspend fun createAndStart(
        sessionId: String,
        kind: MeditationSessionKind,
        requiredSeconds: Int,
        rhythmicDayId: String?
    ): Result<MeditationSession> {
        val created = sessionService.createLocalSession(
            kind = kind,
            requiredSeconds = requiredSeconds,
            sourceRhythmicDayId = rhythmicDayId,
            sessionId = sessionId
        ).getOrElse { return Result.failure(it) }

        val started = sessionService.startSession(created.sessionId)
            .getOrElse { return Result.failure(it) }
        adoptSession(started.sessionId)
        // A fresh session supersedes any previous recovery notice.
        _state.value = _state.value.copy(recoveryNotice = null)
        ensureTicker()
        return Result.success(started)
    }

    /** MANUAL_PAUSE (or a caller-supplied reason) closes the interval immediately. */
    suspend fun pause(reason: InterruptionType = InterruptionType.MANUAL_PAUSE): Result<MeditationSession> =
        mutex.withLock {
            val sessionId = currentSessionId() ?: return@withLock Result.failure(
                IllegalStateException("No session to pause")
            )
            val result = sessionService.pauseSession(sessionId, reason)
            if (result.isSuccess) {
                checkpointRepository.save(currentCheckpoint(sessionId))
                adoptSession(sessionId)
            }
            result
        }

    /** Resume opens a fresh monotonic interval. Paused time is never counted. */
    suspend fun resume(): Result<MeditationSession> = mutex.withLock {
        val sessionId = currentSessionId() ?: return@withLock Result.failure(
            IllegalStateException("No session to resume")
        )
        val result = sessionService.resumeSession(sessionId)
        if (result.isSuccess) {
            adoptSession(sessionId)
            _state.value = _state.value.copy(recoveryNotice = null)
            ensureTicker()
        }
        result
    }

    /**
     * Completes at the exact crossing of the requirement and caps the credited
     * total at the requirement. Idempotent; emits [MeditationRuntimeEvent.Completed]
     * exactly once per session.
     */
    suspend fun completeAtTarget(): Result<MeditationSession> = mutex.withLock {
        val sessionId = currentSessionId() ?: return@withLock Result.failure(
            IllegalStateException("No session to complete")
        )
        val result = sessionService.completeAtQualificationTarget(sessionId)
        val completed = result.getOrNull()
        if (completed != null && completed.status == MeditationSessionStatus.COMPLETED) {
            checkpointRepository.delete(sessionId)
            adoptSession(sessionId)
            emitCompletedOnce(completed)
        }
        result
    }

    /**
     * Ends a session before the requirement is met. The interval is closed and
     * the session CANCELLED — cancelled time is history, never transferable
     * credit toward a later session.
     */
    suspend fun cancel(): Result<MeditationSession> = mutex.withLock {
        val sessionId = currentSessionId() ?: return@withLock Result.failure(
            IllegalStateException("No session to cancel")
        )
        val result = sessionService.cancelSession(sessionId)
        val cancelled = result.getOrNull()
        if (cancelled != null && cancelled.status == MeditationSessionStatus.CANCELLED) {
            checkpointRepository.delete(sessionId)
            adoptSession(sessionId)
            _events.tryEmit(MeditationRuntimeEvent.Cancelled(cancelled))
        }
        result
    }

    /** User-facing "End Session": complete when due, otherwise cancel. */
    suspend fun endSessionConfirmed(): Result<MeditationSession> {
        val now = timeProvider.elapsedRealtimeMillis()
        return if (_state.value.isRequirementMet(now)) completeAtTarget() else cancel()
    }

    // ------------------------------------------------------------------
    // Device / lifecycle signals
    // ------------------------------------------------------------------

    /** App returned to the foreground. Never a resume — resuming is deliberate. */
    fun onAppForeground() {
        scope.launch {
            mutex.withLock {
                val sessionId = currentSessionId() ?: return@withLock
                adoptSession(sessionId)
            }
            ensureTicker()
        }
    }

    /**
     * App left the foreground. Two very different cases:
     * - screen not interactive (put the phone down): KEEP QUALIFYING
     * - screen interactive (using another app / a call): CLOSE INTERVAL + PAUSE
     */
    fun onAppBackground() {
        scope.launch {
            if (screenStateReader.isInteractive()) {
                pause(InterruptionType.APP_BACKGROUND)
            } else {
                // Screen-off meditation continues to qualify; just take a safe point.
                checkpointNow()
            }
        }
    }

    /** Screen turned off: meditation keeps qualifying; persist a safe point. */
    fun onScreenOff() {
        scope.launch {
            mutex.withLock {
                val session = _state.value.session ?: return@withLock
                if (session.status == MeditationSessionStatus.ACTIVE) {
                    sessionService.recordInterruption(session.sessionId, InterruptionType.SCREEN_OFF)
                    checkpointRepository.save(currentCheckpoint(session.sessionId))
                }
            }
        }
    }

    fun onScreenOn() {
        scope.launch { checkpointNow() }
    }

    /** Configuration change / Activity recreation: checkpoint only, never pause. */
    fun onHostActivityRecreated() {
        scope.launch { checkpointNow() }
    }

    // ------------------------------------------------------------------
    // Derived time + checkpoints
    // ------------------------------------------------------------------

    /** Derived qualified seconds right now (monotonic). No persistence. */
    fun qualifiedSecondsNow(): Int = _state.value.qualifiedSeconds(timeProvider.elapsedRealtimeMillis())

    /** Current monotonic reading (for renderers that derive their own ticks). */
    fun nowElapsedMs(): Long = timeProvider.elapsedRealtimeMillis()

    /**
     * Persists a safe point of qualified time while ACTIVE. Called on the
     * checkpoint cadence and on meaningful lifecycle transitions — never per
     * frame and never per second.
     */
    suspend fun checkpointNow(): Unit = mutex.withLock {
        val session = _state.value.session ?: return
        if (session.status != MeditationSessionStatus.ACTIVE) return
        checkpointRepository.save(currentCheckpoint(session.sessionId))
    }

    /**
     * Completes the session if (and only if) the derived qualified time has
     * reached the requirement. Returns true when completion happened.
     */
    suspend fun evaluateCompletionNow(): Boolean = mutex.withLock {
        val stateValue = _state.value
        val session = stateValue.session ?: return@withLock false
        if (session.status != MeditationSessionStatus.ACTIVE) return@withLock false
        if (!stateValue.isRequirementMet(timeProvider.elapsedRealtimeMillis())) return@withLock false

        val result = sessionService.completeAtQualificationTarget(session.sessionId)
        val completed = result.getOrNull()
        if (completed != null && completed.status == MeditationSessionStatus.COMPLETED) {
            checkpointRepository.delete(session.sessionId)
            adoptSession(session.sessionId)
            emitCompletedOnce(completed)
            true
        } else {
            false
        }
    }

    fun setMode(mode: MeditationMode) {
        _state.value = _state.value.copy(mode = mode)
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun emitCompletedOnce(session: MeditationSession) {
        if (completionEmittedFor == session.sessionId) return
        completionEmittedFor = session.sessionId
        _events.tryEmit(MeditationRuntimeEvent.Completed(session))
    }

    private suspend fun currentSessionId(): String? = _state.value.session?.sessionId

    private suspend fun adoptSession(sessionId: String) {
        val session = sessionRepository.getSession(sessionId)
        val openStart = sessionService.openIntervalStart(sessionId)
        _state.value = _state.value.copy(
            session = session,
            openIntervalStartElapsedMs = openStart
        )
    }

    private fun currentCheckpoint(sessionId: String): SessionTimeCheckpoint =
        SessionTimeCheckpoint(
            sessionId = sessionId,
            qualifiedSecondsSnapshot = qualifiedSecondsNow(),
            atElapsedRealtimeMs = timeProvider.elapsedRealtimeMillis(),
            atWallClockMs = timeProvider.currentTimeMillis(),
            bootCount = bootIdentityReader.bootCount()
        )

    private fun ensureTicker() {
        if (tickerJob?.isActive == true) return
        val ticksPerCheckpoint = (checkpointIntervalMs / TICK_INTERVAL_MS).coerceAtLeast(1)
        tickerJob = scope.launch {
            var ticks = 0L
            while (currentCoroutineContext().isActive) {
                delay(TICK_INTERVAL_MS)
                if (!_state.value.isActive) break
                if (evaluateCompletionNow()) break
                ticks++
                if (ticks % ticksPerCheckpoint == 0L) {
                    checkpointNow()
                }
            }
        }
    }

    /** Local (Pass 2) rhythmic day id. Routine supplies the real one later. */
    fun currentRhythmicDayId(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeProvider.currentTimeMillis()))

    private fun rhythmicDayId(): String = currentRhythmicDayId()

    companion object {
        /** Standard requirement: 30 minutes = 1,800 qualified seconds. */
        const val MORNING_REQUIRED_SECONDS = 1_800

        /** Safe-point cadence while ACTIVE (spec: every 10-15 seconds). */
        const val DEFAULT_CHECKPOINT_INTERVAL_MS = 12_000L

        /** Internal render/evaluation tick. No persistence happens per tick. */
        const val TICK_INTERVAL_MS = 1_000L

        private fun maxOfNullable(a: Long?, b: Long?): Long? = when {
            a == null -> b
            b == null -> a
            else -> maxOf(a, b)
        }
    }
}
