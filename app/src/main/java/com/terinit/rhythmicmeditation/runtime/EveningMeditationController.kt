package com.terinit.rhythmicmeditation.runtime

import com.terinit.rhythmicmeditation.data.repository.EveningMeditationRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditation
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.integration.contract.RoutineEveningSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Runtime for the optional Evening Wind-Down practice.
 *
 * The trigger is Routine-owned: [onEveningWindDownDue] / [onEveningWindDownEnded]
 * are the narrow signal surface (also reachable through [RoutineEveningSignal]
 * via [syncFromSignal]) — this class never builds a schedule engine and never
 * infers "evening" from the clock. Everything else is local-first and works
 * standalone against [com.terinit.rhythmicmeditation.integration.contract.
 * UnavailableRoutineEveningSignal].
 *
 * Hard boundaries (tested): evening completion never consumes a Meditation
 * substitution, never satisfies Morning Meditation, and never satisfies a
 * cooldown Restorative Gate — this controller writes only the evening record
 * and its own EVENING_WIND_DOWN sessions, and is kept entirely out of
 * MorningSessionPolicy and restorative-gate logic. Snoozing or deferring never
 * changes tomorrow's Morning requirement or any cooldown.
 */
class EveningMeditationController(
    private val eveningRepository: EveningMeditationRepository,
    private val sessionRepository: MeditationSessionRepository,
    private val runtimeController: MeditationRuntimeController,
    private val routineEveningSignal: RoutineEveningSignal,
    private val timeProvider: TimeProvider,
    private val scope: CoroutineScope
) {

    init {
        // Completion follows the ledger: when the bound session's status
        // becomes COMPLETED, the record is marked COMPLETED. Read-only observe,
        // exactly one record write per evening completion.
        scope.launch {
            combine(
                sessionRepository.observeSessions(),
                eveningRepository.observeRecords()
            ) { sessions, records -> sessions to records }
                .collect { (sessions, records) -> markCompletedWhereDue(sessions, records) }
        }
    }

    /** The evening record for [attentionDayId] (defaults to the current day). */
    fun observeRecord(attentionDayId: String = currentAttentionDayId()): Flow<EveningMeditationRecord?> =
        eveningRepository.observeRecord(attentionDayId)

    /**
     * The Attention Day the evening record is keyed by: Routine's day id when
     * a signal is available, otherwise the local date key (the same local-day
     * convention as MorningSessionPolicy until pairing supplies real day ids).
     */
    fun currentAttentionDayId(): String =
        routineEveningSignal.currentEveningSignal()?.attentionDayId
            ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timeProvider.currentTimeMillis()))

    // ------------------------------------------------------------------
    // Routine signal surface (narrow)
    // ------------------------------------------------------------------

    /**
     * The Evening Wind-Down began (Routine-owned). Creates the day's record
     * and transitions it to DUE — exactly once per Attention Day.
     */
    fun onEveningWindDownDue(attentionDayId: String, dueAtEpochMs: Long) {
        scope.launch { applyDue(attentionDayId, dueAtEpochMs) }
    }

    /**
     * The Evening Wind-Down window ended (Routine-owned). Deliberately does
     * NOT alter the record: a deferral or snooze is a decision, not a failure,
     * and the record stays truthful for the whole Attention Day.
     */
    fun onEveningWindDownEnded() {
        // Intentionally empty — see kdoc. The record is authoritative.
    }

    /** Consults Routine's narrow signal. No signal -> no obligation is created. */
    fun syncFromSignal() {
        scope.launch {
            val signal = routineEveningSignal.currentEveningSignal() ?: return@launch
            applyDue(signal.attentionDayId, signal.dueAtEpochMs)
        }
    }

    private suspend fun applyDue(attentionDayId: String, dueAtEpochMs: Long) {
        val record = eveningRepository.getRecord(attentionDayId)
        val marked = EveningMeditation.markDue(record, attentionDayId, dueAtEpochMs)
        // markDue returns the record unchanged once the day is marked —
        // one write per Attention Day, exactly once.
        if (marked !== record) {
            eveningRepository.upsertRecord(marked)
        }
    }

    // ------------------------------------------------------------------
    // User intent
    // ------------------------------------------------------------------

    /**
     * Starts (or re-opens) tonight's evening meditation. Idempotent: the
     * per-day session id `evening-<attentionDayId>` is reused while a live
     * attempt exists, so starting twice can never create two evening sessions,
     * and a cancelled attempt never transfers progress (a fresh attempt id is
     * used instead). Requires 1,800 qualified seconds like every standard
     * practice.
     */
    suspend fun startEveningSession(): Result<MeditationSession> {
        val dayId = currentAttentionDayId()
        val baseId = EveningMeditation.eveningMeditationSessionId(dayId)
        val existing = sessionRepository.getSessionsWithIdPrefix(baseId)
        val live = existing
            .filter { it.isActiveLifecycle }
            .maxByOrNull { it.createdAtEpochMs }
        val completed = existing.firstOrNull { it.status == MeditationSessionStatus.COMPLETED }
        val targetId = live?.sessionId
            ?: completed?.sessionId
            ?: EveningMeditation.nextSessionId(existing.map { it.sessionId }, dayId)

        val result = runtimeController.startSession(
            kind = MeditationSessionKind.EVENING_WIND_DOWN,
            requiredSeconds = EveningMeditation.REQUIRED_SECONDS,
            rhythmicDayId = dayId,
            sessionId = targetId
        )
        val session = result.getOrNull()
        if (session != null) {
            val record = eveningRepository.getRecord(dayId) ?: EveningMeditationRecord(
                attentionDayId = dayId,
                state = EveningMeditationState.NOT_DUE,
                dueAtEpochMs = null,
                snoozedUntilEpochMs = null,
                sessionId = null,
                updatedAtEpochMs = timeProvider.currentTimeMillis()
            )
            eveningRepository.upsertRecord(
                EveningMeditation.markInProgress(
                    record = record,
                    sessionId = session.sessionId,
                    now = timeProvider.currentTimeMillis()
                )
            )
        }
        return result
    }

    /** Snoozes tonight's offer for 15 minutes. Persisted across restarts. */
    suspend fun snooze15() {
        val record = currentRecord() ?: return
        eveningRepository.upsertRecord(
            EveningMeditation.snooze(record, timeProvider.currentTimeMillis())
        )
    }

    /** Picks a snoozed offer back up early: the practice is DUE again. */
    suspend fun resumeFromSnooze() {
        val record = currentRecord() ?: return
        eveningRepository.upsertRecord(
            EveningMeditation.resumeFromSnooze(record, timeProvider.currentTimeMillis())
        )
    }

    /** Defers tonight's practice. Non-punitive; tomorrow is unaffected. */
    suspend fun deferTonight() {
        val record = currentRecord() ?: return
        eveningRepository.upsertRecord(
            EveningMeditation.defer(record, timeProvider.currentTimeMillis())
        )
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private suspend fun currentRecord(): EveningMeditationRecord? =
        eveningRepository.getRecord(currentAttentionDayId())

    private suspend fun markCompletedWhereDue(
        sessions: List<MeditationSession>,
        records: List<EveningMeditationRecord>
    ) {
        val sessionsById = sessions.associateBy { it.sessionId }
        records.forEach { record ->
            if (record.state != EveningMeditationState.IN_PROGRESS) return@forEach
            val sessionId = record.sessionId ?: return@forEach
            val session = sessionsById[sessionId] ?: return@forEach
            if (session.status == MeditationSessionStatus.COMPLETED) {
                eveningRepository.upsertRecord(
                    EveningMeditation.markCompleted(record, timeProvider.currentTimeMillis())
                )
            }
        }
    }
}
