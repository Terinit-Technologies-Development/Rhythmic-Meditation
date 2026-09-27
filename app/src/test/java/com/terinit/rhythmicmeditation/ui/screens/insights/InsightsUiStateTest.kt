package com.terinit.rhythmicmeditation.ui.screens.insights

import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProtocol
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProjection
import com.terinit.rhythmicmeditation.integration.contract.ReadingEvidence
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
 * Partial rendering: a missing Routine or Reader leaves honest gaps ("not
 * connected") and never fabricates cooldowns, gates, substitutions, or
 * reading. Local data stays truthful regardless of what crosses the boundary.
 */
class InsightsUiStateTest {

    private val zone: ZoneId = ZoneOffset.UTC

    // Week under test: Mon 2026-09-21 .. Sun 2026-09-27.
    private val monday = "2026-09-21"
    private val sunday = "2026-09-27"
    private val weekDateKeys = MeditationInsights.weekDateKeys(sunday)

    private fun epochMs(dateKey: String, hour: Int, minute: Int = 0): Long =
        LocalDate.parse(dateKey).atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun session(
        id: String,
        kind: MeditationSessionKind = MeditationSessionKind.STANDALONE,
        status: MeditationSessionStatus = MeditationSessionStatus.COMPLETED,
        qualifiedSeconds: Int = 1_800,
        completedAtDateKey: String = sunday,
        completedAtHour: Int = 7
    ) = MeditationSession(
        sessionId = id,
        protocolVersion = 1,
        kind = kind,
        status = status,
        requiredSeconds = 1_800,
        completedQualifiedSeconds = qualifiedSeconds,
        startedAtEpochMs = epochMs(completedAtDateKey, completedAtHour),
        completedAtEpochMs = epochMs(completedAtDateKey, completedAtHour, qualifiedSeconds / 60),
        createdAtEpochMs = epochMs(completedAtDateKey, completedAtHour),
        expiresAtEpochMs = null,
        sourceCooldownId = null,
        sourceRhythmicDayId = null,
        interruptionCount = 0,
        pauseCount = 0
    )

    private fun routineProjection(
        attentionDayId: String = sunday,
        gatesCreated: Int = 2,
        gatesSatisfied: Int = 1,
        substitutionsUsed: Int = 0
    ) = AttentionInsightProjection(
        protocolVersion = 1,
        attentionDayId = attentionDayId,
        cooldownsTriggered = 2,
        restorativeGatesCreated = gatesCreated,
        restorativeGatesSatisfied = gatesSatisfied,
        readerRestorativeCompletions = 0,
        meditationRestorativeCompletions = 1,
        meditationSubstitutionsUsed = substitutionsUsed,
        meditationSubstitutionsRemaining = MeditationInsights.substitutionChoicesRemaining(substitutionsUsed)
    )

    private fun build(
        sessions: List<MeditationSession> = emptyList(),
        routineToday: AttentionInsightProjection? = null,
        routineWeek: List<AttentionInsightProjection> = emptyList(),
        readingToday: ReadingEvidence? = null
    ) = buildInsightsUiState(
        sessions = sessions,
        eveningRecord = null,
        todayDateKey = sunday,
        weekDateKeys = weekDateKeys,
        nowEpochMs = epochMs(sunday, 21),
        routineToday = routineToday,
        routineWeek = routineWeek,
        readingToday = readingToday,
        zone = zone
    )

    @Test
    fun `missing routine projection renders partial data`() {
        val state = build(
            sessions = listOf(session("solo-1")),
            readingToday = ReadingEvidence(sunday, 5_400, 42)
        )

        assertFalse(state.routineConnected)
        assertEquals(0, state.gatesCreatedToday)
        assertEquals(0, state.gatesSatisfiedToday)
        assertEquals(0, state.gatesSatisfiedWeek)
        assertEquals(0, state.substitutionChoicesRemaining)
        // Local data is unaffected by the missing Routine.
        assertEquals(30, state.todayMeditationMinutes)
    }

    @Test
    fun `missing reader projection renders partial data`() {
        val state = build(
            sessions = listOf(session("solo-1")),
            routineToday = routineProjection()
        )

        assertFalse(state.readerConnected)
        assertEquals(0, state.readingVerifiedSecondsToday)
        assertEquals(0, state.readingQualifiedPagesToday)
        assertEquals(0, state.readingMinutes)
        // Meditation stays real and unshared — no fabricated reading.
        assertEquals(30, state.todayMeditationMinutes)
        assertEquals(30, state.totalMinutes)
    }

    @Test
    fun `substitution display comes from the routine projection only`() {
        val localRestorativeSessions = listOf(
            session(id = "restorative-1", kind = MeditationSessionKind.COOLDOWN_RESTORATIVE),
            session(id = "restorative-2", kind = MeditationSessionKind.COOLDOWN_RESTORATIVE),
            session(id = "standalone-1", kind = MeditationSessionKind.STANDALONE)
        )

        val withLocalHistory = build(
            sessions = localRestorativeSessions,
            routineToday = routineProjection(substitutionsUsed = 0)
        )
        val withoutLocalHistory = build(routineToday = routineProjection(substitutionsUsed = 0))
        val usedUp = build(
            sessions = localRestorativeSessions,
            routineToday = routineProjection(substitutionsUsed = 2)
        )

        assertEquals(2, withLocalHistory.substitutionChoicesRemaining)
        assertEquals(
            "local session history never changes the count",
            withoutLocalHistory.substitutionChoicesRemaining,
            withLocalHistory.substitutionChoicesRemaining
        )
        assertEquals(0, usedUp.substitutionChoicesRemaining)
    }

    @Test
    fun `untrusted projection is ignored`() {
        val garbageRow = mapOf(
            AttentionInsightProtocol.COLUMN_PROTOCOL_VERSION to "99",
            AttentionInsightProtocol.COLUMN_ATTENTION_DAY_ID to sunday,
            AttentionInsightProtocol.COLUMN_MEDITATION_SUBSTITUTIONS_USED to "0"
        )
        val parsed = AttentionInsightProtocol.parseRow(garbageRow)

        assertNull("unknown protocol never half-trusts", parsed)

        val state = build(routineToday = parsed)

        assertFalse(state.routineConnected)
        assertEquals(0, state.substitutionChoicesRemaining)
        assertEquals(0, state.gatesSatisfiedToday)
    }

    @Test
    fun `weekly numbers come from real sessions`() {
        val sessions = listOf(
            session(id = "a", completedAtDateKey = monday, qualifiedSeconds = 600),
            session(id = "b", completedAtDateKey = sunday, qualifiedSeconds = 1_800),
            session(
                id = "c",
                status = MeditationSessionStatus.CANCELLED,
                completedAtDateKey = sunday,
                qualifiedSeconds = 600
            )
        )

        val state = build(sessions = sessions)

        assertEquals(listOf(10, 0, 0, 0, 0, 0, 30 + 10), state.weeklyMinutes)
        assertEquals("only COMPLETED counts", 2, state.weeklyCompletedSessionCount)
        assertEquals(2, state.daysWithCompletedMeditation)
        assertEquals(
            "morning status is real",
            MorningStatus.REQUIRED,
            state.morningStatus
        )
    }

    @Test
    fun `observation is hidden below the sample threshold`() {
        val sessions = mutableListOf<MeditationSession>()
        // 2 morning days (below the conservative threshold).
        listOf(monday, "2026-09-22").forEach { day ->
            sessions += session(
                id = "morning-$day",
                kind = MeditationSessionKind.MORNING_REQUIRED,
                completedAtDateKey = day
            )
        }
        listOf("2026-09-23", "2026-09-24").forEach { day ->
            sessions += session(id = "standalone-$day", completedAtDateKey = day)
        }

        val state = build(sessions = sessions)

        assertNull(state.observation)
    }

    @Test
    fun `observation is shown at the sample threshold`() {
        val sessions = mutableListOf<MeditationSession>()
        listOf(monday, "2026-09-22", "2026-09-23").forEach { day ->
            sessions += session(
                id = "morning-$day",
                kind = MeditationSessionKind.MORNING_REQUIRED,
                completedAtDateKey = day
            )
        }
        listOf("2026-09-24", "2026-09-25", "2026-09-26").forEach { day ->
            sessions += session(id = "standalone-$day", completedAtDateKey = day)
        }

        val state = build(sessions = sessions)

        assertNotNull(state.observation)
        assertTrue(state.observation!!.contains("tended to"))
    }

    @Test
    fun `routine projection numbers are rendered as projected`() {
        val state = build(
            routineToday = routineProjection(gatesCreated = 3, gatesSatisfied = 2),
            routineWeek = listOf(
                routineProjection(attentionDayId = monday, gatesSatisfied = 2),
                routineProjection(attentionDayId = sunday, gatesSatisfied = 2)
            ),
            readingToday = ReadingEvidence(sunday, 5_400, 42)
        )

        assertTrue(state.routineConnected)
        assertEquals(3, state.gatesCreatedToday)
        assertEquals(2, state.gatesSatisfiedToday)
        assertEquals(4, state.gatesSatisfiedWeek)
        assertTrue(state.readerConnected)
        assertEquals(90, state.readingMinutes)
    }
}
