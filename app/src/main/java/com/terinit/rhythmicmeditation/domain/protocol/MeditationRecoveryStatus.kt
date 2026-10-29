package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Recovery status returned to Rhythmic Routine describing the meditation
 * evidence recorded for a session.
 *
 * The final three fields are additive identity metadata for binding-grade
 * evidence. They default to null so existing seven-field client code and
 * status rows from pre-metadata providers remain source-compatible. A null
 * [sessionKind] means the provider could not attest the session kind; a
 * consumer must treat that as an upgrade/capability limitation, never as a
 * match.
 */
data class MeditationRecoveryStatus(
    val sessionId: String,
    val protocolVersion: Int,
    val status: String,
    val requiredQualifiedSeconds: Int,
    val completedQualifiedSeconds: Int,
    val completedAtEpochMs: Long?,
    val lastUpdatedAtEpochMs: Long,
    val sessionKind: String? = null,
    val sourceCooldownId: String? = null,
    val sourceRhythmicDayId: String? = null
)
