package com.terinit.rhythmicmeditation.domain.evening

/**
 * The Evening Wind-Down: an optional, non-compulsory, deferable, non-punitive
 * evening meditation. Exactly ONE obligation may exist per Attention Day, and
 * nothing here ever touches policy.
 *
 * Hard boundaries (enforced by construction, covered by tests):
 * - Evening completion NEVER consumes a Meditation substitution, NEVER
 *   satisfies Morning Meditation, and NEVER satisfies a cooldown Restorative
 *   Gate. This module writes only the evening record and its own sessions; it
 *   is kept entirely out of [com.terinit.rhythmicmeditation.domain.session.
 *   MorningSessionPolicy] and restorative-gate logic.
 * - Deferring or snoozing never changes tomorrow's Morning requirement or any
 *   cooldown — those belong to Rhythmic Routine.
 * - A record is keyed by Attention Day (`attentionDayId`), so a deferral
 *   persists for this Attention Day only; the next day gets a fresh record.
 *
 * This model is pure Kotlin: no Android, no clock, no persistence.
 */
enum class EveningMeditationState {
    /** The evening wind-down has not begun yet. */
    NOT_DUE,
    /** The evening wind-down began and the practice is offered. */
    DUE,
    /** The user snoozed the offer; it becomes DUE again at [EveningMeditationRecord.snoozedUntilEpochMs]. */
    SNOOZED,
    /** The evening session is running (or awaiting Resume). */
    IN_PROGRESS,
    /** The evening meditation completed. */
    COMPLETED,
    /** The user deferred tonight. Non-punitive and terminal for this Attention Day. */
    DEFERRED;

    companion object {
        /** Parses a wire value; returns null for unknown states. */
        fun fromWire(value: String?): EveningMeditationState? =
            entries.firstOrNull { it.name == value }
    }
}

/**
 * One record per Attention Day (id = `attentionDayId`). Wall-clock fields are
 * diagnostics/display only — the practice itself is qualified by the monotonic
 * meditation engine, never by this record.
 */
data class EveningMeditationRecord(
    val attentionDayId: String,
    val state: EveningMeditationState,
    val dueAtEpochMs: Long?,
    val snoozedUntilEpochMs: Long?,
    val sessionId: String?,
    val updatedAtEpochMs: Long
)

/**
 * Pure state machine for the evening record.
 *
 * Transition table (anything not listed is a no-op returning the record
 * unchanged — the terminal states COMPLETED and DEFERRED can never be revived
 * or re-earned):
 *
 * | from        | markDue | snooze   | resumeFromSnooze | defer    | markInProgress | markCompleted |
 * | ----------- | ------- | -------- | ---------------- | -------- | -------------- | ------------- |
 * | (no record) | DUE     | —        | —                | —        | —              | —             |
 * | NOT_DUE     | DUE     | —        | —                | —        | IN_PROGRESS    | —             |
 * | DUE         | —       | SNOOZED  | —                | DEFERRED | IN_PROGRESS    | —             |
 * | SNOOZED     | —       | SNOOZED  | DUE              | DEFERRED | IN_PROGRESS    | —             |
 * | IN_PROGRESS | —       | —        | —                | —        | IN_PROGRESS    | COMPLETED     |
 * | COMPLETED   | —       | —        | —                | —        | —              | —             |
 * | DEFERRED    | —       | —        | —                | —        | —              | —             |
 *
 * [markDue] is the only creator and transitions to DUE exactly once per
 * Attention Day: the first `dueAtEpochMs` wins and later signals never
 * generate a second obligation or reset the day's decision.
 */
object EveningMeditation {

    const val SESSION_ID_PREFIX = "evening-"

    /** Standard requirement: 30 minutes = 1,800 qualified seconds. */
    const val REQUIRED_SECONDS = 1_800

    /** Snooze length: 15 minutes. */
    const val SNOOZE_DURATION_MS = 15 * 60_000L

    /** Deterministic session id for one Attention Day. */
    fun eveningMeditationSessionId(attentionDayId: String): String =
        "$SESSION_ID_PREFIX$attentionDayId"

    /**
     * Id for a new attempt given the day's existing attempts. A cancelled or
     * expired attempt yields a fresh id (`-1`, `-2`, … — the same convention
     * as MorningSessionPolicy) so no progress is inherited and the cancelled
     * ledger row is never destroyed. The first attempt always uses the
     * deterministic [eveningMeditationSessionId].
     */
    fun nextSessionId(existingAttemptIds: List<String>, attentionDayId: String): String {
        val base = eveningMeditationSessionId(attentionDayId)
        return if (existingAttemptIds.isEmpty()) base else "$base-${existingAttemptIds.size}"
    }

    /**
     * Marks the record DUE — creating it when needed — exactly once per
     * Attention Day. [now] is the moment the evening wind-down began and is
     * recorded as `dueAtEpochMs`. Later calls for the same day are no-ops.
     */
    fun markDue(
        record: EveningMeditationRecord?,
        attentionDayId: String,
        now: Long
    ): EveningMeditationRecord = when {
        record == null -> EveningMeditationRecord(
            attentionDayId = attentionDayId,
            state = EveningMeditationState.DUE,
            dueAtEpochMs = now,
            snoozedUntilEpochMs = null,
            sessionId = null,
            updatedAtEpochMs = now
        )
        record.state == EveningMeditationState.NOT_DUE -> record.copy(
            state = EveningMeditationState.DUE,
            dueAtEpochMs = record.dueAtEpochMs ?: now,
            updatedAtEpochMs = now
        )
        // Already marked due this Attention Day (first due wins).
        else -> record
    }

    /**
     * Snoozes the offer for [durationMs] (default 15 minutes). The snooze is
     * persisted; once `snoozedUntilEpochMs` passes the effective state is DUE
     * again (see [effectiveState]). No-op outside DUE/SNOOZED.
     */
    fun snooze(
        record: EveningMeditationRecord,
        now: Long,
        durationMs: Long = SNOOZE_DURATION_MS
    ): EveningMeditationRecord = when (record.state) {
        EveningMeditationState.DUE, EveningMeditationState.SNOOZED -> record.copy(
            state = EveningMeditationState.SNOOZED,
            snoozedUntilEpochMs = now + durationMs,
            updatedAtEpochMs = now
        )
        else -> record
    }

    /**
     * The user picked the practice back up before the snooze expired: the
     * offer returns to DUE immediately. No-op outside SNOOZED.
     */
    fun resumeFromSnooze(
        record: EveningMeditationRecord,
        now: Long
    ): EveningMeditationRecord = when (record.state) {
        EveningMeditationState.SNOOZED -> record.copy(
            state = EveningMeditationState.DUE,
            snoozedUntilEpochMs = null,
            updatedAtEpochMs = now
        )
        else -> record
    }

    /**
     * Defers tonight's practice. Non-punitive and terminal for this Attention
     * Day only: the next day gets a fresh record, and nothing about tomorrow's
     * Morning requirement or any cooldown changes. No-op outside DUE/SNOOZED.
     */
    fun defer(
        record: EveningMeditationRecord,
        now: Long = record.updatedAtEpochMs
    ): EveningMeditationRecord = when (record.state) {
        EveningMeditationState.DUE, EveningMeditationState.SNOOZED -> record.copy(
            state = EveningMeditationState.DEFERRED,
            snoozedUntilEpochMs = null,
            updatedAtEpochMs = now
        )
        else -> record
    }

    /**
     * Binds the day's evening session ([sessionId] =
     * `evening-<attentionDayId>`, or a later attempt id). Rebinding is allowed
     * from IN_PROGRESS so a fresh attempt after a cancelled one is tracked;
     * the terminal states are never revived.
     */
    fun markInProgress(
        record: EveningMeditationRecord,
        sessionId: String,
        now: Long = record.updatedAtEpochMs
    ): EveningMeditationRecord = when (record.state) {
        EveningMeditationState.NOT_DUE,
        EveningMeditationState.DUE,
        EveningMeditationState.SNOOZED,
        EveningMeditationState.IN_PROGRESS -> record.copy(
            state = EveningMeditationState.IN_PROGRESS,
            sessionId = sessionId,
            snoozedUntilEpochMs = null,
            updatedAtEpochMs = now
        )
        else -> record
    }

    /**
     * Marks the practice complete. Called only when the bound session's
     * status becomes COMPLETED — evening completion is evidence about the
     * evening practice alone and never touches substitutions, gates, or the
     * Morning requirement. Idempotent; no-op outside IN_PROGRESS.
     */
    fun markCompleted(
        record: EveningMeditationRecord,
        now: Long = record.updatedAtEpochMs
    ): EveningMeditationRecord = when (record.state) {
        EveningMeditationState.IN_PROGRESS -> record.copy(
            state = EveningMeditationState.COMPLETED,
            snoozedUntilEpochMs = null,
            updatedAtEpochMs = now
        )
        else -> record
    }

    /**
     * The state the UI should render at [now]: a persisted SNOOZED record
     * whose `snoozedUntilEpochMs` has passed is effectively DUE again. A
     * missing record is NOT_DUE — the app never fabricates an obligation.
     */
    fun effectiveState(record: EveningMeditationRecord?, now: Long): EveningMeditationState =
        when {
            record == null -> EveningMeditationState.NOT_DUE
            record.state == EveningMeditationState.SNOOZED &&
                (record.snoozedUntilEpochMs == null || record.snoozedUntilEpochMs <= now) ->
                EveningMeditationState.DUE
            else -> record.state
        }
}
