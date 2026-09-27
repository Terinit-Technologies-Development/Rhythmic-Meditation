package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.data.repository.EveningMeditationRepository
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [EveningMeditationRepository] for deterministic unit tests.
 */
class FakeEveningMeditationRepository : EveningMeditationRepository {

    private val records = MutableStateFlow<Map<String, EveningMeditationRecord>>(emptyMap())

    /** Number of writes performed (for "due exactly once" assertions). */
    var upsertCount: Int = 0
        private set

    override fun observeRecord(attentionDayId: String): Flow<EveningMeditationRecord?> =
        records.map { map -> map[attentionDayId] }

    override fun observeRecords(): Flow<List<EveningMeditationRecord>> =
        records.map { map -> map.values.sortedBy { it.updatedAtEpochMs } }

    override suspend fun getRecord(attentionDayId: String): EveningMeditationRecord? =
        records.value[attentionDayId]

    override suspend fun upsertRecord(record: EveningMeditationRecord) {
        upsertCount++
        records.value = records.value + (record.attentionDayId to record)
    }
}
