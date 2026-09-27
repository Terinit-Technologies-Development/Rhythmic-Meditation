package com.terinit.rhythmicmeditation.domain.timing

import com.terinit.rhythmicmeditation.domain.model.MeditationInterval

/**
 * Pure helpers for computing qualified meditation time from intervals.
 *
 * All math is done on the monotonic clock ([MeditationInterval.startedElapsedRealtimeMs]).
 * Open intervals are counted up to "now" and every result is clamped so
 * corrupted or reordered data can never produce negative time.
 */
object ElapsedTimeCalculator {

    /** Duration of a single interval on the monotonic clock, clamped at [nowElapsedMs]. */
    fun intervalDurationMs(interval: MeditationInterval, nowElapsedMs: Long): Long =
        interval.durationMs(nowElapsedRealtimeMs = nowElapsedMs)

    /** Total qualified milliseconds across [intervals]. */
    fun qualifiedMillis(intervals: List<MeditationInterval>, nowElapsedMs: Long): Long =
        intervals.sumOf { intervalDurationMs(it, nowElapsedMs) }

    /** Total qualified seconds across [intervals] (whole seconds only, floored). */
    fun qualifiedSeconds(intervals: List<MeditationInterval>, nowElapsedMs: Long): Int =
        (qualifiedMillis(intervals, nowElapsedMs) / 1000L)
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()

    /**
     * Additional qualified seconds contributed by an interval that is being
     * closed at [endedElapsedMs]. Never negative.
     */
    fun closedIntervalSeconds(startedElapsedMs: Long, endedElapsedMs: Long): Int =
        ((endedElapsedMs - startedElapsedMs).coerceAtLeast(0L) / 1000L)
            .coerceIn(0L, Int.MAX_VALUE.toLong())
            .toInt()

    /**
     * Remaining seconds until [requiredSeconds] of qualified time is reached.
     * Zero once the requirement is met.
     */
    fun remainingSeconds(
        intervals: List<MeditationInterval>,
        nowElapsedMs: Long,
        requiredSeconds: Int
    ): Int = (requiredSeconds - qualifiedSeconds(intervals, nowElapsedMs)).coerceAtLeast(0)
}
