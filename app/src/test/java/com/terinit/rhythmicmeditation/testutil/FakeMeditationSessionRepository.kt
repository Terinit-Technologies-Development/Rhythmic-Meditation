package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory [MeditationSessionRepository] for deterministic unit tests.
 */
class FakeMeditationSessionRepository : MeditationSessionRepository {

    private val sessions = MutableStateFlow<Map<String, MeditationSession>>(emptyMap())

    /** Number of writes performed (for idempotency assertions). */
    var upsertCount: Int = 0
        private set

    override fun observeActiveSession(): Flow<MeditationSession?> =
        sessions.map { map ->
            map.values
                .filter {
                    it.status == MeditationSessionStatus.PENDING ||
                        it.status == MeditationSessionStatus.ACTIVE ||
                        it.status == MeditationSessionStatus.PAUSED
                }
                .maxByOrNull { it.createdAtEpochMs }
        }

    override fun observeSessions(): Flow<List<MeditationSession>> =
        sessions.map { map -> map.values.sortedByDescending { it.createdAtEpochMs } }

    override fun observeSession(sessionId: String): Flow<MeditationSession?> =
        sessions.map { map -> map[sessionId] }

    override suspend fun getSession(sessionId: String): MeditationSession? =
        sessions.value[sessionId]

    override suspend fun upsertSession(session: MeditationSession) {
        upsertCount++
        sessions.value = sessions.value + (session.sessionId to session)
    }

    override suspend fun updateStatus(sessionId: String, status: MeditationSessionStatus) {
        val existing = sessions.value[sessionId] ?: return
        sessions.value = sessions.value + (sessionId to existing.copy(status = status))
    }

    override suspend fun addQualifiedSeconds(sessionId: String, additionalSeconds: Int) {
        val existing = sessions.value[sessionId] ?: return
        sessions.value = sessions.value + (
            sessionId to existing.copy(
                completedQualifiedSeconds =
                    existing.completedQualifiedSeconds + additionalSeconds.coerceAtLeast(0)
            )
            )
    }

    override suspend fun incrementInterruptionCount(sessionId: String) {
        val existing = sessions.value[sessionId] ?: return
        sessions.value = sessions.value +
            (sessionId to existing.copy(interruptionCount = existing.interruptionCount + 1))
    }

    override suspend fun incrementPauseCount(sessionId: String) {
        val existing = sessions.value[sessionId] ?: return
        sessions.value = sessions.value +
            (sessionId to existing.copy(pauseCount = existing.pauseCount + 1))
    }
}
