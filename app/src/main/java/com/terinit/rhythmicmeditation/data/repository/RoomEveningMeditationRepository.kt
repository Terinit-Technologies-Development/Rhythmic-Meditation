package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.EveningMeditationDao
import com.terinit.rhythmicmeditation.data.mapper.toDomain
import com.terinit.rhythmicmeditation.data.mapper.toEntity
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [EveningMeditationRepository].
 */
class RoomEveningMeditationRepository(
    private val dao: EveningMeditationDao
) : EveningMeditationRepository {

    override fun observeRecord(attentionDayId: String): Flow<EveningMeditationRecord?> =
        dao.observeById(attentionDayId).map { it?.toDomain() }

    override fun observeRecords(): Flow<List<EveningMeditationRecord>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getRecord(attentionDayId: String): EveningMeditationRecord? =
        dao.getById(attentionDayId)?.toDomain()

    override suspend fun upsertRecord(record: EveningMeditationRecord) {
        dao.upsert(record.toEntity())
    }
}
