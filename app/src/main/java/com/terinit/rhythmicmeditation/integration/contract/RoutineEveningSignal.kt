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
 * Narrow, signature-protected surface placeholder for Routine's evening
 * schedule. The trigger for the Evening Wind-Down is Routine-owned — this app
 * never builds a schedule engine and never infers "evening" from the clock.
 *
 * Routine integration completes later; the evening state model works
 * standalone against [UnavailableRoutineEveningSignal].
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
