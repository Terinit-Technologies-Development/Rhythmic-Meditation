package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.SessionInterruptionEventDao
import com.terinit.rhythmicmeditation.data.mapper.toDomain
import com.terinit.rhythmicmeditation.data.mapper.toEntity
import com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [SessionInterruptionEventRepository].
 */
class RoomSessionInterruptionEventRepository(
    private val dao: SessionInterruptionEventDao
) : SessionInterruptionEventRepository {

    override suspend fun recordEvent(event: SessionInterruptionEvent) {
        dao.insert(event.toEntity())
    }

    override suspend fun getEventsForSession(sessionId: String): List<SessionInterruptionEvent> =
        dao.getForSession(sessionId).map { it.toDomain() }

    override fun observeEventsForSession(sessionId: String): Flow<List<SessionInterruptionEvent>> =
        dao.observeForSession(sessionId).map { list -> list.map { it.toDomain() } }
}
