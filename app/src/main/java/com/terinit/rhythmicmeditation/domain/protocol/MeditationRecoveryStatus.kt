package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Recovery status returned to Rhythmic Routine describing the meditation
 * evidence recorded for a session.
 */
data class MeditationRecoveryStatus(
    val sessionId: String,
    val protocolVersion: Int,
    val status: String,
    val requiredQualifiedSeconds: Int,
    val completedQualifiedSeconds: Int,
    val completedAtEpochMs: Long?,
    val lastUpdatedAtEpochMs: Long
)
