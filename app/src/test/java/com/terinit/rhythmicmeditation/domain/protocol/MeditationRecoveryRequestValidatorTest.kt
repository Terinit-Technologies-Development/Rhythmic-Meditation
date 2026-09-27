package com.terinit.rhythmicmeditation.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationRecoveryRequestValidatorTest {

    private fun validRequest(
        sessionId: String = "session-1",
        protocolVersion: Int = MeditationProtocol.PROTOCOL_VERSION,
        sessionKind: String = "MORNING_REQUIRED",
        requiredQualifiedSeconds: Int = 1800,
        createdAtEpochMs: Long = 1_700_000_000_000L,
        expiresAtEpochMs: Long? = 1_700_000_300_000L
    ) = MeditationRecoveryRequest(
        sessionId = sessionId,
        protocolVersion = protocolVersion,
        sessionKind = sessionKind,
        requiredQualifiedSeconds = requiredQualifiedSeconds,
        createdAtEpochMs = createdAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
        sourceCooldownId = "cooldown-4",
        sourceRiskGroupId = null,
        sourceRhythmicDayId = "day-1"
    )

    @Test
    fun `valid request passes`() {
        assertTrue(MeditationRecoveryRequestValidator.validate(validRequest()).isSuccess)
    }

    @Test
    fun `blank sessionId fails`() {
        val result = MeditationRecoveryRequestValidator.validate(validRequest(sessionId = " "))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `non-positive requiredQualifiedSeconds fails`() {
        listOf(0, -1).forEach { seconds ->
            val result = MeditationRecoveryRequestValidator.validate(
                validRequest(requiredQualifiedSeconds = seconds)
            )
            assertTrue("seconds=$seconds should fail", result.isFailure)
        }
    }

    @Test
    fun `non-positive protocolVersion fails`() {
        listOf(0, -3).forEach { version ->
            val result = MeditationRecoveryRequestValidator.validate(
                validRequest(protocolVersion = version)
            )
            assertTrue("version=$version should fail", result.isFailure)
        }
    }

    @Test
    fun `newer protocolVersion than supported fails`() {
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(protocolVersion = MeditationProtocol.PROTOCOL_VERSION + 1)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `unknown sessionKind fails`() {
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(sessionKind = "POWER_NAP")
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `standalone kind cannot be requested over the protocol`() {
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(sessionKind = "STANDALONE")
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `non-positive createdAtEpochMs fails`() {
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(createdAtEpochMs = 0L, expiresAtEpochMs = null)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `expiresAt must be after createdAt`() {
        val createdAt = 1_700_000_000_000L
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(createdAtEpochMs = createdAt, expiresAtEpochMs = createdAt)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `null expiresAt is allowed`() {
        val result = MeditationRecoveryRequestValidator.validate(
            validRequest(expiresAtEpochMs = null)
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validated returns the request on success`() {
        val request = validRequest()
        val result = MeditationRecoveryRequestValidator.validated(request)
        assertTrue(result.isSuccess)
        assertEquals(request, result.getOrNull())
    }
}
