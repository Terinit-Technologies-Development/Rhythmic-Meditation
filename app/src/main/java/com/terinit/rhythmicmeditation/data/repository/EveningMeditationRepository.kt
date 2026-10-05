package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import kotlinx.coroutines.flow.Flow

/**
 * Local store of evening wind-down records — one record per Attention Day.
 * Local-first evidence only; never policy.
 */
interface EveningMeditationRepository {

    fun observeRecord(attentionDayId: String): Flow<EveningMeditationRecord?>

    fun observeRecords(): Flow<List<EveningMeditationRecord>>

    suspend fun getRecord(attentionDayId: String): EveningMeditationRecord?

    suspend fun upsertRecord(record: EveningMeditationRecord)
}
