package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.data.repository.SessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [SessionInterruptionEventRepository] for deterministic unit tests.
 */
class FakeSessionInterruptionEventRepository : SessionInterruptionEventRepository {

    private val events = MutableStateFlow<List<SessionInterruptionEvent>>(emptyList())

    override suspend fun recordEvent(event: SessionInterruptionEvent) {
        events.value = events.value + event
    }

    override suspend fun getEventsForSession(sessionId: String): List<SessionInterruptionEvent> =
        events.value.filter { it.sessionId == sessionId }

    override fun observeEventsForSession(sessionId: String): Flow<List<SessionInterruptionEvent>> =
        events.map { list -> list.filter { it.sessionId == sessionId } }
}
