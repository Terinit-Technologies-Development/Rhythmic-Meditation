package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Room row for [com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent]. */
@Entity(
    tableName = "session_interruption_events",
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
data class SessionInterruptionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String,
    val type: String,
    val occurredAtEpochMs: Long,
    val note: String?
)
