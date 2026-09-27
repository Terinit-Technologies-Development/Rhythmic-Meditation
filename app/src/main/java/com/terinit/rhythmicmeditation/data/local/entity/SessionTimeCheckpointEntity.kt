package com.terinit.rhythmicmeditation.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A persisted "safe point" of qualified meditation time, written periodically
 * while a session is ACTIVE.
 *
 * After process death or reboot, only time captured here (plus nothing beyond
 * it) can be trusted: the un-checkpointed open tail is discarded. Undercounting
 * a few seconds is acceptable; inventing time is not.
 *
 * [atElapsedRealtimeMs] is also the monotonic baseline used to detect device
 * reboots (elapsedRealtime resets to a small value after reboot).
 */
@Entity(tableName = "session_time_checkpoints")
data class SessionTimeCheckpointEntity(
    @PrimaryKey val sessionId: String,
    val qualifiedSecondsSnapshot: Int,
    val atElapsedRealtimeMs: Long,
    val atWallClockMs: Long,
    /** Settings.Global.BOOT_COUNT when available; secondary reboot guard. */
    val bootCount: Int?
)
