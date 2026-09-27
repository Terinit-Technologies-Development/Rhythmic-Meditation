package com.terinit.rhythmicmeditation.domain.model

/**
 * A lightweight daily insight summary.
 *
 * Pass 1 stores the shape only; insight generation and aggregation land in a
 * later pass. Values are local evidence — no cloud sync.
 */
data class MeditationInsightSnapshot(
    val id: Long = 0,
    val rhythmicDayId: String,
    val meditationMinutes: Int,
    val completedMorningSession: Boolean,
    val completedEveningSession: Boolean,
    val restorativeMeditationCount: Int,
    val createdAtEpochMs: Long
)
