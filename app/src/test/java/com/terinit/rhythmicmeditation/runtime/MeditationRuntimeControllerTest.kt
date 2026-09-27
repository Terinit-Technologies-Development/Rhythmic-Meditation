package com.terinit.rhythmicmeditation.runtime

import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.integration.contract.LocalMeditationStatusRepository
import com.terinit.rhythmicmeditation.testutil.FakeBootIdentityReader
import com.terinit.rhythmicmeditation.testutil.FakeMeditationIntervalRepository
import com.terinit.rhythmicmeditation.testutil.FakeMeditationSessionRepository
import com.terinit.rhythmicmeditation.testutil.FakeScreenStateReader
import com.terinit.rhythmicmeditation.testutil.FakeSessionCheckpointRepository
import com.terinit.rhythmicmeditation.testutil.FakeSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.testutil.FakeTimeProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The trust tests for the meditation engine.
 *
 * These prove the properties Pass 3 depends on: qualification comes from the
 * monotonic ledger only, screen-off counts, interactive use doesn't, and no
 * failure mode (wall-clock tampering, process death, reboot, cancellation)
 * can manufacture qualified time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeditationRuntimeControllerTest {

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var sessionRepository: FakeMeditationSessionRepository
    private lateinit var intervalRepository: FakeMeditationIntervalRepository
    private lateinit var interruptionRepository: FakeSessionInterruptionEventRepository
    private lateinit var checkpointRepository: FakeSessionCheckpointRepository
    private lateinit var screenStateReader: FakeScreenStateReader
    private lateinit var bootIdentityReader: FakeBootIdentityReader
    private lateinit var service: MeditationSessionService

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
    }

    private fun newController(scope: CoroutineScope): MeditationRuntimeController =
        MeditationRuntimeController(
            sessionService = service,
            sessionRepository = sessionRepository,
            checkpointRepository = checkpointRepository,
            timeProvider = timeProvider,
            screenStateReader = screenStateReader,
            bootIdentityReader = bootIdentityReader,
            scope = scope
        )

    /**
     * A controller whose signal handlers run eagerly on the test scheduler.
     *
     * The controller's scope is derived from [TestScope.backgroundScope] so its
     * render/checkpoint ticker is cancelled automatically when the test ends —
     * an infinite ticker would otherwise keep the virtual scheduler busy
     * forever.
     */
    private fun TestScope.controller(): MeditationRuntimeController =
        newController(
            CoroutineScope(
                backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)
            )
        )

    private suspend fun MeditationRuntimeController.startMorning() =
        startMorningSession().getOrThrow()

    // ------------------------------------------------------------------
    // 30:00 -> COMPLETE / 29:59 -> incomplete
    // ------------------------------------------------------------------

    @Test
    fun `thirty minutes completes exactly at the requirement`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(1_800)

        assertTrue(controller.evaluateCompletionNow())

        val session = sessionRepository.getSession(controller.state.value.session!!.sessionId)!!
        assertEquals(MeditationSessionStatus.COMPLETED, session.status)
        assertEquals("credited exactly the requirement", 1_800, session.completedQualifiedSeconds)
        assertNotNull(session.completedAtEpochMs)
        // Completion closes the interval at the exact crossing, not later.
        val interval = intervalRepository.getIntervalsForSession(session.sessionId).single()
        assertEquals(
            FakeTimeProvider.DEFAULT_ELAPSED_MS + 1_800_000L,
            interval.endedElapsedRealtimeMs
        )
    }

    @Test
    fun `twenty nine minutes fifty nine seconds stays incomplete`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(1_799)

        assertFalse(controller.evaluateCompletionNow())

        val session = sessionRepository.getSession(controller.state.value.session!!.sessionId)!!
        assertEquals(MeditationSessionStatus.ACTIVE, session.status)
    }

    @Test
    fun `completion is idempotent and emits one event`() = runTest {
        val controller = controller()
        val events = mutableListOf<MeditationRuntimeEvent>()
        // Infinite collector: lives in backgroundScope so it never blocks the
        // test's completion.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            controller.events.collect { events += it }
        }
        controller.startMorning()
        timeProvider.advanceSeconds(1_800)

        assertTrue(controller.evaluateCompletionNow())
        assertFalse("second evaluation must be a no-op", controller.evaluateCompletionNow())
        controller.completeAtTarget() // explicit repeat must not double anything

        assertEquals(1, events.filterIsInstance<MeditationRuntimeEvent.Completed>().size)
        val session = sessionRepository.getSession(controller.state.value.session!!.sessionId)!!
        assertEquals(1_800, session.completedQualifiedSeconds)
    }

    // ------------------------------------------------------------------
    // Wall-clock tampering
    // ------------------------------------------------------------------

    @Test
    fun `wall clock jump forward adds no qualified time`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(600)

        timeProvider.jumpWallClock(2 * 60 * 60 * 1000L) // +2h wall clock

        assertEquals(600, controller.qualifiedSecondsNow())
    }

    @Test
    fun `wall clock rewind keeps monotonic progress`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(600)

        timeProvider.rewindWallClock(2 * 60 * 60 * 1000L) // -2h wall clock

        assertEquals(600, controller.qualifiedSecondsNow())
    }

    // ------------------------------------------------------------------
    // Screen-off vs interactive backgrounding
    // ------------------------------------------------------------------

    @Test
    fun `screen off keeps qualifying`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(600)

        // The user puts the phone down: screen off, app leaves foreground.
        screenStateReader.interactive = false
        controller.onScreenOff()
        controller.onAppBackground()

        val sessionId = controller.state.value.session!!.sessionId
        assertEquals(MeditationSessionStatus.ACTIVE, sessionRepository.getSession(sessionId)!!.status)
        assertNotNull("interval must stay open", intervalRepository.getOpenInterval(sessionId))

        // Real monotonic time keeps accruing while the screen is off.
        timeProvider.advanceSeconds(600)
        assertEquals(1_200, controller.qualifiedSecondsNow())
    }

    @Test
    fun `interactive app switch closes the interval and pauses`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(600)

        // The user actively uses another app: screen on, meditation in background.
        screenStateReader.interactive = true
        controller.onAppBackground()

        val sessionId = controller.state.value.session!!.sessionId
        assertEquals(MeditationSessionStatus.PAUSED, sessionRepository.getSession(sessionId)!!.status)
        assertNull(intervalRepository.getOpenInterval(sessionId))

        // Time spent outside the app is never counted.
        timeProvider.advanceSeconds(600)
        assertEquals(600, controller.qualifiedSecondsNow())
        assertEquals(600, sessionRepository.getSession(sessionId)!!.completedQualifiedSeconds)
    }

    @Test
    fun `backgrounding with screen off records no app background pause`() = runTest {
        val controller = controller()
        controller.startMorning()
        screenStateReader.interactive = false
        controller.onAppBackground()

        val events = interruptionRepository.getEventsForSession(controller.state.value.session!!.sessionId)
        assertTrue(events.none { it.type == InterruptionType.APP_BACKGROUND })
    }

    // ------------------------------------------------------------------
    // Pause / resume
    // ------------------------------------------------------------------

    @Test
    fun `pause ten minutes and resume twenty minutes completes at exactly thirty`() = runTest {
        val controller = controller()
        controller.startMorning()

        timeProvider.advanceSeconds(600) // 10 min meditating
        controller.pause()
        timeProvider.advanceSeconds(10 * 60) // 10 min paused — never counted
        controller.resume()
        timeProvider.advanceSeconds(1_199) // 19:59 — still one second short

        assertFalse(controller.evaluateCompletionNow())

        timeProvider.advanceSeconds(2) // crosses 30:00 during this interval
        assertTrue(controller.evaluateCompletionNow())

        val session = sessionRepository.getSession(controller.state.value.session!!.sessionId)!!
        assertEquals(MeditationSessionStatus.COMPLETED, session.status)
        assertEquals("paused time is never counted", 1_800, session.completedQualifiedSeconds)
    }

    @Test
    fun `manual pause closes the interval immediately`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(724)

        controller.pause()

        val sessionId = controller.state.value.session!!.sessionId
        val interval = intervalRepository.getIntervalsForSession(sessionId).single()
        assertFalse(interval.isOpen)
        assertEquals(724, sessionRepository.getSession(sessionId)!!.completedQualifiedSeconds)
    }

    // ------------------------------------------------------------------
    // Configuration changes
    // ------------------------------------------------------------------

    @Test
    fun `configuration recreation does not pause the session`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(60)

        controller.onHostActivityRecreated()

        val sessionId = controller.state.value.session!!.sessionId
        assertEquals(MeditationSessionStatus.ACTIVE, sessionRepository.getSession(sessionId)!!.status)
        assertNotNull(intervalRepository.getOpenInterval(sessionId))
        // It only takes a safe point.
        assertNotNull(checkpointRepository.get(sessionId))
    }

    // ------------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------------

    @Test
    fun `cancelling at twenty nine fifty nine can never complete`() = runTest {
        val controller = controller()
        val first = controller.startMorning()
        timeProvider.advanceSeconds(1_799)

        controller.endSessionConfirmed() // requirement not met -> cancel

        val cancelled = sessionRepository.getSession(first.sessionId)!!
        assertEquals(MeditationSessionStatus.CANCELLED, cancelled.status)
        assertNotEquals(MeditationSessionStatus.COMPLETED, cancelled.status)
        // History is kept for auditing/insights — but never as transferable credit.
        assertEquals(1_799, cancelled.completedQualifiedSeconds)
    }

    @Test
    fun `a new session never inherits cancelled progress`() = runTest {
        val controller = controller()
        val first = controller.startMorning()
        timeProvider.advanceSeconds(1_080) // 18 minutes
        controller.cancel()

        val second = controller.startMorning() // fresh id for the new attempt

        assertNotEquals(first.sessionId, second.sessionId)
        assertEquals(0, second.completedQualifiedSeconds)
        assertEquals(MeditationSessionStatus.ACTIVE, second.status)
    }

    // ------------------------------------------------------------------
    // Morning session identity
    // ------------------------------------------------------------------

    @Test
    fun `starting the morning session twice creates one logical session`() = runTest {
        val controller = controller()
        val first = controller.startMorning()
        timeProvider.advanceSeconds(30)
        val second = controller.startMorning()

        assertEquals(first.sessionId, second.sessionId)
        val mornings = sessionRepository.getSessionsWithIdPrefix("morning-")
        assertEquals(1, mornings.size)
        assertEquals(30, controller.qualifiedSecondsNow())
    }

    @Test
    fun `completed morning session is not restarted the same day`() = runTest {
        val controller = controller()
        val first = controller.startMorning()
        timeProvider.advanceSeconds(1_800)
        controller.evaluateCompletionNow()

        val again = controller.startMorning()

        assertEquals(first.sessionId, again.sessionId)
        assertEquals(MeditationSessionStatus.COMPLETED, again.status)
        assertEquals(1, sessionRepository.getSessionsWithIdPrefix("morning-").size)
    }

    // ------------------------------------------------------------------
    // Process death
    // ------------------------------------------------------------------

    @Test
    fun `process restore keeps only checkpointed time`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        timeProvider.advanceSeconds(60)
        controller.checkpointNow() // safe point: 60s

        // The process dies here; 25 unverified minutes pass before relaunch.
        timeProvider.advanceSeconds(1_500)

        val relaunched = controller()
        val report = relaunched.initializeAfterProcessStart()

        assertTrue(report.restoredToPaused)
        assertFalse(report.rebootDetected)
        val session = sessionRepository.getSession(started.sessionId)!!
        assertEquals(MeditationSessionStatus.PAUSED, session.status)
        assertEquals("only the checkpointed tail survives", 60, session.completedQualifiedSeconds)

        val restoreEvents = interruptionRepository.getEventsForSession(started.sessionId)
        assertTrue(restoreEvents.any { it.type == InterruptionType.PROCESS_RESTORE })

        // Resume is required before any further time counts.
        timeProvider.advanceSeconds(600)
        assertEquals(60, relaunched.qualifiedSecondsNow())
        relaunched.resume()
        timeProvider.advanceSeconds(60)
        assertEquals(120, relaunched.qualifiedSecondsNow())
    }

    @Test
    fun `process restore without a checkpoint credits nothing beyond closed intervals`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        timeProvider.advanceSeconds(300) // no checkpoint taken at all

        val relaunched = controller()
        relaunched.initializeAfterProcessStart()

        val session = sessionRepository.getSession(started.sessionId)!!
        assertEquals(MeditationSessionStatus.PAUSED, session.status)
        assertEquals(0, session.completedQualifiedSeconds)
    }

    // ------------------------------------------------------------------
    // Reboot
    // ------------------------------------------------------------------

    @Test
    fun `reboot credits no monotonic gap`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        timeProvider.advanceSeconds(100)
        controller.checkpointNow() // safe point: 100s at old-epoch elapsed

        // Device reboots: elapsedRealtime resets, wall clock jumps, boot count changes.
        timeProvider.elapsedMs = 5_000L
        timeProvider.jumpWallClock(9 * 60 * 60 * 1000L)
        bootIdentityReader.bootCount = 2

        val relaunched = controller()
        val report = relaunched.initializeAfterProcessStart()

        assertTrue(report.rebootDetected)
        val session = sessionRepository.getSession(started.sessionId)!!
        assertEquals(MeditationSessionStatus.PAUSED, session.status)
        assertEquals(
            "the reboot gap must never be credited",
            100,
            session.completedQualifiedSeconds
        )
    }

    // ------------------------------------------------------------------
    // Completion durability
    // ------------------------------------------------------------------

    @Test
    fun `completion status survives recreation and restart`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        timeProvider.advanceSeconds(1_800)
        controller.evaluateCompletionNow()
        val completedAt = sessionRepository.getSession(started.sessionId)!!.completedAtEpochMs

        // "Restart": a brand new controller over the same ledger.
        val relaunched = controller()
        relaunched.initializeAfterProcessStart()

        val statusRepository = LocalMeditationStatusRepository(
            sessionService = service,
            sessionRepository = sessionRepository
        )
        val status = statusRepository.getSessionStatus(started.sessionId)!!
        assertEquals("COMPLETED", status.status)
        assertEquals(1_800, status.completedQualifiedSeconds)
        assertEquals(completedAt, status.completedAtEpochMs)
    }

    // ------------------------------------------------------------------
    // Performance constraints
    // ------------------------------------------------------------------

    @Test
    fun `deriving time never writes to persistence`() = runTest {
        val controller = controller()
        controller.startMorning()
        timeProvider.advanceSeconds(30)
        controller.checkpointNow()
        val savesAfterCheckpoint = checkpointRepository.saveCount
        val writesAfterStart = sessionRepository.upsertCount

        // A render tick may happen every frame — it must stay read-only.
        repeat(100) {
            controller.qualifiedSecondsNow()
            controller.nowElapsedMs()
        }

        assertEquals(savesAfterCheckpoint, checkpointRepository.saveCount)
        assertEquals(writesAfterStart, sessionRepository.upsertCount)
    }

    @Test
    fun `periodic checkpoints run on the safe point cadence`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        val savesBefore = checkpointRepository.saveCount

        // 12 render ticks (one per second) -> exactly one checkpoint,
        // matching the 10-15s safe-point cadence. The fake clock and the
        // virtual scheduler advance in lockstep, as they would on a device.
        repeat(12) {
            timeProvider.advanceSeconds(1)
            advanceTimeBy(1_000)
            runCurrent()
        }

        assertEquals(savesBefore + 1, checkpointRepository.saveCount)
        val checkpoint = checkpointRepository.get(started.sessionId)!!
        assertEquals(12, checkpoint.qualifiedSecondsSnapshot)
    }

    @Test
    fun `checkpoint snapshot matches derived time`() = runTest {
        val controller = controller()
        val started = controller.startMorning()
        timeProvider.advanceSeconds(45)
        controller.checkpointNow()

        val checkpoint = checkpointRepository.get(started.sessionId)!!
        assertEquals(45, checkpoint.qualifiedSecondsSnapshot)
        assertEquals(timeProvider.elapsedRealtimeMillis(), checkpoint.atElapsedRealtimeMs)
        assertEquals(bootIdentityReader.bootCount(), checkpoint.bootCount)
    }

    @Test
    fun `state exposes paused and active correctly for the UI`() = runTest {
        val controller = controller()
        controller.startMorning()
        assertTrue(controller.state.value.isActive)

        controller.pause()
        assertTrue(controller.state.value.isPaused)
        assertEquals(
            MeditationSessionStatus.PAUSED,
            controller.state.first { it.session != null }.session?.status
        )
    }
}
