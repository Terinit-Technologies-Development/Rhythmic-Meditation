package com.terinit.rhythmicmeditation.domain.insights

import com.terinit.rhythmicmeditation.domain.evening.EveningMeditation
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MorningSessionPolicy
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

/**
 * Local-first, truthful, descriptive meditation insights.
 *
 * Aggregates the session ledger — and nothing else — into calm summaries.
 * No scores, no XP, no streaks, no judgment: every number here is a record of
 * what happened, never an evaluation of the user.
 *
 * COMPLETED-vs-PARTIAL RULE (the one rule to remember):
 * - General meditation MINUTES include genuinely qualified time from ANY
 *   non-INVALID session — COMPLETED, CANCELLED, EXPIRED, and incomplete
 *   ACTIVE/PAUSED/PENDING. The qualified seconds recorded in the ledger are
 *   real practice time whatever the session's fate.
 * - COMPLETED-session COUNTS use `status == COMPLETED` ONLY.
 * - A resumed session is one session record and is counted once.
 * - A session crossing midnight is assigned to the calendar day of its
 *   completion (or its last known activity when unfinished) and its minutes
 *   are never split across days and never double-counted. Choosing the
 *   completion day keeps one session in exactly one bucket.
 *
 * All functions are pure Kotlin (java.time for date math only) and take their
 * date keys explicitly so tests are deterministic.
 */
object MeditationInsights {

    /** Date-key format shared with the local attention day ids (`yyyy-MM-dd`). */
    private val DATE_KEY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Minimum sample per group before any behavioral observation is shown. */
    const val MIN_CORRELATION_SAMPLE_DAYS = 3

    /** Routine-owned budget of meditation substitution choices per Attention Day. */
    const val MEDITATION_SUBSTITUTION_CHOICES_TOTAL = 2

    // ------------------------------------------------------------------
    // Date-key mapping
    // ------------------------------------------------------------------

    /** `yyyy-MM-dd` date key for an epoch-millis wall-clock timestamp. */
    fun dateKeyOf(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DATE_KEY_FORMAT.format(dateOf(epochMs, zone))

    private fun dateOf(epochMs: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()

    /** Epoch-day value of a `yyyy-MM-dd` date key (inverse of [dateKeyOf]). */
    fun epochDayOf(dateKey: String): Long = LocalDate.parse(dateKey, DATE_KEY_FORMAT).toEpochDay()

    /** Date key for an epoch-day value. */
    fun dateKeyOfEpochDay(epochDay: Long): String =
        DATE_KEY_FORMAT.format(LocalDate.ofEpochDay(epochDay))

    /** The seven `Mon..Sun` date keys of the week containing [todayDateKey]. */
    fun weekDateKeys(todayDateKey: String): List<String> {
        val monday = LocalDate.parse(todayDateKey, DATE_KEY_FORMAT)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (0L until 7L).map { DATE_KEY_FORMAT.format(monday.plusDays(it)) }
    }

    // ------------------------------------------------------------------
    // Which day does a session's time belong to?
    // ------------------------------------------------------------------

    /**
     * The calendar day a session's qualified time is assigned to: the day of
     * completion for completed sessions, otherwise the day of the last known
     * activity (start, then creation). One session always lands in exactly one
     * day bucket — never split across midnight, never double-counted.
     */
    fun activityDateKeyOf(session: MeditationSession, zone: ZoneId = ZoneId.systemDefault()): String =
        dateKeyOf(
            session.completedAtEpochMs ?: session.startedAtEpochMs ?: session.createdAtEpochMs,
            zone
        )

    /**
     * Genuinely qualified time counts toward minutes regardless of how the
     * session ended (see the COMPLETED-vs-PARTIAL rule). INVALID rows are
     * untrusted evidence and count nowhere.
     */
    fun isCountedForMinutes(session: MeditationSession): Boolean =
        session.status != MeditationSessionStatus.INVALID

    // ------------------------------------------------------------------
    // Day summaries
    // ------------------------------------------------------------------

    /** Qualified meditation seconds recorded on [dateKey] (non-INVALID sessions). */
    fun qualifiedSecondsForDate(
        sessions: List<MeditationSession>,
        dateKey: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = sessions
        .filter { isCountedForMinutes(it) && activityDateKeyOf(it, zone) == dateKey }
        .sumOf { it.completedQualifiedSeconds }

    /**
     * Meditation minutes recorded on [dateKey]. Whole minutes, floored —
     * undercounting seconds is acceptable; inventing minutes is not.
     */
    fun meditationMinutesForDate(
        sessions: List<MeditationSession>,
        dateKey: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = qualifiedSecondsForDate(sessions, dateKey, zone) / 60

    /** Morning requirement state for [dateKey] (same rule the Today screen uses). */
    fun morningStatus(sessions: List<MeditationSession>, dateKey: String): MorningStatus =
        MorningSessionPolicy.resolve(sessions, dateKey)

    /**
     * Evening status for the Attention Day. A snooze whose deadline passed is
     * DUE again; a missing record is NOT_DUE (never fabricated).
     */
    fun eveningStatus(record: EveningMeditationRecord?, nowEpochMs: Long): EveningMeditationState =
        EveningMeditation.effectiveState(record, nowEpochMs)

    /** STANDALONE practice sessions recorded on [dateKey] (non-INVALID rows). */
    fun standaloneSessionCount(
        sessions: List<MeditationSession>,
        dateKey: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = sessions.count {
        it.kind == MeditationSessionKind.STANDALONE &&
            isCountedForMinutes(it) &&
            activityDateKeyOf(it, zone) == dateKey
    }

    /** Cooldown-restorative sessions recorded on [dateKey] (non-INVALID rows). */
    fun cooldownRestorativeCount(
        sessions: List<MeditationSession>,
        dateKey: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = sessions.count {
        it.kind == MeditationSessionKind.COOLDOWN_RESTORATIVE &&
            isCountedForMinutes(it) &&
            activityDateKeyOf(it, zone) == dateKey
    }

    /** COMPLETED sessions on [dateKey] — counts use COMPLETED ONLY. */
    fun completedSessionCountForDate(
        sessions: List<MeditationSession>,
        dateKey: String,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = sessions.count {
        it.status == MeditationSessionStatus.COMPLETED && activityDateKeyOf(it, zone) == dateKey
    }

    // ------------------------------------------------------------------
    // Weekly summaries (7 calendar-day buckets, Mon..Sun)
    // ------------------------------------------------------------------

    /** Meditation minutes per date key, aligned to [weekDateKeys] (Mon..Sun). */
    fun weeklyMinutes(
        sessions: List<MeditationSession>,
        weekDateKeys: List<String>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<Int> = weekDateKeys.map { meditationMinutesForDate(sessions, it, zone) }

    /** COMPLETED sessions inside the given week (counts use COMPLETED ONLY). */
    fun weeklyCompletedSessionCount(
        sessions: List<MeditationSession>,
        weekDateKeys: List<String>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = sessions.count {
        it.status == MeditationSessionStatus.COMPLETED &&
            activityDateKeyOf(it, zone) in weekDateKeys
    }

    /** Days in the week with at least one COMPLETED meditation ("N of 7 days"). */
    fun daysWithCompletedMeditation(
        sessions: List<MeditationSession>,
        weekDateKeys: List<String>,
        zone: ZoneId = ZoneId.systemDefault()
    ): Int = weekDateKeys.count { dateKey ->
        sessions.any {
            it.status == MeditationSessionStatus.COMPLETED && activityDateKeyOf(it, zone) == dateKey
        }
    }

    /**
     * Sample counts for the behavioral observation: days with a completed
     * morning meditation vs days with recorded practice but no completed
     * morning (the comparison group).
     */
    fun morningComparisonSample(
        sessions: List<MeditationSession>,
        weekDateKeys: List<String>,
        zone: ZoneId = ZoneId.systemDefault()
    ): CorrelationSample {
        var morningDays = 0
        var comparisonDays = 0
        weekDateKeys.forEach { dateKey ->
            val morningComplete = sessions.any {
                it.kind == MeditationSessionKind.MORNING_REQUIRED &&
                    it.status == MeditationSessionStatus.COMPLETED &&
                    activityDateKeyOf(it, zone) == dateKey
            }
            val anyPractice = sessions.any {
                isCountedForMinutes(it) &&
                    it.completedQualifiedSeconds > 0 &&
                    activityDateKeyOf(it, zone) == dateKey
            }
            when {
                morningComplete -> morningDays++
                anyPractice -> comparisonDays++
            }
        }
        return CorrelationSample(morningDays = morningDays, comparisonDays = comparisonDays)
    }

    /**
     * A descriptive behavioral observation — shown ONLY when both groups reach
     * the conservative [MIN_CORRELATION_SAMPLE_DAYS] threshold, so a tiny
     * sample never turns into a claim. The wording is observational, never
     * causal ("because", "improves") and never judgmental ("failed", "bad",
     * "wasted", scores, streak punishments).
     */
    fun morningCorrelationObservation(morningDays: Int, comparisonDays: Int): String? {
        if (morningDays < MIN_CORRELATION_SAMPLE_DAYS ||
            comparisonDays < MIN_CORRELATION_SAMPLE_DAYS
        ) {
            return null
        }
        return "On days when you completed Morning Meditation, your total meditation " +
            "minutes tended to be higher."
    }

    // ------------------------------------------------------------------
    // Routine projection (read-only display)
    // ------------------------------------------------------------------

    /**
     * "Meditation restorative choices · X of 2 remaining" — derived ONLY from
     * Routine's read-only projection of substitutions used, never inferred
     * from local session history, and clamped to the Routine-owned budget.
     */
    fun substitutionChoicesRemaining(meditationSubstitutionsUsed: Int): Int =
        (MEDITATION_SUBSTITUTION_CHOICES_TOTAL - meditationSubstitutionsUsed).coerceAtLeast(0)
}

/** Sample sizes behind a behavioral observation. */
data class CorrelationSample(
    val morningDays: Int,
    val comparisonDays: Int
)
