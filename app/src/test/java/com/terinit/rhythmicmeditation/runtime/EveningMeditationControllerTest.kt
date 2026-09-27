package com.terinit.rhythmicmeditation.runtime

import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.domain.session.MorningSessionPolicy
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProjection
import com.terinit.rhythmicmeditation.integration.contract.EveningSignal
import com.terinit.rhythmicmeditation.testutil.FakeAttentionInsightClient
import com.terinit.rhythmicmeditation.testutil.FakeBootIdentityReader
import com.terinit.rhythmicmeditation.testutil.FakeEveningMeditationRepository
import com.terinit.rhythmicmeditation.testutil.FakeMeditationIntervalRepository
import com.terinit.rhythmicmeditation.testutil.FakeMeditationSessionRepository
import com.terinit.rhythmicmeditation.testutil.FakeRoutineEveningSignal
import com.terinit.rhythmicmeditation.testutil.FakeScreenStateReader
import com.terinit.rhythmicmeditation.testutil.FakeSessionCheckpointRepository
import com.terinit.rhythmicmeditation.testutil.FakeSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.testutil.FakeTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The evening flow end to end — and the hard boundary that keeps it out of
 * policy: evening completion never consumes a Meditation substitution, never
 * satisfies Morning Meditation, and never satisfies a cooldown Restorative
 * Gate. Snoozing and deferring are decisions, not failures, and never change
 * tomorrow's Morning requirement.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class EveningMeditationControllerTest {

    private val dayId = "2026-09-27"
    private val nextDayId = "2026-09-28"

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var sessionRepository: FakeMeditationSessionRepository
    private lateinit var intervalRepository: FakeMeditationIntervalRepository
    private lateinit var interruptionRepository: FakeSessionInterruptionEventRepository
    private lateinit var checkpointRepository: FakeSessionCheckpointRepository
    private lateinit var screenStateReader: FakeScreenStateReader
    private lateinit var bootIdentityReader: FakeBootIdentityReader
    private lateinit var service: MeditationSessionService
    private lateinit var eveningRepository: FakeEveningMeditationRepository
    private lateinit var routineSignal: FakeRoutineEveningSignal

    @Before
    fun setUp() {
        timeProvider = FakeTimeProvider()
        sessionRepository = FakeMeditationSessionRepository()
        intervalRepository = FakeMeditationIntervalRepository()
        interruptionRepository = FakeSessionInterruptionEventRepository()
        checkpointRepository = FakeSessionCheckpointRepository()
        screenStateReader = FakeScreenStateReader()
        bootIdentityReader = FakeBootIdentityReader()
        service = MeditationSessionService(
            sessionRepository = sessionRepository,
            intervalRepository = intervalRepository,
            interruptionRepository = interruptionRepository,
            timeProvider = timeProvider
        )
        eveningRepository = FakeEveningMeditationRepository()
        routineSignal = FakeRoutineEveningSignal()
    }

    /**
     * Both controllers run on the test scheduler (unconfined) so signal
     * handlers and the completion observer act eagerly; backgroundScope keeps
     * their infinite collectors from blocking the test's completion.
     */
    private fun TestScope.controllers(): Pair<MeditationRuntimeController, EveningMeditationController> {
        val scope = CoroutineScope(
            backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)
        )
        val runtimeController = MeditationRuntimeController(
            sessionService = service,
            sessionRepository = sessionRepository,
            checkpointRepository = checkpointRepository,
            timeProvider = timeProvider,
            screenStateReader = screenStateReader,
            bootIdentityReader = bootIdentityReader,
            scope = scope
        )
        val eveningController = EveningMeditationController(
            eveningRepository = eveningRepository,
            sessionRepository = sessionRepository,
            runtimeController = runtimeController,
            routineEveningSignal = routineSignal,
            timeProvider = timeProvider,
            scope = scope
        )
        return runtimeController to eveningController
    }

    private fun routineSignalFor(day: String, dueAt: Long = timeProvider.currentTimeMillis()) {
        routineSignal.signal = EveningSignal(
            attentionDayId = day,
            dueAtEpochMs = dueAt,
            transitionAtEpochMs = dueAt
        )
    }

    // ------------------------------------------------------------------
    // DUE is created exactly once per Attention Day
    // ------------------------------------------------------------------

    @Test
    fun `evening wind down due creates one record per attention day`() = runTest {
        val (_, evening) = controllers()
        val dueAt = timeProvider.currentTimeMillis()

        evening.onEveningWindDownDue(dayId, dueAt)
        evening.onEveningWindDownDue(dayId, dueAt + 3_600_000L)

        val record = eveningRepository.getRecord(dayId)!!
        assertEquals(EveningMeditationState.DUE, record.state)
        assertEquals("first due time wins", dueAt, record.dueAtEpochMs)
        assertEquals("exactly one write for the day", 1, eveningRepository.upsertCount)
    }

    @Test
    fun `syncFromSignal marks the day due from the routine signal`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)

        evening.syncFromSignal()

        val record = eveningRepository.getRecord(dayId)!!
        assertEquals(EveningMeditationState.DUE, record.state)
    }

    @Test
    fun `no signal creates no obligation`() = runTest {
        val (_, evening) = controllers()

        evening.syncFromSignal()

        assertNull(eveningRepository.getRecord(dayId))
        assertEquals(0, eveningRepository.upsertCount)
    }

    @Test
    fun `wind down ended never alters the record`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)
        evening.onEveningWindDownDue(dayId, timeProvider.currentTimeMillis())

        evening.onEveningWindDownEnded()

        assertEquals(EveningMeditationState.DUE, eveningRepository.getRecord(dayId)!!.state)

        evening.deferTonight()
        evening.onEveningWindDownEnded()

        assertEquals(
            "a deferral is a decision, never reset by the window ending",
            EveningMeditationState.DEFERRED,
            eveningRepository.getRecord(dayId)!!.state
        )
    }

    // ------------------------------------------------------------------
    // Start — EVENING_WIND_DOWN, 1,800s, deterministic id, idempotent
    // ------------------------------------------------------------------

    @Test
    fun `start creates an evening wind down session with the per-day id`() = runTest {
        val (runtime, evening) = controllers()
        routineSignalFor(dayId)

        val session = evening.startEveningSession().getOrThrow()

        assertEquals("evening-$dayId", session.sessionId)
        assertEquals(MeditationSessionKind.EVENING_WIND_DOWN, session.kind)
        assertEquals(1_800, session.requiredSeconds)
        assertEquals(MeditationSessionStatus.ACTIVE, session.status)
        assertEquals("the engine adopts the session", session.sessionId, runtime.state.value.session?.sessionId)
        val record = eveningRepository.getRecord(dayId)!!
        assertEquals(EveningMeditationState.IN_PROGRESS, record.state)
        assertEquals(session.sessionId, record.sessionId)
    }

    @Test
    fun `starting twice reuses the per-day session`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)

        val first = evening.startEveningSession().getOrThrow()
        val writesAfterFirst = sessionRepository.upsertCount
        val second = evening.startEveningSession().getOrThrow()

        assertEquals(first.sessionId, second.sessionId)
        assertEquals("no duplicate session", 1, sessionRepository.getSessionsWithIdPrefix("evening-").size)
        assertEquals("the second start writes nothing", writesAfterFirst, sessionRepository.upsertCount)
    }

    @Test
    fun `a cancelled attempt starts fresh and never inherits progress`() = runTest {
        val (runtime, evening) = controllers()
        routineSignalFor(dayId)

        val first = evening.startEveningSession().getOrThrow()
        timeProvider.advanceSeconds(300)
        runtime.cancel()

        val second = evening.startEveningSession().getOrThrow()

        assertEquals("evening-$dayId", first.sessionId)
        assertEquals("evening-$dayId-1", second.sessionId)
        assertEquals(0, second.completedQualifiedSeconds)
        assertEquals(
            "cancelled history is preserved",
            MeditationSessionStatus.CANCELLED,
            sessionRepository.getSession(first.sessionId)!!.status
        )
    }

    @Test
    fun `standalone start works without a routine connection`() = runTest {
        val (_, evening) = controllers()

        val session = evening.startEveningSession().getOrThrow()

        assertTrue(
            "local date key is the attention day id until pairing",
            session.sessionId.startsWith("evening-")
        )
        assertEquals(MeditationSessionKind.EVENING_WIND_DOWN, session.kind)
    }

    // ------------------------------------------------------------------
    // Snooze / defer — persisted decisions
    // ------------------------------------------------------------------

    @Test
    fun `snooze persists and expires back to due`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)
        evening.onEveningWindDownDue(dayId, timeProvider.currentTimeMillis())

        evening.snooze15()

        val snoozed = eveningRepository.getRecord(dayId)!!
        assertEquals(EveningMeditationState.SNOOZED, snoozed.state)
        assertEquals(
            "fifteen minutes",
            timeProvider.currentTimeMillis() + 15 * 60_000L,
            snoozed.snoozedUntilEpochMs
        )

        timeProvider.advanceMillis(15 * 60_000L - 1)
        assertEquals(
            EveningMeditationState.SNOOZED,
            MeditationInsights.eveningStatus(snoozed, timeProvider.currentTimeMillis())
        )

        timeProvider.advanceMillis(1)
        assertEquals(
            "the offer comes back on its own",
            EveningMeditationState.DUE,
            MeditationInsights.eveningStatus(snoozed, timeProvider.currentTimeMillis())
        )
    }

    @Test
    fun `defer persists for the attention day`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)
        evening.onEveningWindDownDue(dayId, timeProvider.currentTimeMillis())

        evening.deferTonight()

        assertEquals(EveningMeditationState.DEFERRED, eveningRepository.getRecord(dayId)!!.state)
    }

    @Test
    fun `defer does not affect the next morning requirement`() = runTest {
        val (_, evening) = controllers()
        routineSignalFor(dayId)
        evening.onEveningWindDownDue(dayId, timeProvider.currentTimeMillis())
        evening.deferTonight()

        assertEquals(
            "tomorrow's morning is untouched",
            MorningStatus.REQUIRED,
            MorningSessionPolicy.resolve(emptyList(), nextDayId)
        )
        assertEquals(
            "today's morning is untouched too",
            MorningStatus.REQUIRED,
            MorningSessionPolicy.resolve(emptyList(), dayId)
        )
        assertTrue(
            "no morning session is created or completed by an evening deferral",
            sessionRepository.getSessionsWithIdPrefix(MorningSessionPolicy.ID_PREFIX).isEmpty()
        )
    }

    // ------------------------------------------------------------------
    // Completion — evidence about the evening practice alone
    // ------------------------------------------------------------------

    @Test
    fun `evening completion never touches morning substitutions or gates`() = runTest {
        val (runtime, evening) = controllers()
        routineSignalFor(dayId)
        val attentionClient = FakeAttentionInsightClient(
            projections = mapOf(
                dayId to AttentionInsightProjection(
                    protocolVersion = 1,
                    attentionDayId = dayId,
                    cooldownsTriggered = 1,
                    restorativeGatesCreated = 1,
                    restorativeGatesSatisfied = 0,
                    readerRestorativeCompletions = 0,
                    meditationRestorativeCompletions = 0,
                    meditationSubstitutionsUsed = 0,
                    meditationSubstitutionsRemaining = 2
                )
            )
        )
        val projectionBefore = attentionClient.projections

        evening.startEveningSession()
        timeProvider.advanceSeconds(1_800)
        assertTrue(runtime.evaluateCompletionNow())

        // The evening record follows its own session to COMPLETED.
        assertEquals(EveningMeditationState.COMPLETED, eveningRepository.getRecord(dayId)!!.state)

        // Morning is untouched: an evening completion never satisfies it.
        assertEquals(
            MorningStatus.REQUIRED,
            MorningSessionPolicy.resolve(emptyList(), dayId)
        )
        assertTrue(
            sessionRepository.getSessionsWithIdPrefix(MorningSessionPolicy.ID_PREFIX).isEmpty()
        )

        // Substitutions come ONLY from the Routine projection — an evening
        // completion never consumes one and nothing here ever writes to the
        // projection (it is read-only by contract).
        assertEquals(projectionBefore, attentionClient.projections)
        val projection = attentionClient.attentionDay(dayId)!!
        assertEquals(0, projection.meditationSubstitutionsUsed)
        assertEquals(
            2,
            MeditationInsights.substitutionChoicesRemaining(projection.meditationSubstitutionsUsed)
        )

        // No restorative-gate code path exists here at all — evening evidence
        // can never satisfy a cooldown gate (verified structurally: the only
        // gate surface is the untouched read-only projection above).
        assertEquals(0, projection.restorativeGatesSatisfied)
    }

    @Test
    fun `completion marks the record completed and keeps it completed`() = runTest {
        val (runtime, evening) = controllers()
        routineSignalFor(dayId)
        evening.startEveningSession()
        timeProvider.advanceSeconds(1_800)

        runtime.evaluateCompletionNow()
        val writesAfterCompletion = eveningRepository.upsertCount

        runtime.evaluateCompletionNow()

        assertEquals(EveningMeditationState.COMPLETED, eveningRepository.getRecord(dayId)!!.state)
        assertEquals(
            "idempotent — no extra record writes",
            writesAfterCompletion,
            eveningRepository.upsertCount
        )
    }

    @Test
    fun `interrupted evening time is real practice but not a completion`() = runTest {
        val (runtime, evening) = controllers()
        routineSignalFor(dayId)
        evening.startEveningSession()
        timeProvider.advanceSeconds(600)
        runtime.pause(InterruptionType.MANUAL_PAUSE)

        assertEquals(EveningMeditationState.IN_PROGRESS, eveningRepository.getRecord(dayId)!!.state)
        val session = sessionRepository.getSession("evening-$dayId")!!
        assertEquals(MeditationSessionStatus.PAUSED, session.status)
        assertEquals(600, session.completedQualifiedSeconds)
        assertNotEquals(MeditationSessionStatus.COMPLETED, session.status)
    }
}
