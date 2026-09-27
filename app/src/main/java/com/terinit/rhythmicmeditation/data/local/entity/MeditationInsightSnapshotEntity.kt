package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room row for [com.terinit.rhythmicmeditation.domain.model.MeditationInsightSnapshot]. */
@Entity(
    tableName = "meditation_insight_snapshots",
    indices = [Index(value = ["rhythmicDayId"])]
)
data class MeditationInsightSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rhythmicDayId: String,
    val meditationMinutes: Int,
    val completedMorningSession: Boolean,
    val completedEveningSession: Boolean,
    val restorativeMeditationCount: Int,
    val createdAtEpochMs: Long
)
