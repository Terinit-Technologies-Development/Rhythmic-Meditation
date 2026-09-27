package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.MeditationIntervalDao
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity
import com.terinit.rhythmicmeditation.data.mapper.toDomain
import com.terinit.rhythmicmeditation.domain.model.MeditationInterval

/**
 * Room-backed [MeditationIntervalRepository].
 */
class RoomMeditationIntervalRepository(
    private val dao: MeditationIntervalDao
) : MeditationIntervalRepository {

    override suspend fun startInterval(
        sessionId: String,
        startedElapsedMs: Long,
        startedWallClockMs: Long
    ): Long = dao.insert(
        MeditationIntervalEntity(
            sessionId = sessionId,
            startedElapsedRealtimeMs = startedElapsedMs,
            endedElapsedRealtimeMs = null,
            startedWallClockMs = startedWallClockMs,
            endedWallClockMs = null
        )
    )

    override suspend fun closeOpenInterval(
        sessionId: String,
        endedElapsedMs: Long,
        endedWallClockMs: Long
    ) {
        dao.closeOpenIntervals(sessionId, endedElapsedMs, endedWallClockMs)
    }

    override suspend fun getIntervalsForSession(sessionId: String): List<MeditationInterval> =
        dao.getForSession(sessionId).map { it.toDomain() }

    override suspend fun getOpenInterval(sessionId: String): MeditationInterval? =
        dao.getOpenInterval(sessionId)?.toDomain()
}
