package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity

@Dao
interface MeditationIntervalDao {

    @Insert
    suspend fun insert(entity: MeditationIntervalEntity): Long

    @Query(
        "UPDATE meditation_intervals " +
            "SET endedElapsedRealtimeMs = :endedElapsedMs, endedWallClockMs = :endedWallClockMs " +
            "WHERE sessionId = :sessionId AND endedElapsedRealtimeMs IS NULL"
    )
    suspend fun closeOpenIntervals(sessionId: String, endedElapsedMs: Long, endedWallClockMs: Long)

    @Query(
        "SELECT * FROM meditation_intervals WHERE sessionId = :sessionId " +
            "ORDER BY startedElapsedRealtimeMs ASC"
    )
    suspend fun getForSession(sessionId: String): List<MeditationIntervalEntity>

    @Query(
        "SELECT * FROM meditation_intervals " +
            "WHERE sessionId = :sessionId AND endedElapsedRealtimeMs IS NULL LIMIT 1"
    )
    suspend fun getOpenInterval(sessionId: String): MeditationIntervalEntity?

    @Query("DELETE FROM meditation_intervals WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
