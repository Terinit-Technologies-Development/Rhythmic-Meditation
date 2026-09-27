package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.SessionTimeCheckpointDao
import com.terinit.rhythmicmeditation.data.local.entity.SessionTimeCheckpointEntity

/**
 * Room-backed [SessionCheckpointRepository].
 */
class RoomSessionCheckpointRepository(
    private val dao: SessionTimeCheckpointDao
) : SessionCheckpointRepository {

    override suspend fun save(checkpoint: SessionTimeCheckpoint) {
        dao.save(
            SessionTimeCheckpointEntity(
                sessionId = checkpoint.sessionId,
                qualifiedSecondsSnapshot = checkpoint.qualifiedSecondsSnapshot,
                atElapsedRealtimeMs = checkpoint.atElapsedRealtimeMs,
                atWallClockMs = checkpoint.atWallClockMs,
                bootCount = checkpoint.bootCount
            )
        )
    }

    override suspend fun get(sessionId: String): SessionTimeCheckpoint? =
        dao.get(sessionId)?.let {
            SessionTimeCheckpoint(
                sessionId = it.sessionId,
                qualifiedSecondsSnapshot = it.qualifiedSecondsSnapshot,
                atElapsedRealtimeMs = it.atElapsedRealtimeMs,
                atWallClockMs = it.atWallClockMs,
                bootCount = it.bootCount
            )
        }

    override suspend fun delete(sessionId: String) {
        dao.delete(sessionId)
    }
}
