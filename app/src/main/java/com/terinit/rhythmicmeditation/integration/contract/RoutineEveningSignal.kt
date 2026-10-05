package com.terinit.rhythmicmeditation.integration.contract

/**
 * Read-only projection of Rhythmic Routine's current Evening Wind-Down window.
 *
 * All fields are Routine-owned: the attention day id and the moment the
 * evening wind-down became due are decided by Routine's schedule, never by
 * this app.
 */
data class EveningSignal(
    val attentionDayId: String,
    val dueAtEpochMs: Long,
    val transitionAtEpochMs: Long
)

/**
 * Narrow, signature-protected surface for Routine's evening schedule. The
 * ContentResolver implementation reads Routine's live projection; the trigger
 * remains Routine-owned, and this app never infers "evening" from the clock.
 *
 * The evening state model also works standalone against
 * [UnavailableRoutineEveningSignal].
 */
interface RoutineEveningSignal {

    /** The current evening wind-down signal, or null when none / unavailable. */
    fun currentEveningSignal(): EveningSignal?
}

/**
 * Standalone default: no Routine connection, no signal. The evening record is
 * only ever marked DUE when a signal (or [EveningSignal] handed over directly)
 * says so — an absent Routine never fabricates an obligation.
 */
object UnavailableRoutineEveningSignal : RoutineEveningSignal {
    override fun currentEveningSignal(): EveningSignal? = null
}
