package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus

/**
 * Status surface exposed to Rhythmic Routine.
 *
 * This is meditation evidence only: Routine remains the policy authority for
 * cooldowns, requirements, and enforcement.
 */
interface MeditationStatusRepository {
    suspend fun createOrReplaceSession(request: MeditationRecoveryRequest): Result<Unit>
    suspend fun getSessionStatus(sessionId: String): MeditationRecoveryStatus?
    suspend fun markSessionCancelled(sessionId: String): Result<Unit>
}
