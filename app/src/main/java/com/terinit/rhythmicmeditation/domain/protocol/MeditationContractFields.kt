package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Pure field-level parsing for protocol payloads.
 *
 * The Bundle/Intent glue in integration/contract delegates here so that
 * malformed-payload handling can be unit tested on the JVM without Android
 * framework classes. Every failure is returned as Result — never thrown.
 */
object MeditationContractFields {

    // Request keys
    const val KEY_SESSION_ID = "session_id"
    const val KEY_PROTOCOL_VERSION = "protocol_version"
    const val KEY_SESSION_KIND = "session_kind"
    const val KEY_REQUIRED_QUALIFIED_SECONDS = "required_qualified_seconds"
    const val KEY_CREATED_AT_EPOCH_MS = "created_at_epoch_ms"
    const val KEY_EXPIRES_AT_EPOCH_MS = "expires_at_epoch_ms"
    const val KEY_SOURCE_COOLDOWN_ID = "source_cooldown_id"
    const val KEY_SOURCE_RISK_GROUP_ID = "source_risk_group_id"
    const val KEY_SOURCE_RHYTHMIC_DAY_ID = "source_rhythmic_day_id"

    // Status keys
    const val KEY_STATUS = "status"
    const val KEY_COMPLETED_QUALIFIED_SECONDS = "completed_qualified_seconds"
    const val KEY_COMPLETED_AT_EPOCH_MS = "completed_at_epoch_ms"
    const val KEY_LAST_UPDATED_AT_EPOCH_MS = "last_updated_at_epoch_ms"

    /**
     * Builds a request from an untyped field map (e.g. decoded extras).
     * Type mismatches and missing fields produce a failure result.
     */
    fun requestFromFields(fields: Map<String, Any?>): Result<MeditationRecoveryRequest> {
        val sessionId = fields.stringOf(KEY_SESSION_ID)
            ?: return Result.failure(malformed("$KEY_SESSION_ID is missing or not a String"))
        val protocolVersion = fields.intOf(KEY_PROTOCOL_VERSION)
            ?: return Result.failure(malformed("$KEY_PROTOCOL_VERSION is missing or not an Int"))
        val sessionKind = fields.stringOf(KEY_SESSION_KIND)
            ?: return Result.failure(malformed("$KEY_SESSION_KIND is missing or not a String"))
        val requiredSeconds = fields.intOf(KEY_REQUIRED_QUALIFIED_SECONDS)
            ?: return Result.failure(malformed("$KEY_REQUIRED_QUALIFIED_SECONDS is missing or not an Int"))
        val createdAt = fields.longOf(KEY_CREATED_AT_EPOCH_MS)
            ?: return Result.failure(malformed("$KEY_CREATED_AT_EPOCH_MS is missing or not a Long"))

        val request = MeditationRecoveryRequest(
            sessionId = sessionId,
            protocolVersion = protocolVersion,
            sessionKind = sessionKind,
            requiredQualifiedSeconds = requiredSeconds,
            createdAtEpochMs = createdAt,
            expiresAtEpochMs = fields.longOf(KEY_EXPIRES_AT_EPOCH_MS),
            sourceCooldownId = fields.stringOf(KEY_SOURCE_COOLDOWN_ID),
            sourceRiskGroupId = fields.stringOf(KEY_SOURCE_RISK_GROUP_ID),
            sourceRhythmicDayId = fields.stringOf(KEY_SOURCE_RHYTHMIC_DAY_ID)
        )
        return MeditationRecoveryRequestValidator.validated(request)
    }

    /** Builds a status from an untyped field map. */
    fun statusFromFields(fields: Map<String, Any?>): Result<MeditationRecoveryStatus> {
        val sessionId = fields.stringOf(KEY_SESSION_ID)
            ?: return Result.failure(malformed("$KEY_SESSION_ID is missing or not a String"))
        val protocolVersion = fields.intOf(KEY_PROTOCOL_VERSION)
            ?: return Result.failure(malformed("$KEY_PROTOCOL_VERSION is missing or not an Int"))
        val status = fields.stringOf(KEY_STATUS)
            ?: return Result.failure(malformed("$KEY_STATUS is missing or not a String"))
        val requiredSeconds = fields.intOf(KEY_REQUIRED_QUALIFIED_SECONDS)
            ?: return Result.failure(malformed("$KEY_REQUIRED_QUALIFIED_SECONDS is missing or not an Int"))
        val completedSeconds = fields.intOf(KEY_COMPLETED_QUALIFIED_SECONDS)
            ?: return Result.failure(malformed("$KEY_COMPLETED_QUALIFIED_SECONDS is missing or not an Int"))
        val lastUpdated = fields.longOf(KEY_LAST_UPDATED_AT_EPOCH_MS)
            ?: return Result.failure(malformed("$KEY_LAST_UPDATED_AT_EPOCH_MS is missing or not a Long"))

        return Result.success(
            MeditationRecoveryStatus(
                sessionId = sessionId,
                protocolVersion = protocolVersion,
                status = status,
                requiredQualifiedSeconds = requiredSeconds,
                completedQualifiedSeconds = completedSeconds,
                completedAtEpochMs = fields.longOf(KEY_COMPLETED_AT_EPOCH_MS),
                lastUpdatedAtEpochMs = lastUpdated
            )
        )
    }

    /** Flattens a request into a field map ready for Bundle encoding. */
    fun fieldsOf(request: MeditationRecoveryRequest): Map<String, Any?> = mapOf(
        KEY_SESSION_ID to request.sessionId,
        KEY_PROTOCOL_VERSION to request.protocolVersion,
        KEY_SESSION_KIND to request.sessionKind,
        KEY_REQUIRED_QUALIFIED_SECONDS to request.requiredQualifiedSeconds,
        KEY_CREATED_AT_EPOCH_MS to request.createdAtEpochMs,
        KEY_EXPIRES_AT_EPOCH_MS to request.expiresAtEpochMs,
        KEY_SOURCE_COOLDOWN_ID to request.sourceCooldownId,
        KEY_SOURCE_RISK_GROUP_ID to request.sourceRiskGroupId,
        KEY_SOURCE_RHYTHMIC_DAY_ID to request.sourceRhythmicDayId
    )

    /** Flattens a status into a field map ready for Bundle encoding. */
    fun fieldsOf(status: MeditationRecoveryStatus): Map<String, Any?> = mapOf(
        KEY_SESSION_ID to status.sessionId,
        KEY_PROTOCOL_VERSION to status.protocolVersion,
        KEY_STATUS to status.status,
        KEY_REQUIRED_QUALIFIED_SECONDS to status.requiredQualifiedSeconds,
        KEY_COMPLETED_QUALIFIED_SECONDS to status.completedQualifiedSeconds,
        KEY_COMPLETED_AT_EPOCH_MS to status.completedAtEpochMs,
        KEY_LAST_UPDATED_AT_EPOCH_MS to status.lastUpdatedAtEpochMs
    )

    private fun malformed(reason: String) =
        IllegalArgumentException("Malformed payload: $reason")

    private fun Map<String, Any?>.stringOf(key: String): String? = this[key] as? String

    private fun Map<String, Any?>.intOf(key: String): Int? = when (val value = this[key]) {
        is Int -> value
        is Long -> if (value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) value.toInt() else null
        is Number -> value.toInt()
        else -> null
    }

    private fun Map<String, Any?>.longOf(key: String): Long? = when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Number -> value.toLong()
        else -> null
    }
}
