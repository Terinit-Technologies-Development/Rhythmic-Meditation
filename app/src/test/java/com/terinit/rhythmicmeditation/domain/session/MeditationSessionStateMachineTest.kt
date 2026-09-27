package com.terinit.rhythmicmeditation.domain.session

import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeditationSessionStateMachineTest {

    @Test
    fun `pending can transition to active`() {
        assertTrue(
            MeditationSessionStateMachine.canTransition(
                MeditationSessionStatus.PENDING,
                MeditationSessionStatus.ACTIVE
            )
        )
    }

    @Test
    fun `pending can transition to cancelled, invalid and expired`() {
        listOf(
            MeditationSessionStatus.CANCELLED,
            MeditationSessionStatus.INVALID,
            MeditationSessionStatus.EXPIRED
        ).forEach { target ->
            assertTrue(
                "PENDING -> $target should be allowed",
                MeditationSessionStateMachine.canTransition(MeditationSessionStatus.PENDING, target)
            )
        }
    }

    @Test
    fun `active can pause, complete, cancel and expire`() {
        listOf(
            MeditationSessionStatus.PAUSED,
            MeditationSessionStatus.COMPLETED,
            MeditationSessionStatus.CANCELLED,
            MeditationSessionStatus.EXPIRED
        ).forEach { target ->
            assertTrue(
                "ACTIVE -> $target should be allowed",
                MeditationSessionStateMachine.canTransition(MeditationSessionStatus.ACTIVE, target)
            )
        }
    }

    @Test
    fun `paused can resume, cancel and expire`() {
        listOf(
            MeditationSessionStatus.ACTIVE,
            MeditationSessionStatus.CANCELLED,
            MeditationSessionStatus.EXPIRED
        ).forEach { target ->
            assertTrue(
                "PAUSED -> $target should be allowed",
                MeditationSessionStateMachine.canTransition(MeditationSessionStatus.PAUSED, target)
            )
        }
    }

    @Test
    fun `paused cannot complete directly`() {
        assertFalse(
            MeditationSessionStateMachine.canTransition(
                MeditationSessionStatus.PAUSED,
                MeditationSessionStatus.COMPLETED
            )
        )
    }

    @Test
    fun `pending cannot pause or complete`() {
        assertFalse(
            MeditationSessionStateMachine.canTransition(
                MeditationSessionStatus.PENDING,
                MeditationSessionStatus.PAUSED
            )
        )
        assertFalse(
            MeditationSessionStateMachine.canTransition(
                MeditationSessionStatus.PENDING,
                MeditationSessionStatus.COMPLETED
            )
        )
    }

    @Test
    fun `terminal states cannot transition`() {
        val terminals = listOf(
            MeditationSessionStatus.COMPLETED,
            MeditationSessionStatus.CANCELLED,
            MeditationSessionStatus.EXPIRED,
            MeditationSessionStatus.INVALID
        )
        terminals.forEach { from ->
            assertTrue("state machine should not allow transitions out of $from",
                MeditationSessionStateMachine.allowedTargets(from).isEmpty())
            assertTrue("$from should be terminal", MeditationSessionStateMachine.isTerminal(from))
            MeditationSessionStatus.entries.forEach { to ->
                assertFalse(
                    "$from -> $to should be illegal",
                    MeditationSessionStateMachine.canTransition(from, to)
                )
            }
        }
    }
}
