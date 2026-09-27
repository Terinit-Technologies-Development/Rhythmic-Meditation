package com.terinit.rhythmicmeditation.domain.model

/**
 * Optional framing for a meditation session.
 *
 * Modes are presentation only: every mode qualifies time with exactly the same
 * monotonic rules. There is no mode-specific scoring, no content catalogue,
 * and no guided-session library.
 */
enum class MeditationMode {
    STILLNESS,
    BREATH,
    BODY,
    IMAGINATION;

    companion object {
        fun fromWire(value: String?): MeditationMode? =
            entries.firstOrNull { it.name == value }
    }
}
