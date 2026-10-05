package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
}
