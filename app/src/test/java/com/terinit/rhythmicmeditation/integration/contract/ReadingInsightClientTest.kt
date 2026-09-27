package com.terinit.rhythmicmeditation.integration.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Defensive parsing of Reader's Daily Evidence V2 projection. The boundary is
 * narrow on purpose: verified seconds and qualified pages only — no book
 * titles, contents, or annotations ever cross it.
 */
class ReadingInsightClientTest {

    private val dateKey = "2026-09-27"

    private fun validRow(): Map<String, String?> = mapOf(
        ReadingEvidenceProtocol.COLUMN_VERIFIED_ACTIVE_SECONDS to "5400",
        ReadingEvidenceProtocol.COLUMN_QUALIFIED_PAGES to "42"
    )

    @Test
    fun `valid row parses to reading evidence`() {
        val evidence = ReadingEvidenceProtocol.parseRow(dateKey, validRow())

        assertNotNull(evidence)
        assertEquals(dateKey, evidence!!.dateKey)
        assertEquals(5_400, evidence.verifiedActiveSeconds)
        assertEquals(42, evidence.qualifiedPages)
    }

    @Test
    fun `malformed row is unavailable and never throws`() {
        assertNull(
            ReadingEvidenceProtocol.parseRow(
                dateKey,
                validRow() + (ReadingEvidenceProtocol.COLUMN_QUALIFIED_PAGES to "forty-two")
            )
        )
        assertNull(ReadingEvidenceProtocol.parseRow(dateKey, emptyMap()))
    }

    @Test
    fun `missing values are unavailable`() {
        assertNull(
            ReadingEvidenceProtocol.parseRow(
                dateKey,
                validRow() + (ReadingEvidenceProtocol.COLUMN_VERIFIED_ACTIVE_SECONDS to null)
            )
        )
    }

    @Test
    fun `negative values are unavailable`() {
        assertNull(
            ReadingEvidenceProtocol.parseRow(
                dateKey,
                validRow() + (ReadingEvidenceProtocol.COLUMN_VERIFIED_ACTIVE_SECONDS to "-60")
            )
        )
    }

    @Test
    fun `only the two evidence columns are ever requested`() {
        assertEquals(
            listOf("verifiedActiveSeconds", "qualifiedPages"),
            ReadingEvidenceProtocol.COLUMNS
        )
        val forbidden = listOf("title", "book", "author", "annotation", "content", "text")
        ReadingEvidenceProtocol.COLUMNS.forEach { column ->
            forbidden.forEach { token ->
                assertFalse(
                    "column '$column' must not carry book content",
                    column.contains(token, ignoreCase = true)
                )
            }
        }
    }
}
