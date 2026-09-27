package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.data.mapper.toRecoveryStatus
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService

/**
 * Local implementation of the [MeditationStatusRepository] contract.
 *
 * Delegates lifecycle rules to [MeditationSessionService] so the same state
 * machine governs in-app sessions and protocol-driven sessions.
 */
class LocalMeditationStatusRepository(
    private val sessionService: MeditationSessionService,
    private val sessionRepository: MeditationSessionRepository
) : MeditationStatusRepository {

    override suspend fun createOrReplaceSession(
        request: MeditationRecoveryRequest
    ): Result<Unit> = sessionService.createOrReplaceSession(request).map { }

    override suspend fun getSessionStatus(sessionId: String): MeditationRecoveryStatus? =
        sessionRepository.getSession(sessionId)?.toRecoveryStatus()

    override suspend fun markSessionCancelled(sessionId: String): Result<Unit> =
        sessionService.cancelSession(sessionId).map { }
}
