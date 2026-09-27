package com.terinit.rhythmicmeditation.domain.insights

import com.terinit.rhythmicmeditation.domain.evening.EveningMeditation
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Truthful, descriptive aggregation.
 *
 * The COMPLETED-vs-PARTIAL rule under test: minutes include genuinely
 * qualified time from any non-INVALID session (COMPLETED, CANCELLED, EXPIRED,
 * incomplete ACTIVE/PAUSED), while completed counts use COMPLETED only. No
 * scores, no streaks, no judgment — and never double-counting.
 */
class MeditationInsightsTest {

    private val zone: ZoneId = ZoneOffset.UTC

    // Week under test: Mon 2026-09-21 .. Sun 2026-09-27.
    private val monday = "2026-09-21"
    private val wednesday = "2026-09-23"
    private val sunday = "2026-09-27"

    private fun epochMs(dateKey: String, hour: Int, minute: Int = 0): Long =
        LocalDate.parse(dateKey).atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun session(
        id: String = "session-1",
        kind: MeditationSessionKind = MeditationSessionKind.STANDALONE,
        status: MeditationSessionStatus = MeditationSessionStatus.COMPLETED,
        qualifiedSeconds: Int = 0,
        startedAtEpochMs: Long? = null,
        completedAtEpochMs: Long? = null,
        createdAtEpochMs: Long = epochMs(monday, 8),
        sourceRhythmicDayId: String? = null,
        pauseCount: Int = 0
    ) = MeditationSession(
        sessionId = id,
        protocolVersion = 1,
        kind = kind,
        status = status,
        requiredSeconds = 1_800,
        completedQualifiedSeconds = qualifiedSeconds,
        startedAtEpochMs = startedAtEpochMs,
        completedAtEpochMs = completedAtEpochMs,
        createdAtEpochMs = createdAtEpochMs,
        expiresAtEpochMs = null,
        sourceCooldownId = null,
        sourceRhythmicDayId = sourceRhythmicDayId,
        interruptionCount = 0,
        pauseCount = pauseCount
    )

    // ------------------------------------------------------------------
    // The COMPLETED-vs-PARTIAL rule
    // ------------------------------------------------------------------

    @Test
    fun `today minutes include completed qualified time`() {
        val sessions = listOf(
            session(
                qualifiedSeconds = 1_800,
                startedAtEpochMs = epochMs(sunday, 7),
                completedAtEpochMs = epochMs(sunday, 7, 30)
            )
        )

        assertEquals(30, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
    }

    @Test
    fun `cancelled session qualified minutes count toward minutes but not completed counts`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.CANCELLED,
                qualifiedSeconds = 600,
                startedAtEpochMs = epochMs(sunday, 21),
                completedAtEpochMs = epochMs(sunday, 21, 10)
            )
        )

        assertEquals("real practice time is real", 10, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals(0, MeditationInsights.completedSessionCountForDate(sessions, sunday, zone))
        assertEquals(0, MeditationInsights.weeklyCompletedSessionCount(sessions, MeditationInsights.weekDateKeys(sunday), zone))
    }

    @Test
    fun `incomplete active session partial time counts toward minutes`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.ACTIVE,
                qualifiedSeconds = 700,
                startedAtEpochMs = epochMs(sunday, 22)
            )
        )

        assertEquals(11, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals(0, MeditationInsights.completedSessionCountForDate(sessions, sunday, zone))
    }

    @Test
    fun `paused session partial time counts toward minutes`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.PAUSED,
                qualifiedSeconds = 60,
                startedAtEpochMs = epochMs(sunday, 6)
            )
        )

        assertEquals(1, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
    }

    @Test
    fun `expired session qualified time counts toward minutes`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.EXPIRED,
                qualifiedSeconds = 300,
                startedAtEpochMs = epochMs(sunday, 9)
            )
        )

        assertEquals(5, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
    }

    @Test
    fun `invalid sessions count nowhere`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.INVALID,
                qualifiedSeconds = 1_800,
                startedAtEpochMs = epochMs(sunday, 7),
                completedAtEpochMs = epochMs(sunday, 7, 30)
            )
        )

        assertEquals(0, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals(0, MeditationInsights.completedSessionCountForDate(sessions, sunday, zone))
    }

    @Test
    fun `completed counts use completed only`() {
        val sessions = listOf(
            session(id = "a", status = MeditationSessionStatus.COMPLETED, qualifiedSeconds = 1_800, completedAtEpochMs = epochMs(sunday, 8)),
            session(id = "b", status = MeditationSessionStatus.COMPLETED, qualifiedSeconds = 1_800, completedAtEpochMs = epochMs(sunday, 9)),
            session(id = "c", status = MeditationSessionStatus.CANCELLED, qualifiedSeconds = 600, completedAtEpochMs = epochMs(sunday, 10)),
            session(id = "d", status = MeditationSessionStatus.ACTIVE, qualifiedSeconds = 600, startedAtEpochMs = epochMs(sunday, 11))
        )

        assertEquals(2, MeditationInsights.completedSessionCountForDate(sessions, sunday, zone))
        assertEquals(2, MeditationInsights.weeklyCompletedSessionCount(sessions, MeditationInsights.weekDateKeys(sunday), zone))
        assertEquals(
            "all qualified minutes still count",
            30 + 30 + 10 + 10,
            MeditationInsights.meditationMinutesForDate(sessions, sunday, zone)
        )
    }

    @Test
    fun `a resumed session is counted once`() {
        // One session record whose qualified seconds accrued across pause/resume.
        val sessions = listOf(
            session(
                qualifiedSeconds = 2_100,
                status = MeditationSessionStatus.COMPLETED,
                startedAtEpochMs = epochMs(sunday, 6),
                completedAtEpochMs = epochMs(sunday, 6, 35),
                pauseCount = 2
            )
        )

        assertEquals(35, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals(1, MeditationInsights.completedSessionCountForDate(sessions, sunday, zone))
        assertEquals(1, MeditationInsights.daysWithCompletedMeditation(sessions, MeditationInsights.weekDateKeys(sunday), zone))
    }

    // ------------------------------------------------------------------
    // Calendar-day assignment (midnight, attention day vs calendar day)
    // ------------------------------------------------------------------

    @Test
    fun `a midnight crossing session is counted on its completion day only`() {
        val sessions = listOf(
            session(
                qualifiedSeconds = 1_200,
                startedAtEpochMs = epochMs("2026-09-26", 23, 50),
                completedAtEpochMs = epochMs("2026-09-27", 0, 10)
            )
        )

        assertEquals(
            "all minutes land on the completion day",
            20,
            MeditationInsights.meditationMinutesForDate(sessions, "2026-09-27", zone)
        )
        assertEquals(
            "never split across days",
            0,
            MeditationInsights.meditationMinutesForDate(sessions, "2026-09-26", zone)
        )
        assertEquals(
            "never double-counted",
            listOf(0, 0, 0, 0, 0, 0, 20),
            MeditationInsights.weeklyMinutes(sessions, MeditationInsights.weekDateKeys(sunday), zone)
        )
    }

    @Test
    fun `unfinished session time is assigned to its last activity day`() {
        val sessions = listOf(
            session(
                status = MeditationSessionStatus.ACTIVE,
                qualifiedSeconds = 300,
                startedAtEpochMs = epochMs(sunday, 20),
                createdAtEpochMs = epochMs(sunday, 20)
            )
        )

        assertEquals(5, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals(0, MeditationInsights.meditationMinutesForDate(sessions, "2026-09-26", zone))
    }

    @Test
    fun `attention day ids never drive calendar bucketing`() {
        val sessions = listOf(
            session(
                qualifiedSeconds = 1_800,
                sourceRhythmicDayId = "routine-day-7",
                completedAtEpochMs = epochMs(wednesday, 8)
            )
        )

        assertEquals(30, MeditationInsights.meditationMinutesForDate(sessions, wednesday, zone))
        assertEquals(0, MeditationInsights.meditationMinutesForDate(sessions, sunday, zone))
        assertEquals("routine-day-7", sessions.single().sourceRhythmicDayId)
    }

    // ------------------------------------------------------------------
    // Weekly buckets (Mon..Sun with a date-key mapping)
    // ------------------------------------------------------------------

    @Test
    fun `week date keys run monday to sunday`() {
        val keys = MeditationInsights.weekDateKeys(sunday)

        assertEquals(7, keys.size)
        assertEquals(monday, keys.first())
        assertEquals(sunday, keys.last())
        assertEquals(
            "every day of the week maps to the same bucket set",
            keys,
            MeditationInsights.weekDateKeys(monday)
        )
    }

    @Test
    fun `weekly minutes fill seven monday-to-sunday buckets`() {
        val sessions = listOf(
            session(
                id = "mon",
                qualifiedSeconds = 600,
                completedAtEpochMs = epochMs(monday, 7)
            ),
            session(
                id = "sun",
                qualifiedSeconds = 1_800,
                completedAtEpochMs = epochMs(sunday, 7)
            )
        )

        assertEquals(
            listOf(10, 0, 0, 0, 0, 0, 30),
            MeditationInsights.weeklyMinutes(sessions, MeditationInsights.weekDateKeys(sunday), zone)
        )
    }

    @Test
    fun `days with completed meditation counts days not sessions`() {
        val sessions = listOf(
            session(id = "a", completedAtEpochMs = epochMs(monday, 7)),
            session(id = "b", completedAtEpochMs = epochMs(monday, 8)),
            session(id = "c", completedAtEpochMs = epochMs(wednesday, 7))
        )

        assertEquals(
            2,
            MeditationInsights.daysWithCompletedMeditation(
                sessions,
                MeditationInsights.weekDateKeys(sunday),
                zone
            )
        )
    }

    @Test
    fun `standalone and cooldown restorative counts`() {
        val sessions = listOf(
            session(id = "a", kind = MeditationSessionKind.STANDALONE, completedAtEpochMs = epochMs(sunday, 7)),
            session(id = "b", kind = MeditationSessionKind.STANDALONE, completedAtEpochMs = epochMs(sunday, 8)),
            session(id = "c", kind = MeditationSessionKind.COOLDOWN_RESTORATIVE, completedAtEpochMs = epochMs(sunday, 9)),
            session(id = "d", kind = MeditationSessionKind.MORNING_REQUIRED, completedAtEpochMs = epochMs(sunday, 6))
        )

        assertEquals(2, MeditationInsights.standaloneSessionCount(sessions, sunday, zone))
        assertEquals(1, MeditationInsights.cooldownRestorativeCount(sessions, sunday, zone))
    }

    // ------------------------------------------------------------------
    // Morning / evening status
    // ------------------------------------------------------------------

    @Test
    fun `morning status resolves required and complete from real sessions`() {
        assertEquals(
            MorningStatus.REQUIRED,
            MeditationInsights.morningStatus(emptyList(), sunday)
        )

        val completed = listOf(
            session(
                id = "morning-$sunday",
                kind = MeditationSessionKind.MORNING_REQUIRED,
                completedAtEpochMs = epochMs(sunday, 7)
            )
        )
        assertEquals(MorningStatus.COMPLETE, MeditationInsights.morningStatus(completed, sunday))
    }

    @Test
    fun `evening status uses the effective snooze state`() {
        val snoozed = EveningMeditationRecord(
            attentionDayId = sunday,
            state = EveningMeditationState.SNOOZED,
            dueAtEpochMs = epochMs(sunday, 20),
            snoozedUntilEpochMs = epochMs(sunday, 20, 15),
            sessionId = null,
            updatedAtEpochMs = epochMs(sunday, 20)
        )

        assertEquals(
            EveningMeditationState.SNOOZED,
            MeditationInsights.eveningStatus(snoozed, epochMs(sunday, 20, 10))
        )
        assertEquals(
            EveningMeditationState.DUE,
            MeditationInsights.eveningStatus(snoozed, epochMs(sunday, 20, 15))
        )
        assertEquals(
            EveningMeditationState.NOT_DUE,
            MeditationInsights.eveningStatus(null, epochMs(sunday, 20))
        )
    }

    // ------------------------------------------------------------------
    // Date-key mapping
    // ------------------------------------------------------------------

    @Test
    fun `date key mapping round trips`() {
        val key = "2026-09-27"
        assertEquals(key, MeditationInsights.dateKeyOfEpochDay(MeditationInsights.epochDayOf(key)))
        assertEquals(key, MeditationInsights.dateKeyOf(epochMs(key, 23, 59), zone))
        assertEquals("2026-09-28", MeditationInsights.dateKeyOf(epochMs("2026-09-28", 0, 0), zone))
    }

    // ------------------------------------------------------------------
    // Routine projection: substitution choices
    // ------------------------------------------------------------------

    @Test
    fun `substitution choices remaining follows the routine budget`() {
        assertEquals(2, MeditationInsights.substitutionChoicesRemaining(0))
        assertEquals(1, MeditationInsights.substitutionChoicesRemaining(1))
        assertEquals(0, MeditationInsights.substitutionChoicesRemaining(2))
        assertEquals("clamped at zero", 0, MeditationInsights.substitutionChoicesRemaining(5))
    }

    // ------------------------------------------------------------------
    // Behavioral observation (>= 3 / >= 3 or nothing)
    // ------------------------------------------------------------------

    @Test
    fun `correlation observation is hidden below the sample threshold`() {
        assertNull(MeditationInsights.morningCorrelationObservation(2, 3))
        assertNull(MeditationInsights.morningCorrelationObservation(3, 2))
        assertNull(MeditationInsights.morningCorrelationObservation(2, 2))
        assertNull(MeditationInsights.morningCorrelationObservation(0, 0))
    }

    @Test
    fun `correlation observation is shown at the sample threshold`() {
        assertNotNull(MeditationInsights.morningCorrelationObservation(3, 3))
        assertNotNull(MeditationInsights.morningCorrelationObservation(10, 5))
    }

    @Test
    fun `correlation wording is descriptive and never judgmental`() {
        val line = MeditationInsights.morningCorrelationObservation(3, 3)!!

        assertTrue(line.contains("tended to"))
        listOf("failed", "failure", "bad", "wasted", "score", "streak", "because", "improves").forEach {
            assertFalse("copy must not contain '$it'", line.contains(it, ignoreCase = true))
        }
    }

    @Test
    fun `comparison sample counts morning days and comparison days`() {
        val sessions = mutableListOf<MeditationSession>()
        // Three days with a completed morning.
        listOf(monday, wednesday, sunday).forEach { day ->
            sessions += session(
                id = "morning-$day",
                kind = MeditationSessionKind.MORNING_REQUIRED,
                completedAtEpochMs = epochMs(day, 7)
            )
        }
        // Two more days with practice but no completed morning.
        listOf("2026-09-22", "2026-09-24").forEach { day ->
            sessions += session(
                id = "standalone-$day",
                qualifiedSeconds = 1_800,
                completedAtEpochMs = epochMs(day, 19)
            )
        }

        val sample = MeditationInsights.morningComparisonSample(
            sessions,
            MeditationInsights.weekDateKeys(sunday),
            zone
        )

        assertEquals(3, sample.morningDays)
        assertEquals(2, sample.comparisonDays)
    }
}
