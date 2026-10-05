package com.terinit.rhythmicmeditation.integration.contract

/**
 * Rhythmic Reader's Daily Evidence V2 projection for one date.
 *
 * Deliberately narrow: protocol/date identity plus verified reading SECONDS
 * and qualified PAGES only. No book titles, contents, or annotations ever
 * cross the boundary.
 */
data class ReadingEvidence(
    val dateKey: String,
    val verifiedActiveSeconds: Int,
    val qualifiedPages: Int
)

/**
 * Narrow client for Reader's existing Daily Evidence V2 provider
 * (`content://com.terinit.rhythmicreader.evidence/daily/{dateKey}`), guarded
 * by Reader's `com.terinit.rhythmicreader.permission.RECOVERY` signature
 * permission.
 *
 * Read-only and fail-open: when Reader is not installed or the row is
 * unavailable/untrusted, return null and Insights shows partial data. Missing
 * Reader never blocks meditation.
 */
interface ReadingInsightClient {

    /** Verified reading evidence for [dateKey], or null when unavailable. */
    fun dailyEvidence(dateKey: String): ReadingEvidence?
}

/**
 * Protocol constants and the defensive row parser for the Daily Evidence V2
 * provider. Only protocol/date identity and the two evidence values are ever
 * requested; a malformed or incompatible row is rejected as "unavailable",
 * never half-parsed and never thrown.
 */
object ReadingEvidenceProtocol {

    const val PROTOCOL_VERSION = 2
    const val AUTHORITY = "com.terinit.rhythmicreader.evidence"
    const val PATH_PREFIX = "daily"

    /** Signature permission guarding Reader's provider (declared read-only). */
    const val RECOVERY_PERMISSION = "com.terinit.rhythmicreader.permission.RECOVERY"

    const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    const val COLUMN_DATE_KEY = "dateKey"
    const val COLUMN_VERIFIED_ACTIVE_SECONDS = "verifiedActiveSeconds"
    const val COLUMN_QUALIFIED_PAGES = "qualifiedPages"

    /** Only protocol/date identity and aggregate evidence are requested. */
    val COLUMNS = listOf(
        COLUMN_PROTOCOL_VERSION,
        COLUMN_DATE_KEY,
        COLUMN_VERIFIED_ACTIVE_SECONDS,
        COLUMN_QUALIFIED_PAGES
    )

    /**
     * Parses one provider row defensively. A mismatched protocol or date is
     * unavailable, just like missing or malformed evidence.
     */
    fun parseRow(dateKey: String, values: Map<String, String?>): ReadingEvidence? {
        val protocolVersion = values[COLUMN_PROTOCOL_VERSION]
            ?.trim()?.toIntOrNull() ?: return null
        if (protocolVersion != PROTOCOL_VERSION) return null
        if (values[COLUMN_DATE_KEY]?.trim() != dateKey) return null

        val verifiedActiveSeconds = values[COLUMN_VERIFIED_ACTIVE_SECONDS]
            ?.trim()?.toIntOrNull() ?: return null
        val qualifiedPages = values[COLUMN_QUALIFIED_PAGES]
            ?.trim()?.toIntOrNull() ?: return null
        if (verifiedActiveSeconds < 0 || qualifiedPages < 0) return null
        return ReadingEvidence(
            dateKey = dateKey,
            verifiedActiveSeconds = verifiedActiveSeconds,
            qualifiedPages = qualifiedPages
        )
    }
}
