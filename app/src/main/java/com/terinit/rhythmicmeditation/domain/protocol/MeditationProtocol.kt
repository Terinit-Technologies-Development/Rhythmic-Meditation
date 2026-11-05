package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Protocol constants for Routine <-> Meditation coordination.
 *
 * Rhythmic Routine is the policy authority; Meditation only exposes evidence
 * about meditation sessions through this contract.
 */
object MeditationProtocol {
    const val PROTOCOL_VERSION = 1

    const val ACTION_START_MEDITATION_RECOVERY =
        "com.terinit.rhythmicmeditation.action.START_MEDITATION_RECOVERY"

    const val EXTRA_REQUEST_PAYLOAD = "extra_request_payload"

    const val STATUS_PROVIDER_AUTHORITY =
        "com.terinit.rhythmicmeditation.status"

    /**
     * Column names exposed by the status provider.
     *
     * The seven legacy columns are frozen for existing V1 clients. The three
     * identity columns are additive and are only returned when the caller
     * explicitly requests them, so old seven-column projections keep their
     * exact shape. Protocol V1 is unchanged.
     */
    object StatusColumns {
        const val SESSION_ID = "sessionId"
        const val PROTOCOL_VERSION = "protocolVersion"
        const val STATUS = "status"
        const val REQUIRED_QUALIFIED_SECONDS = "requiredQualifiedSeconds"
        const val COMPLETED_QUALIFIED_SECONDS = "completedQualifiedSeconds"
        const val COMPLETED_AT_EPOCH_MS = "completedAtEpochMs"
        const val LAST_UPDATED_AT_EPOCH_MS = "lastUpdatedAtEpochMs"

        /** Additive binding-grade identity (may be absent on older providers). */
        const val SESSION_KIND = "sessionKind"
        const val SOURCE_COOLDOWN_ID = "sourceCooldownId"
        const val SOURCE_RHYTHMIC_DAY_ID = "sourceRhythmicDayId"

        /** Frozen V1 seven-column projection. */
        val LEGACY_COLUMNS = listOf(
            SESSION_ID,
            PROTOCOL_VERSION,
            STATUS,
            REQUIRED_QUALIFIED_SECONDS,
            COMPLETED_QUALIFIED_SECONDS,
            COMPLETED_AT_EPOCH_MS,
            LAST_UPDATED_AT_EPOCH_MS
        )

        /** Additive identity columns; pre-metadata providers omit these. */
        val IDENTITY_COLUMNS = listOf(
            SESSION_KIND,
            SOURCE_COOLDOWN_ID,
            SOURCE_RHYTHMIC_DAY_ID
        )

        val ALL_COLUMNS = LEGACY_COLUMNS + IDENTITY_COLUMNS
    }
}

/**
 * Exact binding identity a consumer must check before qualifying a session.
 *
 * A status produced by a pre-metadata provider has a null [sessionKind], so
 * [identityOrNull] returns null: that is a visible upgrade/capability
 * limitation, not a match. Source fields may legitimately be null for a
 * morning session; the consumer still compares them exactly.
 */
data class MeditationSessionIdentity(
    val sessionId: String,
    val protocolVersion: Int,
    val sessionKind: String,
    val sourceCooldownId: String?,
    val sourceRhythmicDayId: String?
)

fun MeditationRecoveryStatus.identityOrNull(): MeditationSessionIdentity? {
    val kind = sessionKind?.takeIf { it.isNotBlank() } ?: return null
    return MeditationSessionIdentity(
        sessionId = sessionId,
        protocolVersion = protocolVersion,
        sessionKind = kind,
        sourceCooldownId = sourceCooldownId,
        sourceRhythmicDayId = sourceRhythmicDayId
    )
}
