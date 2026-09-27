package com.terinit.rhythmicmeditation.runtime

import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus

/**
 * UI-facing projection of the meditation runtime.
 *
 * Qualified time is ALWAYS derived from the monotonic ledger:
 *
 *   qualifiedMs = closedQualifiedIntervals.sumOf { durationMs } +
 *                 currentOpenIntervalDelta
 *
 * Nothing here mutates the ledger; the UI only renders this calculation.
 */
data class MeditationRuntimeState(
    val session: MeditationSession? = null,
    val openIntervalStartElapsedMs: Long? = null,
    val mode: MeditationMode = MeditationMode.STILLNESS,
    val recoveryNotice: RecoveryNotice? = null
) {
    val isActive: Boolean get() = session?.status == MeditationSessionStatus.ACTIVE

    val isPaused: Boolean get() = session?.status == MeditationSessionStatus.PAUSED

    /** Derived qualified seconds at [nowElapsedMs] (monotonic). */
    fun qualifiedSeconds(nowElapsedMs: Long): Int {
        val session = session ?: return 0
        val tailSeconds = openIntervalStartElapsedMs
            ?.let { ((nowElapsedMs - it).coerceAtLeast(0L)) / 1000L }
            ?: 0L
        return (session.completedQualifiedSeconds + tailSeconds)
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()
    }

    /** Seconds still missing before the requirement is met. */
    fun remainingSeconds(nowElapsedMs: Long): Int {
        val required = session?.requiredSeconds ?: return 0
        return (required - qualifiedSeconds(nowElapsedMs)).coerceAtLeast(0)
    }

    fun isRequirementMet(nowElapsedMs: Long): Boolean {
        val session = session ?: return false
        return session.requiredSeconds > 0 &&
            qualifiedSeconds(nowElapsedMs) >= session.requiredSeconds
    }
}

/** One-shot notices the UI may surface after a recovery. */
sealed interface RecoveryNotice {
    /** A previously active session was conservatively restored to PAUSED. */
    data class RestoredToPaused(val rebootDetected: Boolean) : RecoveryNotice
}

/** One-shot runtime events (delivered exactly once per session). */
sealed interface MeditationRuntimeEvent {
    data class Completed(val session: MeditationSession) : MeditationRuntimeEvent
    data class Cancelled(val session: MeditationSession) : MeditationRuntimeEvent
    data class RestoredToPaused(val session: MeditationSession, val rebootDetected: Boolean) :
        MeditationRuntimeEvent
}

/** Outcome of the process-start recovery pass. */
data class RecoveryReport(
    val sessionId: String?,
    val restoredToPaused: Boolean,
    val rebootDetected: Boolean
)
