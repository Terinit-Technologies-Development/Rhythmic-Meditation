package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room row for [com.terinit.rhythmicmeditation.domain.model.MeditationSession].
 * Enums are stored by name; see data/mapper for round-trip mapping.
 */
@Entity(tableName = "meditation_sessions")
data class MeditationSessionEntity(
    @PrimaryKey val sessionId: String,
    val protocolVersion: Int,
    val kind: String,
    val status: String,
    val requiredSeconds: Int,
    val completedQualifiedSeconds: Int,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long?,
    val sourceCooldownId: String?,
    val sourceRhythmicDayId: String?,
    val interruptionCount: Int,
    val pauseCount: Int
)
