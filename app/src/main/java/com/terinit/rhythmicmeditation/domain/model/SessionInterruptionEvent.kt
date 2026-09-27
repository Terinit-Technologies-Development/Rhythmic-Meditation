package com.terinit.rhythmicmeditation.domain.model

/**
 * Why a session was interrupted.
 *
 * Kept intentionally broad so later passes can reason about calls, essential
 * access pauses, manual exits, screen-off, and process restore without schema
 * changes.
 */
enum class InterruptionType {
    APP_BACKGROUND,
    SCREEN_OFF,
    PHONE_CALL,
    ESSENTIAL_ACCESS,
    MANUAL_PAUSE,
    PROCESS_RESTORE,
    UNKNOWN;

    companion object {
        fun fromWire(value: String?): InterruptionType? =
            entries.firstOrNull { it.name == value }
    }
}

/**
 * An interruption observed during a session. Pure evidence — no policy.
 */
data class SessionInterruptionEvent(
    val id: Long = 0,
    val sessionId: String,
    val type: InterruptionType,
    val occurredAtEpochMs: Long,
    val note: String?
)
