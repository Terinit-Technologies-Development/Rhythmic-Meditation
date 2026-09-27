package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.data.repository.MeditationIntervalRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationInterval

/**
 * In-memory [MeditationIntervalRepository] for deterministic unit tests.
 */
class FakeMeditationIntervalRepository : MeditationIntervalRepository {

    private val intervals = mutableListOf<MeditationInterval>()
    private var nextId = 1L

    val allIntervals: List<MeditationInterval> get() = intervals.toList()

    override suspend fun startInterval(
        sessionId: String,
        startedElapsedMs: Long,
        startedWallClockMs: Long
    ): Long {
        val id = nextId++
        intervals += MeditationInterval(
            id = id,
            sessionId = sessionId,
            startedElapsedRealtimeMs = startedElapsedMs,
            endedElapsedRealtimeMs = null,
            startedWallClockMs = startedWallClockMs,
            endedWallClockMs = null
        )
        return id
    }

    override suspend fun closeOpenInterval(
        sessionId: String,
        endedElapsedMs: Long,
        endedWallClockMs: Long
    ) {
        for (index in intervals.indices) {
            val interval = intervals[index]
            if (interval.sessionId == sessionId && interval.isOpen) {
                intervals[index] = interval.copy(
                    endedElapsedRealtimeMs = endedElapsedMs,
                    endedWallClockMs = endedWallClockMs
                )
            }
        }
    }

    override suspend fun getIntervalsForSession(sessionId: String): List<MeditationInterval> =
        intervals.filter { it.sessionId == sessionId }

    override suspend fun getOpenInterval(sessionId: String): MeditationInterval? =
        intervals.firstOrNull { it.sessionId == sessionId && it.isOpen }
}
