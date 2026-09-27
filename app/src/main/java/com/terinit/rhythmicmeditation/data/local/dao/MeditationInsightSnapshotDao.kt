package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.MeditationInsightSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeditationInsightSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeditationInsightSnapshotEntity)

    @Query(
        "SELECT * FROM meditation_insight_snapshots " +
            "ORDER BY createdAtEpochMs DESC LIMIT :limit"
    )
    fun observeLatest(limit: Int): Flow<List<MeditationInsightSnapshotEntity>>

    @Query(
        "SELECT * FROM meditation_insight_snapshots WHERE rhythmicDayId = :rhythmicDayId " +
            "ORDER BY createdAtEpochMs DESC LIMIT 1"
    )
    suspend fun latestForDay(rhythmicDayId: String): MeditationInsightSnapshotEntity?
}
