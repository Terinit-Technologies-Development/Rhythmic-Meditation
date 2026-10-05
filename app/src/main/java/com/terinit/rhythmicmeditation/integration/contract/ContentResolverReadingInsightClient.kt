package com.terinit.rhythmicmeditation.integration.contract

import android.content.ContentResolver
import android.net.Uri

/**
 * [ReadingInsightClient] backed by the platform ContentResolver, reading
 * Reader's existing Daily Evidence V2 provider.
 *
 * Fail-open by contract: an absent Reader, mismatched protocol/date, or
 * malformed row yields null ("Rhythmic Reader not connected"). Only protocol/
 * date identity and the two evidence values are requested — no book titles,
 * contents, or annotations cross the boundary.
 */
class ContentResolverReadingInsightClient(
    private val contentResolver: ContentResolver
) : ReadingInsightClient {

    override fun dailyEvidence(dateKey: String): ReadingEvidence? = try {
        val uri = Uri.parse(
            "content://${ReadingEvidenceProtocol.AUTHORITY}/" +
                "${ReadingEvidenceProtocol.PATH_PREFIX}/$dateKey"
        )
        contentResolver.query(
            uri,
            ReadingEvidenceProtocol.COLUMNS.toTypedArray(),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val values = ReadingEvidenceProtocol.COLUMNS.associateWith { column ->
                val index = cursor.getColumnIndex(column)
                if (index < 0) null else cursor.getString(index)
            }
            ReadingEvidenceProtocol.parseRow(dateKey, values)
        }
    } catch (e: Exception) {
        // Untrusted or unavailable evidence is simply "not connected".
        null
    }
}
