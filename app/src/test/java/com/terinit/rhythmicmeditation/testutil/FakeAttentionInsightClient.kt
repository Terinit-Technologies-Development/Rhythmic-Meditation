package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightClient
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProjection

/**
 * In-memory [AttentionInsightClient] for deterministic unit tests. Read-only
 * by construction: nothing in this app ever writes to it, which is exactly
 * the property "evening completion never consumes a substitution" relies on.
 */
class FakeAttentionInsightClient(
    var projections: Map<String, AttentionInsightProjection> = emptyMap()
) : AttentionInsightClient {

    /** Days queried (to prove insights reads are one-shot and day-scoped). */
    val queriedDays: MutableList<String> = mutableListOf()

    override fun attentionDay(attentionDayId: String): AttentionInsightProjection? {
        queriedDays += attentionDayId
        return projections[attentionDayId]
    }
}
