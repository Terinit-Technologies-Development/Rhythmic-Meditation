package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.domain.timing.TimeProvider

/**
 * Deterministic [TimeProvider] for tests.
 *
 * Simulates both monotonic and wall-clock time; [jumpWallClock] models
 * wall-clock tampering without touching the monotonic clock.
 */
class FakeTimeProvider(
    var wallClockMs: Long = DEFAULT_WALL_CLOCK_MS,
    var elapsedMs: Long = DEFAULT_ELAPSED_MS
) : TimeProvider {

    override fun currentTimeMillis(): Long = wallClockMs

    override fun elapsedRealtimeMillis(): Long = elapsedMs

    /** Advances both clocks (normal time passing). */
    fun advanceMillis(millis: Long) {
        wallClockMs += millis
        elapsedMs += millis
    }

    fun advanceSeconds(seconds: Int) = advanceMillis(seconds * 1000L)

    /** Advances only the wall clock (tampering / timezone drift). */
    fun jumpWallClock(millis: Long) {
        wallClockMs += millis
    }

    /** Rewinds only the wall clock (backward tampering). */
    fun rewindWallClock(millis: Long) {
        wallClockMs -= millis
    }

    companion object {
        const val DEFAULT_WALL_CLOCK_MS = 1_700_000_000_000L
        const val DEFAULT_ELAPSED_MS = 100_000L
    }
}
