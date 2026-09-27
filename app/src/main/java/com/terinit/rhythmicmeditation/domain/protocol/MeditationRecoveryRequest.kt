package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Recovery request sent by Rhythmic Routine to (re)establish a meditation
 * session after restart, restore, or process death.
 *
 * Wire-level representation of a session requirement. Pure Kotlin so parsing
 * and validation stay unit-testable.
 */
data class MeditationRecoveryRequest(
    val sessionId: String,
    val protocolVersion: Int,
    val sessionKind: String,
    val requiredQualifiedSeconds: Int,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long?,
    val sourceCooldownId: String?,
    val sourceRiskGroupId: String?,
    val sourceRhythmicDayId: String?
)
