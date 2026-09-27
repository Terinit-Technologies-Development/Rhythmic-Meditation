package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.MeditationInsightSnapshotDao
import com.terinit.rhythmicmeditation.data.mapper.toDomain
import com.terinit.rhythmicmeditation.data.mapper.toEntity
import com.terinit.rhythmicmeditation.domain.model.MeditationInsightSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [MeditationInsightsRepository]. Snapshot generation is
 * intentionally deferred; this repository only stores and exposes evidence.
 */
class RoomMeditationInsightsRepository(
    private val dao: MeditationInsightSnapshotDao
) : MeditationInsightsRepository {

    override fun observeLatestInsightSnapshots(): Flow<List<MeditationInsightSnapshot>> =
        dao.observeLatest(LATEST_LIMIT).map { list -> list.map { it.toDomain() } }

    override suspend fun upsertSnapshot(snapshot: MeditationInsightSnapshot) {
        dao.upsert(snapshot.toEntity())
    }

    private companion object {
        const val LATEST_LIMIT = 30
    }
}
