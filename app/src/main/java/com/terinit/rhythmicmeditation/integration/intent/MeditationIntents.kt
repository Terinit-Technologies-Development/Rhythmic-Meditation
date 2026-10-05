package com.terinit.rhythmicmeditation.integration.intent

import android.content.Intent
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.integration.contract.MeditationContractCodec

/**
 * Intent surface used by Rhythmic Routine to hand a meditation session to
 * this app (including after restart / process restore).
 *
 * MainActivity handles valid requests through RecoveryRequestHandler, verifies
 * the caller, and routes the exact persisted session into the runtime.
 */
object MeditationIntents {

    /** Builds the recovery intent Routine sends to Meditation. */
    fun buildStartRecoveryIntent(request: MeditationRecoveryRequest): Intent =
        Intent(MeditationProtocol.ACTION_START_MEDITATION_RECOVERY)
            .setPackage("com.terinit.rhythmicmeditation")
            .putExtra(
                MeditationProtocol.EXTRA_REQUEST_PAYLOAD,
                MeditationContractCodec.encodeRequest(request)
            )

    /**
     * Parses and validates a recovery intent. Fails on wrong action, missing
     * payload, or malformed fields — never throws.
     */
    fun parseStartRecoveryIntent(intent: Intent?): Result<MeditationRecoveryRequest> {
        if (intent == null) {
            return Result.failure(IllegalArgumentException("Malformed payload: intent is null"))
        }
        if (intent.action != MeditationProtocol.ACTION_START_MEDITATION_RECOVERY) {
            return Result.failure(
                IllegalArgumentException("Unexpected action '${intent.action}'")
            )
        }
        val payload = intent.getBundleExtra(MeditationProtocol.EXTRA_REQUEST_PAYLOAD)
            ?: return Result.failure(
                IllegalArgumentException(
                    "Malformed payload: missing ${MeditationProtocol.EXTRA_REQUEST_PAYLOAD}"
                )
            )
        return MeditationContractCodec.decodeRequest(payload)
    }
}
