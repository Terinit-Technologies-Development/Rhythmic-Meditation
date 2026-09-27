package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.data.repository.SessionCheckpointRepository
import com.terinit.rhythmicmeditation.data.repository.SessionTimeCheckpoint

/**
 * In-memory [SessionCheckpointRepository] for deterministic unit tests.
 */
class FakeSessionCheckpointRepository : SessionCheckpointRepository {

    private val checkpoints = mutableMapOf<String, SessionTimeCheckpoint>()

    /** Number of saves performed (for cadence assertions). */
    var saveCount: Int = 0
        private set

    val all: Map<String, SessionTimeCheckpoint> get() = checkpoints.toMap()

    override suspend fun save(checkpoint: SessionTimeCheckpoint) {
        saveCount++
        checkpoints[checkpoint.sessionId] = checkpoint
    }

    override suspend fun get(sessionId: String): SessionTimeCheckpoint? =
        checkpoints[sessionId]

    override suspend fun delete(sessionId: String) {
        checkpoints.remove(sessionId)
    }
}
