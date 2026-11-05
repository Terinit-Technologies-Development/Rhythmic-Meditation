package com.terinit.rhythmicmeditation.integration.provider

import com.terinit.rhythmicmeditation.data.mapper.toRecoveryStatus
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus
import com.terinit.rhythmicmeditation.domain.protocol.MeditationSessionIdentity
import com.terinit.rhythmicmeditation.domain.protocol.identityOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The status provider is the binding-grade evidence surface for Routine.
 *
 * These tests pin the compatibility rule: the frozen seven-column V1
 * projection is unchanged unless the caller explicitly asks for the additive
 * identity columns. They also prove that stored kind/source identities are
 * projected verbatim, and that a pre-metadata status is visibly unqualified
 * rather than silently accepted.
 */
class MeditationStatusProviderTest {

    private val legacy = MeditationProtocol.StatusColumns.LEGACY_COLUMNS
    private val identity = MeditationProtocol.StatusColumns.IDENTITY_COLUMNS
    private val all = MeditationProtocol.StatusColumns.ALL_COLUMNS

    private fun status(
        sessionKind: String? = "COOLDOWN_RESTORATIVE",
        sourceCooldownId: String? = "cd-2",
        sourceRhythmicDayId: String? = "day-2"
    ) = MeditationRecoveryStatus(
        sessionId = "session-42",
        protocolVersion = 1,
        status = "COMPLETED",
        requiredQualifiedSeconds = 1_800,
        completedQualifiedSeconds = 1_800,
        completedAtEpochMs = 1_700_000_100_000L,
        lastUpdatedAtEpochMs = 1_700_000_100_000L,
        sessionKind = sessionKind,
        sourceCooldownId = sourceCooldownId,
        sourceRhythmicDayId = sourceRhythmicDayId
    )

    private fun row(
        status: MeditationRecoveryStatus?,
        projection: Array<out String>?
    ): Map<String, Any?> {
        val projected = meditationStatusProjection(status, projection)
        val values = projected.row ?: return emptyMap()
        return projected.columns.zip(values).toMap()
    }

    @Test
    fun `legacy projection keeps the frozen seven-column shape`() {
        val projected = meditationStatusProjection(status(), legacy.toTypedArray())

        assertEquals(legacy, projected.columns)
        assertEquals(7, projected.row!!.size)
        assertTrue(identity.none { it in projected.columns })
    }

    @Test
    fun `null projection also keeps the legacy shape`() {
        val projected = meditationStatusProjection(status(), null)

        assertEquals(legacy, projected.columns)
        assertEquals(7, projected.row!!.size)
    }

    @Test
    fun `a legacy subset still gets the frozen seven-column shape`() {
        val projected = meditationStatusProjection(
            status(),
            arrayOf(MeditationProtocol.StatusColumns.STATUS)
        )

        assertEquals(legacy, projected.columns)
    }

    @Test
    fun `metadata projection returns stored identity values exactly`() {
        val stored = status()
        val projected = meditationStatusProjection(stored, all.toTypedArray())

        assertEquals(all, projected.columns)
        val values = row(stored, all.toTypedArray())
        assertEquals("session-42", values[MeditationProtocol.StatusColumns.SESSION_ID])
        assertEquals(1, values[MeditationProtocol.StatusColumns.PROTOCOL_VERSION])
        assertEquals("COMPLETED", values[MeditationProtocol.StatusColumns.STATUS])
        assertEquals(
            1_800,
            values[MeditationProtocol.StatusColumns.REQUIRED_QUALIFIED_SECONDS]
        )
        assertEquals(
            1_800,
            values[MeditationProtocol.StatusColumns.COMPLETED_QUALIFIED_SECONDS]
        )
        assertEquals(
            "COOLDOWN_RESTORATIVE",
            values[MeditationProtocol.StatusColumns.SESSION_KIND]
        )
        assertEquals("cd-2", values[MeditationProtocol.StatusColumns.SOURCE_COOLDOWN_ID])
        assertEquals("day-2", values[MeditationProtocol.StatusColumns.SOURCE_RHYTHMIC_DAY_ID])

        assertEquals(
            MeditationSessionIdentity(
                sessionId = "session-42",
                protocolVersion = 1,
                sessionKind = "COOLDOWN_RESTORATIVE",
                sourceCooldownId = "cd-2",
                sourceRhythmicDayId = "day-2"
            ),
            stored.identityOrNull()
        )
    }

    @Test
    fun `wrong kind or source is visible so a consumer can reject it`() {
        val stored = status(
            sessionKind = "MORNING_REQUIRED",
            sourceCooldownId = null,
            sourceRhythmicDayId = "day-9"
        )
        val values = row(stored, all.toTypedArray())
        val expected = MeditationSessionIdentity(
            sessionId = "session-42",
            protocolVersion = 1,
            sessionKind = "COOLDOWN_RESTORATIVE",
            sourceCooldownId = "cd-2",
            sourceRhythmicDayId = "day-2"
        )

        assertEquals(
            "MORNING_REQUIRED",
            values[MeditationProtocol.StatusColumns.SESSION_KIND]
        )
        assertNull(values[MeditationProtocol.StatusColumns.SOURCE_COOLDOWN_ID])
        assertEquals(
            "day-9",
            values[MeditationProtocol.StatusColumns.SOURCE_RHYTHMIC_DAY_ID]
        )
        assertNotEquals(expected, stored.identityOrNull())
    }

    @Test
    fun `pre-metadata status fails identity qualification`() {
        val legacyStatus = status(
            sessionKind = null,
            sourceCooldownId = null,
            sourceRhythmicDayId = null
        )
        val values = row(legacyStatus, all.toTypedArray())

        assertNull(values[MeditationProtocol.StatusColumns.SESSION_KIND])
        assertNull(values[MeditationProtocol.StatusColumns.SOURCE_COOLDOWN_ID])
        assertNull(values[MeditationProtocol.StatusColumns.SOURCE_RHYTHMIC_DAY_ID])
        assertNull(
            "missing metadata is an upgrade limitation, never a match",
            legacyStatus.identityOrNull()
        )
    }

    @Test
    fun `unknown session still advertises the requested schema`() {
        val projected = meditationStatusProjection(null, all.toTypedArray())

        assertNull(projected.row)
        assertEquals(all, projected.columns)
    }

    @Test
    fun `an identity request returns the full self-describing schema`() {
        val projected = meditationStatusProjection(
            status(),
            arrayOf(MeditationProtocol.StatusColumns.SESSION_KIND)
        )

        assertEquals(all, projected.columns)
    }

    @Test
    fun `unknown columns are ignored without changing the legacy contract`() {
        val projected = meditationStatusProjection(status(), arrayOf("notAColumn"))

        assertEquals(legacy, projected.columns)
        assertFalse(projected.columns.contains("notAColumn"))
    }

    @Test
    fun `session identity survives the recovery-status mapper`() {
        val session = MeditationSession(
            sessionId = "session-42",
            protocolVersion = 1,
            kind = MeditationSessionKind.COOLDOWN_RESTORATIVE,
            status = MeditationSessionStatus.COMPLETED,
            requiredSeconds = 1_800,
            completedQualifiedSeconds = 1_800,
            startedAtEpochMs = 1_700_000_000_000L,
            completedAtEpochMs = 1_700_000_100_000L,
            createdAtEpochMs = 1_700_000_000_000L,
            expiresAtEpochMs = null,
            sourceCooldownId = "cd-2",
            sourceRhythmicDayId = "day-2",
            interruptionCount = 0,
            pauseCount = 0
        )

        val status = session.toRecoveryStatus()
        val values = row(status, all.toTypedArray())

        assertEquals(
            "COOLDOWN_RESTORATIVE",
            values[MeditationProtocol.StatusColumns.SESSION_KIND]
        )
        assertEquals("cd-2", values[MeditationProtocol.StatusColumns.SOURCE_COOLDOWN_ID])
        assertEquals("day-2", values[MeditationProtocol.StatusColumns.SOURCE_RHYTHMIC_DAY_ID])
        assertEquals(
            MeditationSessionIdentity(
                sessionId = "session-42",
                protocolVersion = 1,
                sessionKind = "COOLDOWN_RESTORATIVE",
                sourceCooldownId = "cd-2",
                sourceRhythmicDayId = "day-2"
            ),
            status.identityOrNull()
        )
    }
}
