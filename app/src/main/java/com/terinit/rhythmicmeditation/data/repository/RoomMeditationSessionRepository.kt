package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.data.local.dao.MeditationSessionDao
import com.terinit.rhythmicmeditation.data.mapper.toDomain
import com.terinit.rhythmicmeditation.data.mapper.toEntity
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Room-backed [MeditationSessionRepository].
 */
class RoomMeditationSessionRepository(
    private val dao: MeditationSessionDao
) : MeditationSessionRepository {

    override fun observeActiveSession(): Flow<MeditationSession?> =
        dao.observeCurrentSession().map { it?.toDomain() }

    override fun observeSessions(): Flow<List<MeditationSession>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeSession(sessionId: String): Flow<MeditationSession?> =
        dao.observeById(sessionId).map { it?.toDomain() }

    override suspend fun getSession(sessionId: String): MeditationSession? =
        dao.getById(sessionId)?.toDomain()

    override suspend fun getSessionsWithIdPrefix(prefix: String): List<MeditationSession> =
        dao.getByIdPrefix(prefix).map { it.toDomain() }

    override suspend fun upsertSession(session: MeditationSession) {
        dao.upsert(session.toEntity())
    }

    override suspend fun updateStatus(sessionId: String, status: MeditationSessionStatus) {
        dao.updateStatus(sessionId, status.name)
    }

    override suspend fun addQualifiedSeconds(sessionId: String, additionalSeconds: Int) {
        dao.addQualifiedSeconds(sessionId, additionalSeconds.coerceAtLeast(0))
    }

    override suspend fun incrementInterruptionCount(sessionId: String) {
        dao.incrementInterruptionCount(sessionId)
    }

    override suspend fun incrementPauseCount(sessionId: String) {
        dao.incrementPauseCount(sessionId)
    }
}
