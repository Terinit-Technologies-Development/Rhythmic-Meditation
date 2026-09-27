package com.terinit.rhythmicmeditation.integration.contract

/**
 * Rhythmic Routine's versioned per-Attention-Day insight projection.
 *
 * Read-only and fail-open: when the projection is unavailable or untrusted,
 * Insights shows partial data. It NEVER affects policy, NEVER blocks
 * meditation, and is NEVER used to infer substitutions from local history.
 */
data class AttentionInsightProjection(
    val protocolVersion: Int,
    val attentionDayId: String,
    val cooldownsTriggered: Int,
    val restorativeGatesCreated: Int,
    val restorativeGatesSatisfied: Int,
    val readerRestorativeCompletions: Int,
    val meditationRestorativeCompletions: Int,
    val meditationSubstitutionsUsed: Int,
    val meditationSubstitutionsRemaining: Int
)

/**
 * Narrow client for Routine's attention-insight ContentProvider.
 *
 * Implementations must degrade gracefully: return null when Routine is not
 * installed, not paired, or returns anything unparseable. Callers treat null
 * as "Rhythmic Routine not connected".
 */
interface AttentionInsightClient {

    /** Projection for one Attention Day, or null when unavailable/untrusted. */
    fun attentionDay(attentionDayId: String): AttentionInsightProjection?
}

/**
 * Protocol constants and the defensive row parser. Unknown
 * [PROTOCOL_VERSION] values and malformed rows are rejected as "unavailable" —
 * parsing never throws and never half-trusts a row.
 */
object AttentionInsightProtocol {

    /** Only this exact protocol version is understood. */
    const val PROTOCOL_VERSION = 1

    const val AUTHORITY = "com.terinit.rhythmicroutine.attention-insight"
    const val QA_AUTHORITY = "com.terinit.rhythmicroutine.qa.attention-insight"
    val AUTHORITIES = listOf(AUTHORITY, QA_AUTHORITY)

    const val PATH_PREFIX = "attention-day"

    val COLUMN_PROTOCOL_VERSION = "protocolVersion"
    val COLUMN_ATTENTION_DAY_ID = "attentionDayId"
    val COLUMN_COOLDOWNS_TRIGGERED = "cooldownsTriggered"
    val COLUMN_RESTORATIVE_GATES_CREATED = "restorativeGatesCreated"
    val COLUMN_RESTORATIVE_GATES_SATISFIED = "restorativeGatesSatisfied"
    val COLUMN_READER_RESTORATIVE_COMPLETIONS = "readerRestorativeCompletions"
    val COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS = "meditationRestorativeCompletions"
    val COLUMN_MEDITATION_SUBSTITUTIONS_USED = "meditationSubstitutionsUsed"
    val COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING = "meditationSubstitutionsRemaining"

    val COLUMNS = listOf(
        COLUMN_PROTOCOL_VERSION,
        COLUMN_ATTENTION_DAY_ID,
        COLUMN_COOLDOWNS_TRIGGERED,
        COLUMN_RESTORATIVE_GATES_CREATED,
        COLUMN_RESTORATIVE_GATES_SATISFIED,
        COLUMN_READER_RESTORATIVE_COMPLETIONS,
        COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS,
        COLUMN_MEDITATION_SUBSTITUTIONS_USED,
        COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING
    )

    private val COUNT_COLUMNS = listOf(
        COLUMN_COOLDOWNS_TRIGGERED,
        COLUMN_RESTORATIVE_GATES_CREATED,
        COLUMN_RESTORATIVE_GATES_SATISFIED,
        COLUMN_READER_RESTORATIVE_COMPLETIONS,
        COLUMN_MEDITATION_RESTORATIVE_COMPLETIONS,
        COLUMN_MEDITATION_SUBSTITUTIONS_USED,
        COLUMN_MEDITATION_SUBSTITUTIONS_REMAINING
    )

    /**
     * Parses one provider row defensively. Returns null (unavailable) for an
     * unknown protocol version, a blank attention day id, or any missing or
     * malformed value — a garbage row can never crash the app or show a
     * half-parsed projection.
     */
    fun parseRow(values: Map<String, String?>): AttentionInsightProjection? {
        val protocolVersion = values[COLUMN_PROTOCOL_VERSION]?.toIntOrNull()
        if (protocolVersion != PROTOCOL_VERSION) return null
        val attentionDayId = values[COLUMN_ATTENTION_DAY_ID]?.takeIf { it.isNotBlank() }
            ?: return null
        val counts = COUNT_COLUMNS.map { column ->
            val raw = values[column] ?: return null
            val parsed = raw.trim().toIntOrNull() ?: return null
            if (parsed < 0) return null
            parsed
        }
        return AttentionInsightProjection(
            protocolVersion = protocolVersion,
            attentionDayId = attentionDayId,
            cooldownsTriggered = counts[0],
            restorativeGatesCreated = counts[1],
            restorativeGatesSatisfied = counts[2],
            readerRestorativeCompletions = counts[3],
            meditationRestorativeCompletions = counts[4],
            meditationSubstitutionsUsed = counts[5],
            meditationSubstitutionsRemaining = counts[6]
        )
    }
}
