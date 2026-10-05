package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for [com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord].
 * One row per Attention Day; enums are stored by name (see data/mapper).
 */
@Entity(tableName = "evening_meditation")
data class EveningMeditationEntity(
    @PrimaryKey val attentionDayId: String,
    val state: String,
    val dueAtEpochMs: Long?,
    val snoozedUntilEpochMs: Long?,
    val sessionId: String?,
    val updatedAtEpochMs: Long
)
