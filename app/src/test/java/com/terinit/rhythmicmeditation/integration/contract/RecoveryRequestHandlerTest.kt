package com.terinit.rhythmicmeditation.integration.contract

import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryRequest
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.testutil.FakeCallerTrustPolicy
import com.terinit.rhythmicmeditation.testutil.FakeMeditationIntervalRepository
import com.terinit.rhythmicmeditation.testutil.FakeMeditationSessionRepository
import com.terinit.rhythmicmeditation.testutil.FakeSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.testutil.FakeTimeProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * IPC contract tests: a valid recovery request must be able to arrive,
 * validate, verify its caller, create/load idempotently, and route into the
 * session UI — while malformed or unverified traffic is rejected without side
 * effects. Caller verification is exercised with an injected trust policy;
 * production stays deny-by-default.
 */
class RecoveryRequestHandlerTest {

    private lateinit var timeProvider: FakeTimeProvider
    private lateinit var sessionRepository: FakeMeditationSessionRepository
    private lateinit var sessionService: MeditationSessionService
    private lateinit var statusRepository: LocalMeditationStatusRepository

    private val peerPackage = "com.terinit.rhythmicroutine"
    private val peerDigest = "AB:CD:EF"

    private val trustedCaller = CallerIdentity(peerPackage, setOf(peerDigest))

    @Before
    fun setUp() {
        timeProvider = FakeTimeProvider()
        sessionRepository = FakeMeditationSessionRepository()
        sessionService = MeditationSessionService(
            sessionRepository = sessionRepository,
            intervalRepository = FakeMeditationIntervalRepository(),
            interruptionRepository = FakeSessionInterruptionEventRepository(),
            timeProvider = timeProvider
        )
        statusRepository = LocalMeditationStatusRepository(
            sessionService = sessionService,
            sessionRepository = sessionRepository
        )
    }

    private fun handler(trusted: Boolean): RecoveryRequestHandler = RecoveryRequestHandler(
        callerVerifier = CallerVerifier(
            FakeCallerTrustPolicy(
                packageName = if (trusted) peerPackage else null,
                digests = if (trusted) setOf(peerDigest) else emptySet()
            )
        ),
        sessionService = sessionService,
        sessionStatusRepository = statusRepository
    )

    private fun request(
        sessionId: String = "routine-session-1",
        requiredSeconds: Int = 1_800
    ) = MeditationRecoveryRequest(
        sessionId = sessionId,
        protocolVersion = 1,
        sessionKind = "MORNING_REQUIRED",
        requiredQualifiedSeconds = requiredSeconds,
        createdAtEpochMs = FakeTimeProvider.DEFAULT_WALL_CLOCK_MS,
        expiresAtEpochMs = null,
        sourceCooldownId = "cooldown-4",
        sourceRiskGroupId = "risk-1",
        sourceRhythmicDayId = "day-1"
    )

    @Test
    fun `valid request from verified caller creates and routes to the session`() = runTest {
        val outcome = handler(trusted = true).handle(request(), trustedCaller).getOrThrow()

        assertEquals(
            RecoveryOutcome.RouteToSession("routine-session-1", alreadyCompleted = false),
            outcome
        )
        val session = sessionRepository.getSession("routine-session-1")!!
        assertEquals(MeditationSessionStatus.PENDING, session.status)
        assertEquals(1_800, session.requiredSeconds)
    }

    @Test
    fun `same request twice yields one logical session`() = runTest {
        val handler = handler(trusted = true)

        handler.handle(request(), trustedCaller).getOrThrow()
        handler.handle(request(), trustedCaller).getOrThrow()

        assertEquals(1, sessionRepository.getSessionsWithIdPrefix("routine-session-1").size)
    }

    @Test
    fun `verified request loads an existing completed session`() = runTest {
        val handler = handler(trusted = true)
        handler.handle(request(), trustedCaller).getOrThrow()
        sessionService.startSession("routine-session-1").getOrThrow()
        timeProvider.advanceSeconds(1_800)
        sessionService.completeAtQualificationTarget("routine-session-1").getOrThrow()

        val outcome = handler.handle(request(), trustedCaller).getOrThrow()

        assertEquals(
            RecoveryOutcome.RouteToSession("routine-session-1", alreadyCompleted = true),
            outcome
        )
    }

    @Test
    fun `unverified caller is rejected and nothing is created`() = runTest {
        val result = handler(trusted = false).handle(request(), trustedCaller)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
        assertEquals(0, sessionRepository.getSessionsWithIdPrefix("routine-").size)
    }

    @Test
    fun `unattested caller identity fails closed`() = runTest {
        val result = handler(trusted = true).handle(request(), CallerIdentity.UNKNOWN)

        assertTrue(result.isFailure)
        assertEquals(0, sessionRepository.getSessionsWithIdPrefix("routine-").size)
    }

    @Test
    fun `malformed requests are rejected before any session exists`() = runTest {
        val handler = handler(trusted = true)

        val malformed = listOf(
            request().copy(sessionId = " "),
            request().copy(requiredQualifiedSeconds = 0),
            request().copy(protocolVersion = 0),
            request().copy(protocolVersion = 99),
            request().copy(sessionKind = "POWER_NAP"),
            request().copy(sessionKind = "STANDALONE"),
            request().copy(createdAtEpochMs = 0L)
        )
        malformed.forEach { bad ->
            val result = handler.handle(bad, trustedCaller)
            assertTrue("must reject: $bad", result.isFailure)
        }
        assertEquals(0, sessionRepository.getSessionsWithIdPrefix("routine-").size)
    }

    @Test
    fun `a new session id never inherits prior progress`() = runTest {
        val handler = handler(trusted = true)
        handler.handle(request(sessionId = "routine-a"), trustedCaller).getOrThrow()
        sessionService.startSession("routine-a").getOrThrow()
        timeProvider.advanceSeconds(1_000)
        sessionService.cancelSession("routine-a").getOrThrow()

        handler.handle(request(sessionId = "routine-b"), trustedCaller).getOrThrow()

        val sessionB = sessionRepository.getSession("routine-b")!!
        assertEquals(0, sessionB.completedQualifiedSeconds)
        assertFalse(sessionB.isRequirementMet)
    }
}
