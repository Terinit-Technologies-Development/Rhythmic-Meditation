package com.terinit.rhythmicmeditation.integration.intent

import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.terinit.rhythmicmeditation.domain.protocol.MeditationContractFields
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation coverage for the recovery intent surface (real Android
 * Intent/Bundle classes). Malformed IPC must be rejected without exceptions.
 */
@RunWith(AndroidJUnit4::class)
class MeditationIntentsTest {

    private fun validRequest() = MeditationRecoveryRequest(
        sessionId = "routine-session-9",
        protocolVersion = 1,
        sessionKind = "COOLDOWN_RESTORATIVE",
        requiredQualifiedSeconds = 1_800,
        createdAtEpochMs = 1_700_000_000_000L,
        expiresAtEpochMs = null,
        sourceCooldownId = "cooldown-3",
        sourceRiskGroupId = "risk-2",
        sourceRhythmicDayId = "day-2"
    )

    @Test
    fun validRecoveryIntentRoundTrips() {
        val intent = MeditationIntents.buildStartRecoveryIntent(validRequest())

        val parsed = MeditationIntents.parseStartRecoveryIntent(intent)

        assertTrue(parsed.isSuccess)
        assertEquals(validRequest(), parsed.getOrThrow())
    }

    @Test
    fun wrongActionIsRejected() {
        val intent = Intent("com.example.SOMETHING_ELSE")
            .putExtra(
                MeditationProtocol.EXTRA_REQUEST_PAYLOAD,
                Bundle().apply { putString(MeditationContractFields.KEY_SESSION_ID, "x") }
            )

        assertTrue(MeditationIntents.parseStartRecoveryIntent(intent).isFailure)
    }

    @Test
    fun missingPayloadIsRejected() {
        val intent = Intent(MeditationProtocol.ACTION_START_MEDITATION_RECOVERY)

        assertTrue(MeditationIntents.parseStartRecoveryIntent(intent).isFailure)
    }

    @Test
    fun malformedPayloadIsRejected() {
        val junk = Bundle().apply { putString("unexpected", "payload") }
        val intent = Intent(MeditationProtocol.ACTION_START_MEDITATION_RECOVERY)
            .putExtra(MeditationProtocol.EXTRA_REQUEST_PAYLOAD, junk)

        assertTrue(MeditationIntents.parseStartRecoveryIntent(intent).isFailure)
    }

    @Test
    fun payloadWithInvalidFieldsIsRejected() {
        val bad = Bundle().apply {
            putString(MeditationContractFields.KEY_SESSION_ID, " ")
            putInt(MeditationContractFields.KEY_PROTOCOL_VERSION, 1)
            putString(MeditationContractFields.KEY_SESSION_KIND, "NOT_A_KIND")
            putInt(MeditationContractFields.KEY_REQUIRED_QUALIFIED_SECONDS, -5)
            putLong(MeditationContractFields.KEY_CREATED_AT_EPOCH_MS, 1L)
        }
        val intent = Intent(MeditationProtocol.ACTION_START_MEDITATION_RECOVERY)
            .putExtra(MeditationProtocol.EXTRA_REQUEST_PAYLOAD, bad)

        assertTrue(MeditationIntents.parseStartRecoveryIntent(intent).isFailure)
    }

    @Test
    fun nullIntentIsRejected() {
        assertTrue(MeditationIntents.parseStartRecoveryIntent(null).isFailure)
    }
}
