package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Defensive parsing of Routine's attention-insight projection: an unknown
 * protocol version or a malformed row is "unavailable", never a crash and
 * never a half-trusted number. Insights degrades to partial data — policy is
 * never derived from this path.
 */
class AttentionInsightClientTest {

    private fun validRow(): Map<String, String?> = mapOf(
        AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "1",
        AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to "2026-09-27",
        AttentionInsightProtocol.COLUMN_COOLDOWNS_TRIGGERED to "3",
        AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_CREATED to "2",
        AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_SATISFIED to "1",
        AttentionInsightProtocol.COLUMN_READER_RESTORATIVE_COMPLETIONS to "1",
        AttentionInsightProtocol.COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS to "1",
        AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED to "1",
        AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING to "1"
    )

    @Test
    fun `valid row parses to a projection`() {
        val projection = AttentionInsightProtocol.parseRow(validRow())

        assertNotNull(projection)
        assertEquals(1, projection!!.protocolVersion)
        assertEquals("2026-09-27", projection.attentionDayId)
        assertEquals(3, projection.cooldownsTriggered)
        assertEquals(2, projection.restorativeGatesCreated)
        assertEquals(1, projection.restorativeGatesSatisfied)
        assertEquals(1, projection.readerRestorativeCompletions)
        assertEquals(1, projection.meditationRestorativeCompletions)
        assertEquals(1, projection.meditationSubstitutionsUsed)
        assertEquals(1, projection.meditationSubstitutionsRemaining)
    }

    @Test
    fun `unknown protocol version is unavailable`() {
        val row = validRow() + (AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "2")

        assertNull(AttentionInsightProtocol.parseRow(row))
        assertNull(
            AttentionInsightProtocol.parseRow(
                validRow() + (AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "0")
            )
        )
    }

    @Test
    fun `garbage row is unavailable and never throws`() {
        val row = validRow() + (AttentionInsightProtocol.COLUMN_COOLDOWNS_TRIGGERED to "three")

        assertNull(AttentionInsightProtocol.parseRow(row))
        assertNull(AttentionInsightProtocol.parseRow(emptyMap()))
        assertNull(AttentionInsightProtocol.parseRow(mapOf("unexpected" to "column")))
    }

    @Test
    fun `missing column is unavailable`() {
        val row = validRow() - AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED

        assertNull(AttentionInsightProtocol.parseRow(row))
        assertNull(
            AttentionInsightProtocol.parseRow(
                validRow() +
                    (AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING to null)
            )
        )
    }

    @Test
    fun `negative counts are unavailable`() {
        val row = validRow() +
            (AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_SATISFIED to "-1")

        assertNull(AttentionInsightProtocol.parseRow(row))
    }

    @Test
    fun `blank attention day id is unavailable`() {
        val row = validRow() + (AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to "  ")

        assertNull(AttentionInsightProtocol.parseRow(row))
    }

    @Test
    fun `substitution remaining derives only from the routine projection`() {
        fun remainingFor(used: String): Int {
            val projection = AttentionInsightProtocol.parseRow(
                validRow() + (
                    AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED to used
                    )
            )!!
            return MeditationInsights.substitutionChoicesRemaining(projection.meditationSubstitutionsUsed)
        }

        assertEquals(2, remainingFor("0"))
        assertEquals(1, remainingFor("1"))
        assertEquals(0, remainingFor("2"))
        assertEquals(0, remainingFor("5"))
    }

    @Test
    fun `zero valued row stays connected for the exact day`() {
        val row = validRow() + mapOf(
            AttentionInsightProtocol.COLUMN_COOLDOWNS_TRIGGERED to "0",
            AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_CREATED to "0",
            AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_SATISFIED to "0",
            AttentionInsightProtocol.COLUMN_READER_RESTORATIVE_COMPLETIONS to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING to "2"
        )

        val result = AttentionInsightProtocol.interpretRow("2026-09-27", row)

        assertTrue(result is AttentionInsightQueryResult.Connected)
        val projection = (result as AttentionInsightQueryResult.Connected).projection
        assertEquals("2026-09-27", projection.attentionDayId)
        assertEquals(0, projection.cooldownsTriggered)
        assertEquals(0, projection.restorativeGatesCreated)
        assertEquals(0, projection.restorativeGatesSatisfied)
        assertEquals(2, projection.meditationSubstitutionsRemaining)
    }

    @Test
    fun `typed parser distinguishes a version mismatch from malformed data`() {
        val mismatch = AttentionInsightProtocol.parseRowResult(
            validRow() + (AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "2")
        )
        val malformed = AttentionInsightProtocol.parseRowResult(
            validRow() + (AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "two")
        )

        assertTrue(mismatch is AttentionInsightRowResult.Rejected)
        assertEquals(
            AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH,
            (mismatch as AttentionInsightRowResult.Rejected).reason
        )
        assertEquals(2, mismatch.observedProtocolVersion)

        assertTrue(malformed is AttentionInsightRowResult.Rejected)
        assertEquals(
            AttentionInsightUnavailableReason.MALFORMED_ROW,
            (malformed as AttentionInsightRowResult.Rejected).reason
        )
    }

    @Test
    fun `row for another attention day is unexpected rather than connected`() {
        val result = AttentionInsightProtocol.interpretRow("2026-09-28", validRow())

        assertTrue(result is AttentionInsightQueryResult.Unavailable)
        val unavailable = result as AttentionInsightQueryResult.Unavailable
        assertEquals(
            AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY,
            unavailable.reason
        )
        assertEquals("2026-09-27", unavailable.observedAttentionDayId)
        assertEquals("2026-09-28", unavailable.expectedAttentionDayId)
    }

    @Test
    fun `default typed query rejects a mismatched projection day`() {
        val mismatched = AttentionInsightProtocol.parseRow(
            validRow() + (AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to "2026-09-28")
        )!!

        val result = FixedProjectionClient(mismatched).attentionDayResult("2026-09-27")

        assertTrue(result is AttentionInsightQueryResult.Unavailable)
        assertEquals(
            AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY,
            (result as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    @Test
    fun `fallback authority cannot hide the primary denial reason`() {
        val denial = AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED,
            authority = AttentionInsightProtocol.AUTHORITY
        )
        val missingQa = AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
            authority = AttentionInsightProtocol.QA_AUTHORITY
        )

        val selected = mostInformativeAttentionInsightResult(listOf(denial, missingQa))

        assertTrue(selected is AttentionInsightQueryResult.Unavailable)
        assertEquals(
            AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED,
            (selected as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    @Test
    fun `a connected fallback authority wins over a missing primary`() {
        val projection = AttentionInsightProtocol.parseRow(validRow())!!
        val missingProduction = AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
            authority = AttentionInsightProtocol.AUTHORITY
        )
        val connectedQa = AttentionInsightQueryResult.Connected(projection)

        val selected = mostInformativeAttentionInsightResult(listOf(missingProduction, connectedQa))

        assertEquals(connectedQa, selected)
    }

    @Test
    fun `reachability distinguishes an empty provider from an absent one`() {
        assertTrue(AttentionInsightUnavailableReason.EMPTY_CURRENT_DAY.isProviderReachable)
        assertTrue(AttentionInsightUnavailableReason.MALFORMED_ROW.isProviderReachable)
        assertTrue(
            AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH.isProviderReachable
        )
        assertFalse(AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED.isProviderReachable)
        assertFalse(
            AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED.isProviderReachable
        )
        assertTrue(
            AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE.isProviderReachable
        )
    }

    @Test
    fun `current selector accepts opaque identity and zero counts`() {
        assertEquals("current", AttentionInsightProtocol.CURRENT_SELECTOR)
        val row = validRow() + mapOf(
            AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to "routine-opaque-current-1",
            AttentionInsightProtocol.COLUMN_COOLDOWNS_TRIGGERED to "0",
            AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_CREATED to "0",
            AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_SATISFIED to "0",
            AttentionInsightProtocol.COLUMN_READER_RESTORATIVE_COMPLETIONS to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED to "0",
            AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING to "2"
        )

        val result = AttentionInsightProtocol.interpretCurrentRow(row)

        assertTrue(result is AttentionInsightQueryResult.Connected)
        val projection = (result as AttentionInsightQueryResult.Connected).projection
        assertEquals("routine-opaque-current-1", projection.attentionDayId)
        assertEquals(0, projection.cooldownsTriggered)
        assertEquals(0, projection.restorativeGatesCreated)
        assertEquals(0, projection.restorativeGatesSatisfied)
        assertEquals(0, projection.meditationSubstitutionsUsed)
    }

    @Test
    fun `exact query still rejects an opaque current identity`() {
        val row = validRow() + (
            AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to "routine-opaque-current-1"
            )

        val result = AttentionInsightProtocol.interpretRow("2026-09-27", row)

        assertTrue(result is AttentionInsightQueryResult.Unavailable)
        assertEquals(
            AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY,
            (result as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    @Test
    fun `current malformed and version mismatch rows are rejected`() {
        val versionMismatch = AttentionInsightProtocol.interpretCurrentRow(
            validRow() + (AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "2")
        )
        val malformed = AttentionInsightProtocol.interpretCurrentRow(
            validRow() - AttentionInsightProtocol.COLUMN_RESTORATIVE_GATES_CREATED
        )
        val selectorEcho = AttentionInsightProtocol.interpretCurrentRow(
            validRow() + (
                AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to
                    AttentionInsightProtocol.CURRENT_SELECTOR
                )
        )

        assertEquals(
            AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH,
            (versionMismatch as AttentionInsightQueryResult.Unavailable).reason
        )
        assertEquals(
            AttentionInsightUnavailableReason.MALFORMED_ROW,
            (malformed as AttentionInsightQueryResult.Unavailable).reason
        )
        assertEquals(
            AttentionInsightUnavailableReason.MALFORMED_ROW,
            (selectorEcho as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    @Test
    fun `current capability limitation outranks a missing fallback authority`() {
        val oldProvider = AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE,
            authority = AttentionInsightProtocol.AUTHORITY
        )
        val missingQa = AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
            authority = AttentionInsightProtocol.QA_AUTHORITY
        )

        val selected = mostInformativeAttentionInsightResult(listOf(oldProvider, missingQa))

        assertEquals(
            AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE,
            (selected as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    @Test
    fun `default current query is honestly unavailable for simple clients`() {
        val projection = AttentionInsightProtocol.parseRow(validRow())!!
        val result = FixedProjectionClient(projection).currentAttentionDayResult()

        assertTrue(result is AttentionInsightQueryResult.Unavailable)
        assertEquals(
            AttentionInsightUnavailableReason.UNKNOWN,
            (result as AttentionInsightQueryResult.Unavailable).reason
        )
    }

    private class FixedProjectionClient(
        private val projection: AttentionInsightProjection?
    ) : AttentionInsightClient {
        override fun attentionDay(attentionDayId: String): AttentionInsightProjection? = projection
    }
}
