package com.terinit.rhythmicmeditation.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.terinit.rhythmicmeditation.data.local.entity.EveningMeditationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EveningMeditationDao {

    @Query("SELECT * FROM evening_meditation WHERE attentionDayId = :attentionDayId LIMIT 1")
    suspend fun getById(attentionDayId: String): EveningMeditationEntity?

    @Query("SELECT * FROM evening_meditation WHERE attentionDayId = :attentionDayId LIMIT 1")
    fun observeById(attentionDayId: String): Flow<EveningMeditationEntity?>

    @Query("SELECT * FROM evening_meditation ORDER BY updatedAtEpochMs DESC")
    fun observeAll(): Flow<List<EveningMeditationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EveningMeditationEntity)
}
