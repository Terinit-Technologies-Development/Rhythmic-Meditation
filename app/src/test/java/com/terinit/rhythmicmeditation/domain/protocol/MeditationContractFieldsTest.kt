package com.terinit.rhythmicmeditation.domain.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationContractFieldsTest {

    private fun validFields(): Map<String, Any?> = mapOf(
        MeditationContractFields.KEY_SESSION_ID to "session-1",
        MeditationContractFields.KEY_PROTOCOL_VERSION to 1,
        MeditationContractFields.KEY_SESSION_KIND to "COOLDOWN_RESTORATIVE",
        MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS to 1800,
        MeditationContractFields.KEY_CREATED_AT_EPOCH_MS to 1_700_000_000_000L,
        MeditationContractFields.KEY_EXPIRES_AT_EPOCH_MS to 1_700_000_300_000L,
        MeditationContractFields.KEY_SOURCE_COOLDOWN_ID to "cooldown-4",
        MeditationContractFields.KEY_SOURCE_RISK_GROUP_ID to null,
        MeditationContractFields.KEY_SOURCE_RHYTHMIC_DAY_ID to "day-1"
    )

    @Test
    fun `valid field map decodes to a request`() {
        val result = MeditationContractFields.requestFromFields(validFields())
        assertTrue(result.isSuccess)
        val request = result.getOrThrow()
        assertEquals("session-1", request.sessionId)
        assertEquals("COOLDOWN_RESTORATIVE", request.sessionKind)
        assertEquals(1800, request.requiredQualifiedSeconds)
        assertEquals("cooldown-4", request.sourceCooldownId)
        assertEquals(null, request.sourceRiskGroupId)
    }

    @Test
    fun `empty map is malformed`() {
        val result = MeditationContractFields.requestFromFields(emptyMap())
        assertTrue(result.isFailure)
    }

    @Test
    fun `missing sessionId is malformed`() {
        val result = MeditationContractFields.requestFromFields(
            validFields() - MeditationContractFields.KEY_SESSION_ID
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `wrong type for protocolVersion is malformed`() {
        val result = MeditationContractFields.requestFromFields(
            validFields() + (MeditationContractFields.KEY_PROTOCOL_VERSION to "one")
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `wrong type for requiredQualifiedSeconds is malformed`() {
        val result = MeditationContractFields.requestFromFields(
            validFields() + (MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS to "1800")
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `long values are accepted where ints are expected`() {
        val result = MeditationContractFields.requestFromFields(
            validFields() +
                (MeditationContractFields.KEY_PROTOCOL_VERSION to 1L) +
                (MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS to 1800L)
        )
        assertTrue(result.isSuccess)
    }

    @Test
    fun `payload failing validation is rejected`() {
        val result = MeditationContractFields.requestFromFields(
            validFields() + (MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS to 0)
        )
        assertTrue(result.isFailure)
    }

    @Test
    fun `field round trip preserves the request`() {
        val request = MeditationContractFields.requestFromFields(validFields()).getOrThrow()
        val decoded = MeditationContractFields.requestFromFields(
            MeditationContractFields.fieldsOf(request)
        ).getOrThrow()
        assertEquals(request, decoded)
    }

    @Test
    fun `status decodes and round trips`() {
        val status = MeditationRecoveryStatus(
            sessionId = "session-1",
            protocolVersion = 1,
            status = "ACTIVE",
            requiredQualifiedSeconds = 1800,
            completedQualifiedSeconds = 768,
            completedAtEpochMs = null,
            lastUpdatedAtEpochMs = 1_700_000_000_000L
        )
        val decoded = MeditationContractFields.statusFromFields(
            MeditationContractFields.fieldsOf(status)
        ).getOrThrow()
        assertEquals(status, decoded)
    }

    @Test
    fun `status with missing fields is malformed`() {
        val result = MeditationContractFields.statusFromFields(
            mapOf(MeditationContractFields.KEY_SESSION_ID to "session-1")
        )
        assertTrue(result.isFailure)
    }
}
