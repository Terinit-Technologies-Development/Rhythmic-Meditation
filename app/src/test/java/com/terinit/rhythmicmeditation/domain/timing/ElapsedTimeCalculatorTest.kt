package com.terinit.rhythmicmeditation.domain.timing

import com.terinit.rhythmicmeditation.domain.model.MeditationInterval
import org.junit.Assert.assertEquals
import org.junit.Test

class ElapsedTimeCalculatorTest {

    private fun interval(
        id: Long,
        start: Long,
        end: Long?
    ) = MeditationInterval(
        id = id,
        sessionId = "session-1",
        startedElapsedRealtimeMs = start,
        endedElapsedRealtimeMs = end,
        startedWallClockMs = start,
        endedWallClockMs = end
    )

    @Test
    fun `closed interval duration is end minus start`() {
        val duration = ElapsedTimeCalculator.intervalDurationMs(
            interval(1, start = 1_000, end = 61_000),
            nowElapsedMs = 999_999
        )
        assertEquals(60_000L, duration)
    }

    @Test
    fun `open interval is counted up to now`() {
        val duration = ElapsedTimeCalculator.intervalDurationMs(
            interval(1, start = 10_000, end = null),
            nowElapsedMs = 40_000
        )
        assertEquals(30_000L, duration)
    }

    @Test
    fun `interval duration is never negative`() {
        val duration = ElapsedTimeCalculator.intervalDurationMs(
            interval(1, start = 50_000, end = 40_000),
            nowElapsedMs = 60_000
        )
        assertEquals(0L, duration)
    }

    @Test
    fun `qualified time sums all intervals`() {
        val intervals = listOf(
            interval(1, start = 0, end = 30_000),
            interval(2, start = 40_000, end = 70_000),
            interval(3, start = 100_000, end = null)
        )
        // 30s + 30s + open 25s (now = 125_000)
        assertEquals(
            85_000L,
            ElapsedTimeCalculator.qualifiedMillis(intervals, nowElapsedMs = 125_000)
        )
        assertEquals(
            85,
            ElapsedTimeCalculator.qualifiedSeconds(intervals, nowElapsedMs = 125_000)
        )
    }

    @Test
    fun `qualified seconds floors partial seconds`() {
        val intervals = listOf(interval(1, start = 0, end = 1_999))
        assertEquals(1, ElapsedTimeCalculator.qualifiedSeconds(intervals, nowElapsedMs = 10_000))
    }

    @Test
    fun `closed interval seconds clamps to zero`() {
        assertEquals(
            0,
            ElapsedTimeCalculator.closedIntervalSeconds(
                startedElapsedMs = 20_000,
                endedElapsedMs = 10_000
            )
        )
        assertEquals(
            12,
            ElapsedTimeCalculator.closedIntervalSeconds(
                startedElapsedMs = 0,
                endedElapsedMs = 12_999
            )
        )
    }

    @Test
    fun `remaining seconds reaches zero once requirement is met`() {
        val intervals = listOf(interval(1, start = 0, end = 45_000))
        assertEquals(
            15,
            ElapsedTimeCalculator.remainingSeconds(intervals, nowElapsedMs = 60_000, requiredSeconds = 60)
        )
        assertEquals(
            0,
            ElapsedTimeCalculator.remainingSeconds(intervals, nowElapsedMs = 60_000, requiredSeconds = 30)
        )
    }

    @Test
    fun `wall clock values are never used for duration`() {
        // Wall clock differs wildly from monotonic clock; duration must follow
        // the monotonic columns only.
        val suspicious = MeditationInterval(
            id = 1,
            sessionId = "session-1",
            startedElapsedRealtimeMs = 1_000,
            endedElapsedRealtimeMs = 11_000,
            startedWallClockMs = 0,
            endedWallClockMs = 999_999_999
        )
        assertEquals(
            10_000L,
            ElapsedTimeCalculator.intervalDurationMs(suspicious, nowElapsedMs = 20_000)
        )
    }
}
