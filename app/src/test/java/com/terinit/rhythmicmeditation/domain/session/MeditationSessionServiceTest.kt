package com.terinit.rhythmicmeditation.domain.session

import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.testutil.FakeMeditationIntervalRepository
import com.terinit.rhythmicmeditation.testutil.FakeMeditationSessionRepository
import com.terinit.rhythmicmeditation.testutil.FakeSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.testutil.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeditationSessionServiceTest {

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var sessionRepository: FakeMeditationSessionRepository
    private lateinit var intervalRepository: FakeMeditationIntervalRepository
    private lateinit var interruptionRepository: FakeSessionInterruptionEventRepository
    private lateinit var service: MeditationSessionService

    @Before
    fun setUp() {
        timeProvider = FakeTimeProvider()
        sessionRepository = FakeMeditationSessionRepository()
        intervalRepository = FakeMeditationIntervalRepository()
        interruptionRepository = FakeSessionInterruptionEventRepository()
        service = MeditationSessionService(
            sessionRepository = sessionRepository,
            intervalRepository = intervalRepository,
            interruptionRepository = interruptionRepository,
            timeProvider = timeProvider
        )
    }

    private fun recoveryRequest(
        sessionId: String = "session-1",
        requiredSeconds: Int = 60,
        createdAt: Long = FakeTimeProvider.DEFAULT_WALL_CLOCK_MS
    ) = MeditationRecoveryRequest(
        sessionId = sessionId,
        protocolVersion = 1,
        sessionKind = "MORNING_REQUIRED",
        requiredQualifiedSeconds = requiredSeconds,
        createdAtEpochMs = createdAt,
        expiresAtEpochMs = null,
        sourceCooldownId = null,
        sourceRiskGroupId = null,
        sourceRhythmicDayId = "day-1"
    )

    // ------------------------------------------------------------------
    // createOrReplaceSession semantics
    // ------------------------------------------------------------------

    @Test
    fun `createOrReplaceSession creates a pending session`() = runTest {
        val created = service.createOrReplaceSession(recoveryRequest()).getOrThrow()

        assertEquals("session-1", created.sessionId)
        assertEquals(MeditationSessionStatus.PENDING, created.status)
        assertEquals(60, created.requiredSeconds)
        assertEquals(0, created.completedQualifiedSeconds)
        assertNotNull(sessionRepository.getSession("session-1"))
    }

    @Test
    fun `identical recovery request is idempotent`() = runTest {
        val first = service.createOrReplaceSession(recoveryRequest()).getOrThrow()
        val writesAfterFirst = sessionRepository.upsertCount

        val second = service.createOrReplaceSession(recoveryRequest()).getOrThrow()

        assertEquals(first, second)
        assertEquals("no additional writes expected", writesAfterFirst, sessionRepository.upsertCount)
    }

    @Test
    fun `changed recovery request replaces metadata but keeps evidence`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 60))
        service.startSession("session-1")
        timeProvider.advanceSeconds(10)
        service.pauseSession("session-1")

        val replaced = service.createOrReplaceSession(
            recoveryRequest(requiredSeconds = 120).copy(sourceCooldownId = "cooldown-9")
        ).getOrThrow()

        assertEquals(120, replaced.requiredSeconds)
        assertEquals("cooldown-9", replaced.sourceCooldownId)
        assertEquals(10, replaced.completedQualifiedSeconds)
        assertEquals(MeditationSessionStatus.PAUSED, replaced.status)
    }

    @Test
    fun `replacement of a finished session resets status to pending`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 10))
        service.startSession("session-1")
        timeProvider.advanceSeconds(10)
        service.completeSession("session-1")

        val replaced = service.createOrReplaceSession(recoveryRequest(requiredSeconds = 30)).getOrThrow()

        assertEquals(MeditationSessionStatus.PENDING, replaced.status)
        assertNull(replaced.completedAtEpochMs)
    }

    @Test
    fun `invalid recovery request is rejected and creates nothing`() = runTest {
        val result = service.createOrReplaceSession(
            recoveryRequest(requiredSeconds = 0)
        )

        assertTrue(result.isFailure)
        assertNull(sessionRepository.getSession("session-1"))
    }

    // ------------------------------------------------------------------
    // Lifecycle + interval closing
    // ------------------------------------------------------------------

    @Test
    fun `startSession opens an interval and is idempotent`() = runTest {
        service.createOrReplaceSession(recoveryRequest())
        val started = service.startSession("session-1").getOrThrow()

        assertEquals(MeditationSessionStatus.ACTIVE, started.status)
        assertEquals(FakeTimeProvider.DEFAULT_WALL_CLOCK_MS, started.startedAtEpochMs)
        assertEquals(1, intervalRepository.getIntervalsForSession("session-1").size)

        // Starting again must not open a second interval.
        service.startSession("session-1")
        assertEquals(1, intervalRepository.getIntervalsForSession("session-1").size)
    }

    @Test
    fun `pause closes the interval and accumulates qualified seconds`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 60))
        service.startSession("session-1")
        timeProvider.advanceSeconds(25)

        val paused = service.pauseSession("session-1", InterruptionType.ESSENTIAL_ACCESS).getOrThrow()

        assertEquals(MeditationSessionStatus.PAUSED, paused.status)
        assertEquals(1, paused.pauseCount)
        val intervals = intervalRepository.getIntervalsForSession("session-1")
        assertEquals(1, intervals.size)
        assertFalse(intervals.first().isOpen)
        assertEquals(25, sessionRepository.getSession("session-1")!!.completedQualifiedSeconds)
    }

    @Test
    fun `pause and resume cycles accumulate distinct intervals`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 90))
        service.startSession("session-1")

        timeProvider.advanceSeconds(20)
        service.pauseSession("session-1")
        timeProvider.advanceSeconds(500) // paused time must not count
        service.resumeSession("session-1")
        timeProvider.advanceSeconds(35)
        service.completeSession("session-1")

        val session = sessionRepository.getSession("session-1")!!
        assertEquals(MeditationSessionStatus.COMPLETED, session.status)
        assertEquals(55, session.completedQualifiedSeconds)
        assertEquals(1, session.pauseCount)

        val intervals = intervalRepository.getIntervalsForSession("session-1")
        assertEquals(2, intervals.size)
        assertTrue("all intervals must be closed", intervals.none { it.isOpen })
        assertEquals(
            "wall clock diagnostics recorded",
            2,
            intervals.count { it.endedWallClockMs != null }
        )
    }

    @Test
    fun `resume is idempotent while active`() = runTest {
        service.createOrReplaceSession(recoveryRequest())
        service.startSession("session-1")

        service.resumeSession("session-1")

        assertEquals(1, intervalRepository.getIntervalsForSession("session-1").size)
    }

    @Test
    fun `completeSession sets completedAt and closes the interval`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 30))
        service.startSession("session-1")
        timeProvider.advanceSeconds(30)

        val completed = service.completeSession("session-1").getOrThrow()

        assertEquals(MeditationSessionStatus.COMPLETED, completed.status)
        assertEquals(FakeTimeProvider.DEFAULT_WALL_CLOCK_MS + 30_000L, completed.completedAtEpochMs)
        assertTrue(completed.isRequirementMet)
        assertNull(intervalRepository.getOpenInterval("session-1"))
    }

    @Test
    fun `cancelSession accumulates only the active interval`() = runTest {
        service.createOrReplaceSession(recoveryRequest())
        service.startSession("session-1")
        timeProvider.advanceSeconds(12)
        service.cancelSession("session-1")

        val session = sessionRepository.getSession("session-1")!!
        assertEquals(MeditationSessionStatus.CANCELLED, session.status)
        assertEquals(12, session.completedQualifiedSeconds)
    }

    @Test
    fun `illegal transitions fail without changing state`() = runTest {
        service.createOrReplaceSession(recoveryRequest())

        // PENDING cannot be paused or completed directly.
        assertTrue(service.pauseSession("session-1").isFailure)
        assertTrue(service.completeSession("session-1").isFailure)
        assertEquals(
            MeditationSessionStatus.PENDING,
            sessionRepository.getSession("session-1")!!.status
        )
    }

    @Test
    fun `unknown session operations fail`() = runTest {
        assertTrue(service.startSession("missing").isFailure)
        assertTrue(service.cancelSession("missing").isFailure)
        assertEquals(0, service.currentQualifiedSeconds("missing"))
    }

    // ------------------------------------------------------------------
    // Qualified time + interruptions
    // ------------------------------------------------------------------

    @Test
    fun `currentQualifiedSeconds includes the open interval`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 100))
        service.startSession("session-1")
        timeProvider.advanceSeconds(40)

        assertEquals(40, service.currentQualifiedSeconds("session-1"))
    }

    @Test
    fun `currentQualifiedSeconds ignores paused time`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 100))
        service.startSession("session-1")
        timeProvider.advanceSeconds(40)
        service.pauseSession("session-1")
        timeProvider.advanceSeconds(600)

        assertEquals(40, service.currentQualifiedSeconds("session-1"))
    }

    @Test
    fun `wall clock tampering does not affect qualified time`() = runTest {
        service.createOrReplaceSession(recoveryRequest(requiredSeconds = 100))
        service.startSession("session-1")

        timeProvider.advanceSeconds(30)
        timeProvider.jumpWallClock(3_600_000) // clock jumps forward one hour
        assertEquals(30, service.currentQualifiedSeconds("session-1"))

        timeProvider.rewindWallClock(7_200_000) // clock rewinds two hours
        assertEquals(30, service.currentQualifiedSeconds("session-1"))
    }

    @Test
    fun `recordInterruption stores an event and counts it`() = runTest {
        service.createOrReplaceSession(recoveryRequest())
        service.recordInterruption("session-1", InterruptionType.PHONE_CALL, note = "incoming")

        val session = sessionRepository.getSession("session-1")!!
        assertEquals(1, session.interruptionCount)

        val events = interruptionRepository.getEventsForSession("session-1")
        assertEquals(1, events.size)
        assertEquals(InterruptionType.PHONE_CALL, events.first().type)
        assertEquals("incoming", events.first().note)
    }

    @Test
    fun `local sessions are created pending with generated id`() = runTest {
        val session = service.createLocalSession(
            kind = MeditationSessionKind.EVENING_WIND_DOWN,
            requiredSeconds = 1800,
            sourceRhythmicDayId = "day-7"
        ).getOrThrow()

        assertEquals(MeditationSessionKind.EVENING_WIND_DOWN, session.kind)
        assertEquals(MeditationSessionStatus.PENDING, session.status)
        assertTrue(session.sessionId.isNotBlank())
    }

    @Test
    fun `local session requires positive duration`() = runTest {
        assertTrue(
            service.createLocalSession(MeditationSessionKind.STANDALONE, requiredSeconds = 0)
                .isFailure
        )
    }
}
