package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room row for [com.terinit.rhythmicmeditation.domain.model.MeditationInterval].
 *
 * Elapsed (monotonic) columns are authoritative for qualification; wall-clock
 * columns are diagnostics only.
 */
@Entity(
    tableName = "meditation_intervals",
    indices = [Index(value = ["sessionId"])],
    foreignKeys = [
        ForeignKey(
            entity = MeditationSessionEntity::class,
            parentColumns = ["sessionId"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class MeditationIntervalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val startedElapsedRealtimeMs: Long,
    val endedElapsedRealtimeMs: Long?,
    val startedWallClockMs: Long,
    val endedWallClockMs: Long?
)
