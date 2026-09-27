package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.domain.model.MeditationInsightSnapshot
import kotlinx.coroutines.flow.Flow

/**
 * Read surface for local insight snapshots. Full insight generation lands in
 * a later pass; Pass 1 stores and exposes the raw snapshots only.
 */
interface MeditationInsightsRepository {

    fun observeLatestInsightSnapshots(): Flow<List<MeditationInsightSnapshot>>

    suspend fun upsertSnapshot(snapshot: MeditationInsightSnapshot)
}
