package com.terinit.rhythmicmeditation.domain.session

import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus

/** Presentation state of the local morning requirement. */
enum class MorningStatus {
    /** No morning session yet today (or only failed attempts) — begin one. */
    REQUIRED,
    /** A morning session is running. */
    IN_PROGRESS,
    /** A morning session is paused and awaiting Resume. */
    PAUSED,
    /** Today's morning requirement is complete. */
    COMPLETE
}

/**
 * Pure rules for the local morning meditation requirement (Pass 2 dogfood).
 *
 * Session ids are deterministic per day (`morning-<dayId>`, then `-2`, `-3` …
 * for restarts after cancellation) so opening the same id twice can never
 * create two morning sessions, while a cancelled attempt never transfers
 * progress into the next one.
 */
object MorningSessionPolicy {

    const val ID_PREFIX = "morning-"

    fun idPrefixForDay(rhythmicDayId: String): String = "$ID_PREFIX$rhythmicDayId"

    /** Base id for the first attempt of the day. */
    fun firstSessionId(rhythmicDayId: String): String = idPrefixForDay(rhythmicDayId)

    /**
     * Id for a new attempt given the day's existing sessions. A cancelled or
     * expired attempt yields a fresh id, so no progress is inherited.
     */
    fun nextSessionId(existingForDay: List<MeditationSession>, rhythmicDayId: String): String {
        val prefix = idPrefixForDay(rhythmicDayId)
        return if (existingForDay.isEmpty()) firstSessionId(rhythmicDayId)
        else "$prefix-${existingForDay.size}"
    }

    /** Resolves the morning requirement state from the day's sessions. */
    fun resolve(
        existingForDay: List<MeditationSession>,
        rhythmicDayId: String
    ): MorningStatus {
        val prefix = idPrefixForDay(rhythmicDayId)
        val forDay = existingForDay.filter { it.sessionId.startsWith(prefix) }
        return when {
            forDay.any { it.status == MeditationSessionStatus.COMPLETED } ->
                MorningStatus.COMPLETE
            forDay.any {
                it.status == MeditationSessionStatus.ACTIVE ||
                    it.status == MeditationSessionStatus.PAUSED ||
                    it.status == MeditationSessionStatus.PENDING
            } -> {
                val latest = forDay
                    .filter {
                        it.status == MeditationSessionStatus.ACTIVE ||
                            it.status == MeditationSessionStatus.PAUSED ||
                            it.status == MeditationSessionStatus.PENDING
                    }
                    .maxByOrNull { it.createdAtEpochMs }
                if (latest?.status == MeditationSessionStatus.PAUSED) {
                    MorningStatus.PAUSED
                } else {
                    MorningStatus.IN_PROGRESS
                }
            }
            else -> MorningStatus.REQUIRED
        }
    }
}
