package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequestValidator
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService

/**
 * Identity of an IPC caller as supplied by the transport layer. Where the
 * platform cannot attest a caller, both fields are null/empty and verification
 * fails closed.
 */
data class CallerIdentity(
    val packageName: String?,
    val signingCertificateDigests: Set<String>
) {
    companion object {
        val UNKNOWN = CallerIdentity(null, emptySet())
    }
}

/** Outcome of a handled recovery request. */
sealed interface RecoveryOutcome {
    /** The session exists (created or reloaded) and the UI should route to it. */
    data class RouteToSession(
        val sessionId: String,
        val alreadyCompleted: Boolean
    ) : RecoveryOutcome
}

/**
 * Handles Routine recovery requests end to end:
 *
 *   arrive -> validate protocol -> verify caller -> create/load session
 *            idempotently -> route into the session UI
 *
 * Caller verification is deny-by-default and lives in [CallerVerifier]; this
 * class never bypasses it (no debug shortcuts). Tests inject a fake verifier /
 * trust policy — production keeps rejecting until the peer is paired.
 */
class RecoveryRequestHandler(
    private val callerVerifier: CallerVerifier,
    private val sessionService: MeditationSessionService,
    private val sessionStatusRepository: MeditationStatusRepository
) {

    suspend fun handle(
        request: MeditationRecoveryRequest,
        caller: CallerIdentity
    ): Result<RecoveryOutcome> {
        // 1. Protocol validation (malformed payloads are rejected here).
        MeditationRecoveryRequestValidator.validate(request)
            .getOrElse { return Result.failure(it) }

        // 2. Caller verification. Deny-by-default; never skipped.
        callerVerifier.verifyCaller(
            callingPackage = caller.packageName,
            callingSigningCertificateDigests = caller.signingCertificateDigests
        ).getOrElse { return Result.failure(it) }

        // 3. Idempotent create/load: the same request never creates two sessions.
        sessionService.createOrReplaceSession(request)
            .getOrElse { return Result.failure(it) }

        val status = sessionStatusRepository.getSessionStatus(request.sessionId)
            ?: return Result.failure(
                IllegalStateException("Session '${request.sessionId}' could not be loaded")
            )

        return Result.success(
            RecoveryOutcome.RouteToSession(
                sessionId = request.sessionId,
                alreadyCompleted = status.status == "COMPLETED"
            )
        )
    }
}
