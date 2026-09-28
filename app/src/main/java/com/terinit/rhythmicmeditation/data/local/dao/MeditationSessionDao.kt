package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationSessionDao {

    @Query("SELECT * FROM meditation_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): MeditationSessionEntity?

    @Query("SELECT * FROM meditation_sessions WHERE sessionId LIKE :prefix || '%' ORDER BY createdAtEpochMs ASC")
    suspend fun getByIdPrefix(prefix: String): List<MeditationSessionEntity>

    @Query("SELECT * FROM meditation_sessions WHERE sessionId = :sessionId LIMIT 1")
    fun observeById(sessionId: String): Flow<MeditationSessionEntity?>

    @Query(
        "SELECT * FROM meditation_sessions " +
            "WHERE status IN ('PENDING', 'ACTIVE', 'PAUSED') " +
            "ORDER BY createdAtEpochMs DESC LIMIT 1"
    )
    fun observeCurrentSession(): Flow<MeditationSessionEntity?>

    @Query("SELECT * FROM meditation_sessions ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<MeditationSessionEntity>>

    /**
     * True update-or-insert semantics (NOT OnConflictStrategy.REPLACE):
     * SQLite REPLACE deletes the old row first, which would fire the
     * ON DELETE CASCADE on meditation_intervals / session_interruption_events
     * and destroy the evidence trail exactly when a session is completed.
     * Found by physical-device validation (Pass 4).
     */
    @Upsert
    suspend fun upsert(entity: MeditationSessionEntity)

    @Query("UPDATE meditation_sessions SET status = :status WHERE sessionId = :sessionId")
    suspend fun updateStatus(sessionId: String, status: String)

    @Query(
        "UPDATE meditation_sessions " +
            "SET completedQualifiedSeconds = completedQualifiedSeconds + :additionalSeconds " +
            "WHERE sessionId = :sessionId"
    )
    suspend fun addQualifiedSeconds(sessionId: String, additionalSeconds: Int)

    @Query(
        "UPDATE meditation_sessions SET interruptionCount = interruptionCount + 1 " +
            "WHERE sessionId = :sessionId"
    )
    suspend fun incrementInterruptionCount(sessionId: String)

    @Query(
        "UPDATE meditation_sessions SET pauseCount = pauseCount + 1 " +
            "WHERE sessionId = :sessionId"
    )
    suspend fun incrementPauseCount(sessionId: String)
}
