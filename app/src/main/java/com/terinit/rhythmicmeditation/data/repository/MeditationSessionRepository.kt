package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Local ledger of meditation sessions. The primary session store lives in
 * Room, never in DataStore.
 */
interface MeditationSessionRepository {

    /** The most recent PENDING/ACTIVE/PAUSED session, if any. */
    fun observeActiveSession(): Flow<MeditationSession?>

    /** All sessions, newest first. */
    fun observeSessions(): Flow<List<MeditationSession>>

    fun observeSession(sessionId: String): Flow<MeditationSession?>

    suspend fun getSession(sessionId: String): MeditationSession?

    suspend fun upsertSession(session: MeditationSession)

    suspend fun updateStatus(sessionId: String, status: MeditationSessionStatus)

    /** Adds qualified meditation seconds (evidence only; never policy). */
    suspend fun addQualifiedSeconds(sessionId: String, additionalSeconds: Int)

    suspend fun incrementInterruptionCount(sessionId: String)

    suspend fun incrementPauseCount(sessionId: String)
}
