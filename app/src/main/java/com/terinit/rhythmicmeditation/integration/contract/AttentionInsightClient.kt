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
 * Honest, privacy-safe reason a projection could not be used.
 *
 * These are diagnostic categories only. They never affect policy, never
 * block meditation, and never carry provider exception messages or stack
 * traces across the boundary.
 */
enum class AttentionInsightUnavailableReason {
    /** No Routine provider is installed for either authority. */
    PROVIDER_NOT_INSTALLED,

    /** The provider exists but the caller lacks the signature permission. */
    SIGNATURE_OR_PERMISSION_DENIED,

    /** The provider answered with a null cursor (not the same as empty). */
    NULL_CURSOR,

    /** A valid provider answered, but has no row for the expected day yet. */
    EMPTY_CURRENT_DAY,

    /**
     * The provider is reachable but does not support the additive
     * `attention-day/current` selector, or returned no current projection.
     * This is an old-provider compatibility limitation, never a guessed day.
     */
    CURRENT_PROJECTION_UNAVAILABLE,

    /** A row was returned but its required values were missing/invalid. */
    MALFORMED_ROW,

    /** The provider speaks a protocol version this build does not understand. */
    PROTOCOL_VERSION_MISMATCH,

    /** A valid row described a different Attention Day than the one requested. */
    UNEXPECTED_ATTENTION_DAY,

    /** The query failed at the transport/platform layer. */
    TRANSPORT_ERROR,

    /** Compatibility fallback for clients that only implement the nullable API. */
    UNKNOWN
}

/**
 * True when the provider itself answered, even though the projection is not
 * usable for the expected day. Used only for honest UI copy ("connected but
 * no insight yet") — never for policy.
 */
val AttentionInsightUnavailableReason.isProviderReachable: Boolean
    get() = when (this) {
        AttentionInsightUnavailableReason.EMPTY_CURRENT_DAY,
        AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE,
        AttentionInsightUnavailableReason.MALFORMED_ROW,
        AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH,
        AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY -> true
        else -> false
    }

/** Additive typed result for one Attention Day query. */
sealed interface AttentionInsightQueryResult {

    /** A usable projection for the exact expected Attention Day. */
    data class Connected(
        val projection: AttentionInsightProjection
    ) : AttentionInsightQueryResult

    /** No usable projection; [reason] explains why without leaking internals. */
    data class Unavailable(
        val reason: AttentionInsightUnavailableReason,
        val authority: String? = null,
        val expectedAttentionDayId: String? = null,
        val observedAttentionDayId: String? = null,
        val observedProtocolVersion: Int? = null
    ) : AttentionInsightQueryResult
}

/**
 * Narrow client for Routine's attention-insight ContentProvider.
 *
 * Implementations must degrade gracefully and may never crash the app. The
 * original nullable method stays for source/binary compatibility; new callers
 * use [attentionDayResult] to distinguish "not installed" from "denied",
 * "empty", "malformed", and "wrong day". Exact-ID queries are connected only
 * when the returned identity matches; [currentAttentionDayResult] instead
 * accepts Routine's opaque current identity under the coordinated selector.
 */
interface AttentionInsightClient {

    /** Projection for one Attention Day, or null when unavailable/untrusted. */
    fun attentionDay(attentionDayId: String): AttentionInsightProjection?

    /**
     * Additive typed query. The default preserves compatibility for simple
     * implementations; production overrides it with precise diagnostics.
     */
    fun attentionDayResult(attentionDayId: String): AttentionInsightQueryResult {
        val projection = attentionDay(attentionDayId)
            ?: return AttentionInsightQueryResult.Unavailable(
                reason = AttentionInsightUnavailableReason.UNKNOWN,
                expectedAttentionDayId = attentionDayId
            )
        return if (projection.attentionDayId == attentionDayId) {
            AttentionInsightQueryResult.Connected(projection)
        } else {
            AttentionInsightQueryResult.Unavailable(
                reason = AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY,
                expectedAttentionDayId = attentionDayId,
                observedAttentionDayId = projection.attentionDayId
            )
        }
    }

    /**
     * Projection for Rhythmic Routine's own current Attention Day, or null when
     * unavailable. Additive capability; callers must not substitute a local
     * calendar date or evening signal for the Routine-owned identity.
     */
    fun currentAttentionDay(): AttentionInsightProjection? =
        (currentAttentionDayResult() as? AttentionInsightQueryResult.Connected)
            ?.projection

    /**
     * Additive current-day query using the coordinated
     * `attention-day/current` selector. The default is unavailable so simple
     * implementations remain source-compatible; the production client
     * overrides it with the real provider query.
     */
    fun currentAttentionDayResult(): AttentionInsightQueryResult =
        AttentionInsightQueryResult.Unavailable(
            reason = AttentionInsightUnavailableReason.UNKNOWN,
            expectedAttentionDayId = AttentionInsightProtocol.CURRENT_SELECTOR
        )
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

    /**
     * Additive selector for Routine's real current Attention Day. It is an
     * opaque capability selector, not a calendar date guessed by Meditation.
     * Exact-ID queries keep their existing exact-match semantics.
     */
    const val CURRENT_SELECTOR = "current"

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
     *
     * Kept for compatibility; new callers use [parseRowResult] when they need
     * to distinguish a version mismatch from malformed data.
     */
    fun parseRow(values: Map<String, String?>): AttentionInsightProjection? =
        (parseRowResult(values) as? AttentionInsightRowResult.Parsed)?.projection

    /** Typed defensive parser; never throws. */
    fun parseRowResult(values: Map<String, String?>): AttentionInsightRowResult {
        val rawVersion = values[COLUMN_PROTOCOL_VERSION]?.trim()
            ?: return AttentionInsightRowResult.Rejected(
                AttentionInsightUnavailableReason.MALFORMED_ROW
            )
        val protocolVersion = rawVersion.toIntOrNull()
            ?: return AttentionInsightRowResult.Rejected(
                AttentionInsightUnavailableReason.MALFORMED_ROW
            )
        if (protocolVersion != PROTOCOL_VERSION) {
            return AttentionInsightRowResult.Rejected(
                reason = AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH,
                observedProtocolVersion = protocolVersion
            )
        }
        val attentionDayId = values[COLUMN_ATTENTION_DAY_ID]
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return AttentionInsightRowResult.Rejected(
                AttentionInsightUnavailableReason.MALFORMED_ROW
            )
        val counts = COUNT_COLUMNS.map { column ->
            val raw = values[column]
                ?: return AttentionInsightRowResult.Rejected(
                    AttentionInsightUnavailableReason.MALFORMED_ROW
                )
            val parsed = raw.trim().toIntOrNull()
                ?: return AttentionInsightRowResult.Rejected(
                    AttentionInsightUnavailableReason.MALFORMED_ROW
                )
            if (parsed < 0) {
                return AttentionInsightRowResult.Rejected(
                    AttentionInsightUnavailableReason.MALFORMED_ROW
                )
            }
            parsed
        }
        return AttentionInsightRowResult.Parsed(
            AttentionInsightProjection(
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
        )
    }

    /**
     * Evaluates one row against the exact expected Attention Day. Zero counts
     * are valid evidence and remain [AttentionInsightQueryResult.Connected];
     * a row for any other day is explicitly rejected as unexpected.
     */
    fun interpretRow(
        expectedAttentionDayId: String,
        values: Map<String, String?>
    ): AttentionInsightQueryResult = when (val parsed = parseRowResult(values)) {
        is AttentionInsightRowResult.Parsed -> {
            val projection = parsed.projection
            if (projection.attentionDayId == expectedAttentionDayId) {
                AttentionInsightQueryResult.Connected(projection)
            } else {
                AttentionInsightQueryResult.Unavailable(
                    reason = AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY,
                    expectedAttentionDayId = expectedAttentionDayId,
                    observedAttentionDayId = projection.attentionDayId
                )
            }
        }
        is AttentionInsightRowResult.Rejected -> AttentionInsightQueryResult.Unavailable(
            reason = parsed.reason,
            expectedAttentionDayId = expectedAttentionDayId,
            observedProtocolVersion = parsed.observedProtocolVersion
        )
    }

    /**
     * Evaluates a current-selector row. Current is the one query where a
     * nonblank opaque Attention Day identity is expected rather than compared
     * with a caller-supplied ID. Protocol and count validation are unchanged;
     * zero counts remain connected and no gate is created.
     */
    fun interpretCurrentRow(values: Map<String, String?>): AttentionInsightQueryResult =
        when (val parsed = parseRowResult(values)) {
            is AttentionInsightRowResult.Parsed -> {
                val projection = parsed.projection
                if (projection.attentionDayId == CURRENT_SELECTOR) {
                    // Echoing the selector is not a real routine identity.
                    AttentionInsightQueryResult.Unavailable(
                        reason = AttentionInsightUnavailableReason.MALFORMED_ROW,
                        expectedAttentionDayId = CURRENT_SELECTOR,
                        observedAttentionDayId = projection.attentionDayId
                    )
                } else {
                    AttentionInsightQueryResult.Connected(projection)
                }
            }
            is AttentionInsightRowResult.Rejected -> AttentionInsightQueryResult.Unavailable(
                reason = parsed.reason,
                expectedAttentionDayId = CURRENT_SELECTOR,
                observedProtocolVersion = parsed.observedProtocolVersion
            )
        }
}

/** Outcome of defensively parsing one provider row. */
sealed interface AttentionInsightRowResult {

    data class Parsed(
        val projection: AttentionInsightProjection
    ) : AttentionInsightRowResult

    data class Rejected(
        val reason: AttentionInsightUnavailableReason,
        val observedProtocolVersion: Int? = null
    ) : AttentionInsightRowResult
}

/**
 * Chooses the most useful result across production and QA authorities.
 *
 * A connected projection always wins (production first when both answer). If
 * none connect, a specific denial/malformed/version reason outranks a merely
 * absent or null provider, so falling back from production to QA can never
 * hide the useful primary diagnostic.
 */
internal fun mostInformativeAttentionInsightResult(
    results: List<AttentionInsightQueryResult>
): AttentionInsightQueryResult {
    results.firstOrNull { it is AttentionInsightQueryResult.Connected }?.let { return it }

    var best: AttentionInsightQueryResult.Unavailable? = null
    for (candidate in results.filterIsInstance<AttentionInsightQueryResult.Unavailable>()) {
        val current = best
        if (current == null || candidate.reason.diagnosticPriority() > current.reason.diagnosticPriority()) {
            best = candidate
        }
    }
    return best ?: AttentionInsightQueryResult.Unavailable(
        reason = AttentionInsightUnavailableReason.UNKNOWN
    )
}

private fun AttentionInsightUnavailableReason.diagnosticPriority(): Int = when (this) {
    AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED -> 90
    AttentionInsightUnavailableReason.PROTOCOL_VERSION_MISMATCH -> 80
    AttentionInsightUnavailableReason.MALFORMED_ROW -> 70
    AttentionInsightUnavailableReason.UNEXPECTED_ATTENTION_DAY -> 60
    AttentionInsightUnavailableReason.TRANSPORT_ERROR -> 50
    AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE -> 45
    AttentionInsightUnavailableReason.EMPTY_CURRENT_DAY -> 40
    AttentionInsightUnavailableReason.NULL_CURSOR -> 30
    AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED -> 20
    AttentionInsightUnavailableReason.UNKNOWN -> 10
}
