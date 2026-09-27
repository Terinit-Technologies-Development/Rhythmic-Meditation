package com.terinit.rhythmicmeditation.integration.contract

import android.os.Bundle
import com.terinit.rhythmicmeditation.domain.protocol.MeditationContractFields
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus

/**
 * Bundle <-> DTO codec for the Routine <-> Meditation protocol.
 *
 * All decoding is defensive: malformed or missing fields produce a failure
 * result (via [MeditationContractFields]) instead of an exception. The pure
 * field-level logic is unit tested on the JVM; this class is thin glue.
 */
object MeditationContractCodec {

    fun encodeRequest(request: MeditationRecoveryRequest): Bundle =
        bundleOf(MeditationContractFields.fieldsOf(request))

    fun decodeRequest(bundle: Bundle?): Result<MeditationRecoveryRequest> {
        if (bundle == null) {
            return Result.failure(IllegalArgumentException("Malformed payload: request bundle is null"))
        }
        return MeditationContractFields.requestFromFields(fieldsOf(bundle))
    }

    fun encodeStatus(status: MeditationRecoveryStatus): Bundle =
        bundleOf(MeditationContractFields.fieldsOf(status))

    fun decodeStatus(bundle: Bundle?): Result<MeditationRecoveryStatus> {
        if (bundle == null) {
            return Result.failure(IllegalArgumentException("Malformed payload: status bundle is null"))
        }
        return MeditationContractFields.statusFromFields(fieldsOf(bundle))
    }

    private fun bundleOf(fields: Map<String, Any?>): Bundle {
        val bundle = Bundle()
        fields.forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        return bundle
    }

    private fun fieldsOf(bundle: Bundle): Map<String, Any?> {
        val keys = setOf(
            MeditationContractFields.KEY_SESSION_ID,
            MeditationContractFields.KEY_PROTOCOL_VERSION,
            MeditationContractFields.KEY_SESSION_KIND,
            MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS,
            MeditationContractFields.KEY_CREATED_AT_EPOCH_MS,
            MeditationContractFields.KEY_EXPIRES_AT_EPOCH_MS,
            MeditationContractFields.KEY_SOURCE_COOLDOWN_ID,
            MeditationContractFields.KEY_SOURCE_RISK_GROUP_ID,
            MeditationContractFields.KEY_SOURCE_RHYTHMIC_DAY_ID,
            MeditationContractFields.KEY_STATUS,
            MeditationContractFields.KEY_COMPLETED_QUALIFIED_SECONDS,
            MeditationContractFields.KEY_COMPLETED_AT_EPOCH_MS,
            MeditationContractFields.KEY_LAST_UPDATED_AT_EPOCH_MS
        )
        return keys.associateWith { key ->
            when {
                !bundle.containsKey(key) -> null
                else -> bundle.getString(key)
                    ?: bundle.getLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
                    ?: bundle.getInt(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
            }
        }
    }
}
