package com.terinit.rhythmicmeditation.data.repository

/**
 * Persisted safe-point of qualified time for conservative recovery.
 *
 * The checkpoint is the only thing that survives process death or reboot;
 * everything after it is treated as unsafe and discarded.
 */
data class SessionTimeCheckpoint(
    val sessionId: String,
    val qualifiedSecondsSnapshot: Int,
    val atElapsedRealtimeMs: Long,
    val atWallClockMs: Long,
    val bootCount: Int?
)

interface SessionCheckpointRepository {
    suspend fun save(checkpoint: SessionTimeCheckpoint)
    suspend fun get(sessionId: String): SessionTimeCheckpoint?
    suspend fun delete(sessionId: String)
}
