package com.terinit.rhythmicmeditation.domain.protocol

import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind

/**
 * Validates [MeditationRecoveryRequest] payloads.
 *
 * Malformed or hostile payloads must never crash the app or create bogus
 * sessions: validation returns Result and callers propagate failures.
 */
object MeditationRecoveryRequestValidator {

    fun validate(request: MeditationRecoveryRequest): Result<Unit> {
        if (request.sessionId.isBlank()) {
            return Result.failure(IllegalArgumentException("sessionId is required"))
        }
        if (request.requiredQualifiedSeconds <= 0) {
            return Result.failure(IllegalArgumentException("requiredQualifiedSeconds must be > 0"))
        }
        if (request.protocolVersion <= 0) {
            return Result.failure(IllegalArgumentException("protocolVersion must be > 0"))
        }
        if (request.protocolVersion > MeditationProtocol.PROTOCOL_VERSION) {
            return Result.failure(
                IllegalArgumentException(
                    "protocolVersion ${request.protocolVersion} is newer than supported " +
                        MeditationProtocol.PROTOCOL_VERSION
                )
            )
        }
        val kind = MeditationSessionKind.fromWire(request.sessionKind)
            ?: return Result.failure(
                IllegalArgumentException("sessionKind '${request.sessionKind}' is not recognized")
            )
        if (kind == MeditationSessionKind.STANDALONE) {
            return Result.failure(
                IllegalArgumentException("STANDALONE sessions cannot be requested over the protocol")
            )
        }
        if (request.createdAtEpochMs <= 0L) {
            return Result.failure(IllegalArgumentException("createdAtEpochMs must be > 0"))
        }
        val expiresAt = request.expiresAtEpochMs
        if (expiresAt != null && expiresAt <= request.createdAtEpochMs) {
            return Result.failure(
                IllegalArgumentException("expiresAtEpochMs must be after createdAtEpochMs")
            )
        }
        return Result.success(Unit)
    }

    /** Convenience: validate and return the request on success. */
    fun validated(request: MeditationRecoveryRequest): Result<MeditationRecoveryRequest> =
        validate(request).map { request }
}
