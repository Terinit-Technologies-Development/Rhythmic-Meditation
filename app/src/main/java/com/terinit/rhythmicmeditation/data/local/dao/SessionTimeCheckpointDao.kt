package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.SessionTimeCheckpointEntity

@Dao
interface SessionTimeCheckpointDao {

    /** One checkpoint per session; each save replaces the previous one. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(entity: SessionTimeCheckpointEntity)

    @Query("SELECT * FROM session_time_checkpoints WHERE sessionId = :sessionId LIMIT 1")
    suspend fun get(sessionId: String): SessionTimeCheckpointEntity?

    @Query("DELETE FROM session_time_checkpoints WHERE sessionId = :sessionId")
    suspend fun delete(sessionId: String)
}
