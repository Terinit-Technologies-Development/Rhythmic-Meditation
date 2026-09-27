package com.terinit.rhythmicmeditation.integration.contract

import android.content.ContentResolver
import android.net.Uri

/**
 * [AttentionInsightClient] backed by the platform ContentResolver.
 *
 * Fail-open by contract: an absent Routine, a denied provider, or any
 * malformed row yields null ("Rhythmic Routine not connected") — this read
 * path can never crash the app, affect policy, or block meditation.
 */
class ContentResolverAttentionInsightClient(
    private val contentResolver: ContentResolver
) : AttentionInsightClient {

    override fun attentionDay(attentionDayId: String): AttentionInsightProjection? {
        for (authority in AttentionInsightProtocol.AUTHORITIES) {
            val projection = query(authority, attentionDayId)
            if (projection != null) return projection
        }
        return null
    }

    private fun query(
        authority: String,
        attentionDayId: String
    ): AttentionInsightProjection? = try {
        val uri = Uri.parse(
            "content://$authority/${AttentionInsightProtocol.PATH_PREFIX}/$attentionDayId"
        )
        contentResolver.query(
            uri,
            AttentionInsightProtocol.COLUMNS.toTypedArray(),
            null,
            null,
            null
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val values = AttentionInsightProtocol.COLUMNS.associateWith { column ->
                val index = cursor.getColumnIndex(column)
                if (index < 0) null else cursor.getString(index)
            }
            AttentionInsightProtocol.parseRow(values)
        }
    } catch (e: Exception) {
        // Untrusted or unavailable projections are simply "not connected".
        null
    }
}
