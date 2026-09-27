package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationSessionDao {

    @Query("SELECT * FROM meditation_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getById(sessionId: String): MeditationSessionEntity?

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
