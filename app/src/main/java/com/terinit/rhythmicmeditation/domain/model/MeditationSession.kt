package com.terinit.rhythmicmeditation.domain.model

/**
 * A discrete meditation session.
 *
 * Covers the four session surfaces of the product:
 * - [MeditationSessionKind.MORNING_REQUIRED] — the required morning meditation
 * - [MeditationSessionKind.COOLDOWN_RESTORATIVE] — restorative meditation bound to a cooldown
 * - [MeditationSessionKind.EVENING_WIND_DOWN] — optional evening practice
 * - [MeditationSessionKind.STANDALONE] — free/unbound practice
 *
 * This model is pure Kotlin: it holds meditation evidence only. Policy
 * (cooldowns, requirements, enforcement) is owned by Rhythmic Routine.
 */
data class MeditationSession(
    val sessionId: String,
    val protocolVersion: Int,
    val kind: MeditationSessionKind,
    val status: MeditationSessionStatus,
    val requiredSeconds: Int,
    val completedQualifiedSeconds: Int,
    val startedAtEpochMs: Long?,
    val completedAtEpochMs: Long?,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long?,
    val sourceCooldownId: String?,
    val sourceRhythmicDayId: String?,
    val interruptionCount: Int,
    val pauseCount: Int
) {
    /** Progress in the range 0f..1f, or 0f when no time is required. */
    val progress: Float
        get() = when {
            requiredSeconds <= 0 -> 0f
            else -> (completedQualifiedSeconds.toFloat() / requiredSeconds)
                .coerceIn(0f, 1f)
        }

    /** True once enough qualified time has been accumulated. */
    val isRequirementMet: Boolean
        get() = requiredSeconds > 0 && completedQualifiedSeconds >= requiredSeconds

    /** True while the session can still accept qualified time. */
    val isActiveLifecycle: Boolean
        get() = status == MeditationSessionStatus.ACTIVE ||
            status == MeditationSessionStatus.PAUSED ||
            status == MeditationSessionStatus.PENDING
}

enum class MeditationSessionKind {
    MORNING_REQUIRED,
    COOLDOWN_RESTORATIVE,
    EVENING_WIND_DOWN,
    STANDALONE;

    companion object {
        /** Parses a wire value; returns null for unknown kinds. */
        fun fromWire(value: String?): MeditationSessionKind? =
            entries.firstOrNull { it.name == value }
    }
}

enum class MeditationSessionStatus {
    PENDING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED,
    EXPIRED,
    INVALID;

    companion object {
        /** Parses a wire value; returns null for unknown statuses. */
        fun fromWire(value: String?): MeditationSessionStatus? =
            entries.firstOrNull { it.name == value }
    }
}
