package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent
import kotlinx.coroutines.flow.Flow

/**
 * Ledger of session interruption events (calls, essential access, manual
 * pauses, screen-off, process restore). Evidence only.
 */
interface SessionInterruptionEventRepository {

    suspend fun recordEvent(event: SessionInterruptionEvent)

    suspend fun getEventsForSession(sessionId: String): List<SessionInterruptionEvent>

    fun observeEventsForSession(sessionId: String): Flow<List<SessionInterruptionEvent>>
}
