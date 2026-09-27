package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.SessionInterruptionEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionInterruptionEventDao {

    @Insert
    suspend fun insert(entity: SessionInterruptionEventEntity)

    @Query(
        "SELECT * FROM session_interruption_events WHERE sessionId = :sessionId " +
            "ORDER BY occurredAtEpochMs ASC"
    )
    suspend fun getForSession(sessionId: String): List<SessionInterruptionEventEntity>

    @Query(
        "SELECT * FROM session_interruption_events WHERE sessionId = :sessionId " +
            "ORDER BY occurredAtEpochMs ASC"
    )
    fun observeForSession(sessionId: String): Flow<List<SessionInterruptionEventEntity>>

    @Query("SELECT COUNT(*) FROM session_interruption_events WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: String): Int
}
