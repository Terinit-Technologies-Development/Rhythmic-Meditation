package com.terinit.rhythmicmeditation.data.repository

import com.terinit.rhythmicmeditation.domain.model.MeditationInterval

/**
 * Interval ledger used to reconstruct qualified time after interruption,
 * restart, or process death. Times are monotonic (elapsedRealtime) with wall
 * clock values recorded for diagnostics only.
 */
interface MeditationIntervalRepository {

    /**
     * Opens an interval and returns its row id.
     */
    suspend fun startInterval(
        sessionId: String,
        startedElapsedMs: Long,
        startedWallClockMs: Long
    ): Long

    /**
     * Closes any open interval for [sessionId]. Safe to call when nothing is
     * open (no-op).
     */
    suspend fun closeOpenInterval(
        sessionId: String,
        endedElapsedMs: Long,
        endedWallClockMs: Long
    )

    suspend fun getIntervalsForSession(sessionId: String): List<MeditationInterval>

    /** The currently open interval for [sessionId], if any. */
    suspend fun getOpenInterval(sessionId: String): MeditationInterval?
}
