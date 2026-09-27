package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.integration.contract.ReadingEvidence
import com.terinit.rhythmicmeditation.integration.contract.ReadingInsightClient

/**
 * In-memory [ReadingInsightClient] for deterministic unit tests. Set
 * [evidence] per date key; an absent key reads as "not connected".
 */
class FakeReadingInsightClient(
    var evidence: Map<String, ReadingEvidence> = emptyMap()
) : ReadingInsightClient {

    override fun dailyEvidence(dateKey: String): ReadingEvidence? = evidence[dateKey]
}
