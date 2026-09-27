package com.terinit.rhythmicmeditation.domain.model

/**
 * A qualified session interval: a stretch of uninterrupted meditation time.
 *
 * Intervals are the durable evidence used to reconstruct qualified time after
 * process death or interruption.
 *
 * Why two clocks:
 * - [startedElapsedRealtimeMs] / [endedElapsedRealtimeMs] use elapsedRealtime
 *   (monotonic) and are authoritative for qualification and tamper resistance.
 * - [startedWallClockMs] / [endedWallClockMs] are wall-clock values kept for
 *   diagnostics and human-readable debugging only. They must never be used to
 *   compute qualified time.
 */
data class MeditationInterval(
    val id: Long = 0,
    val sessionId: String,
    val startedElapsedRealtimeMs: Long,
    val endedElapsedRealtimeMs: Long?,
    val startedWallClockMs: Long,
    val endedWallClockMs: Long?
) {
    /** An interval that has not been closed yet (e.g. app was killed mid-session). */
    val isOpen: Boolean
        get() = endedElapsedRealtimeMs == null

    /**
     * Duration on the monotonic clock, clamped at [nowElapsedRealtimeMs] for
     * open intervals and never negative.
     */
    fun durationMs(nowElapsedRealtimeMs: Long): Long {
        val end = endedElapsedRealtimeMs ?: nowElapsedRealtimeMs
        return (end - startedElapsedRealtimeMs).coerceAtLeast(0L)
    }
}
