package com.terinit.rhythmicmeditation.domain.evening

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The evening state machine: one obligation per Attention Day, snooze and
 * defer as first-class decisions, and terminal states that can never be
 * revived or re-earned. Evening is kept entirely out of morning and
 * restorative-gate logic — see EveningMeditationControllerTest for the
 * cross-boundary guarantees.
 */
class EveningMeditationTest {

    private val dayId = "2026-09-27"
    private val now = 1_700_000_000_000L

    private fun due(state: EveningMeditationState = EveningMeditationState.DUE) =
        EveningMeditationRecord(
            attentionDayId = dayId,
            state = state,
            dueAtEpochMs = now,
            snoozedUntilEpochMs = null,
            sessionId = null,
            updatedAtEpochMs = now
        )

    // ------------------------------------------------------------------
    // markDue — exactly once per Attention Day
    // ------------------------------------------------------------------

    @Test
    fun `markDue creates a due record when none exists`() {
        val record = EveningMeditation.markDue(null, dayId, now)

        assertEquals(dayId, record.attentionDayId)
        assertEquals(EveningMeditationState.DUE, record.state)
        assertEquals(now, record.dueAtEpochMs)
        assertEquals(now, record.updatedAtEpochMs)
    }

    @Test
    fun `markDue transitions a not-due record to due`() {
        val notDue = due(EveningMeditationState.NOT_DUE).copy(dueAtEpochMs = null)

        val record = EveningMeditation.markDue(notDue, dayId, now)

        assertEquals(EveningMeditationState.DUE, record.state)
        assertEquals(now, record.dueAtEpochMs)
    }

    @Test
    fun `markDue happens exactly once per attention day`() {
        val first = EveningMeditation.markDue(null, dayId, now)

        val second = EveningMeditation.markDue(first, dayId, now + 3_600_000L)

        assertSame("second signal must not create a second obligation", first, second)
        assertEquals("first due time wins", now, second.dueAtEpochMs)
    }

    @Test
    fun `markDue never resets a deferred decision`() {
        val deferred = EveningMeditation.defer(due(), now)

        val record = EveningMeditation.markDue(deferred, dayId, now + 3_600_000L)

        assertEquals(EveningMeditationState.DEFERRED, record.state)
    }

    @Test
    fun `markDue never resets a completed record`() {
        val completed = EveningMeditation.markCompleted(
            due(EveningMeditationState.IN_PROGRESS).copy(sessionId = "evening-$dayId")
        )

        val record = EveningMeditation.markDue(completed, dayId, now + 3_600_000L)

        assertEquals(EveningMeditationState.COMPLETED, record.state)
    }

    // ------------------------------------------------------------------
    // Snooze — persisted, and effectively DUE again on expiry
    // ------------------------------------------------------------------

    @Test
    fun `snooze stores the fifteen minute deadline`() {
        val record = EveningMeditation.snooze(due(), now)

        assertEquals(EveningMeditationState.SNOOZED, record.state)
        assertEquals(now + 15 * 60_000L, record.snoozedUntilEpochMs)
        assertEquals(EveningMeditation.SNOOZE_DURATION_MS, record.snoozedUntilEpochMs!! - now)
    }

    @Test
    fun `effective state of a missing record is not due`() {
        assertEquals(EveningMeditationState.NOT_DUE, EveningMeditation.effectiveState(null, now))
    }

    @Test
    fun `snoozed record is still snoozed before the deadline`() {
        val snoozed = EveningMeditation.snooze(due(), now)

        assertEquals(
            EveningMeditationState.SNOOZED,
            EveningMeditation.effectiveState(snoozed, now + 15 * 60_000L - 1)
        )
    }

    @Test
    fun `snooze expiry returns the effective state to due`() {
        val snoozed = EveningMeditation.snooze(due(), now)

        assertEquals(
            EveningMeditationState.DUE,
            EveningMeditation.effectiveState(snoozed, now + 15 * 60_000L)
        )
    }

    @Test
    fun `re-snoozing extends the deadline`() {
        val snoozed = EveningMeditation.snooze(due(), now)

        val resnoozed = EveningMeditation.snooze(snoozed, now + 5 * 60_000L)

        assertEquals(EveningMeditationState.SNOOZED, resnoozed.state)
        assertEquals(now + 5 * 60_000L + 15 * 60_000L, resnoozed.snoozedUntilEpochMs)
    }

    @Test
    fun `snooze is a no-op outside due and snoozed`() {
        val inProgress = EveningMeditation.markInProgress(due(), "evening-$dayId")
        val completed = EveningMeditation.markCompleted(inProgress)

        assertSame(inProgress, EveningMeditation.snooze(inProgress, now))
        assertSame(completed, EveningMeditation.snooze(completed, now))
    }

    @Test
    fun `resume from snooze returns the offer to due`() {
        val snoozed = EveningMeditation.snooze(due(), now)

        val resumed = EveningMeditation.resumeFromSnooze(snoozed, now + 60_000L)

        assertEquals(EveningMeditationState.DUE, resumed.state)
        assertNull(resumed.snoozedUntilEpochMs)
    }

    @Test
    fun `resume from snooze is a no-op outside snoozed`() {
        val due = due()

        assertSame(due, EveningMeditation.resumeFromSnooze(due, now))
    }

    // ------------------------------------------------------------------
    // Defer — a decision, persisted for this Attention Day only
    // ------------------------------------------------------------------

    @Test
    fun `defer persists for the attention day`() {
        val deferred = EveningMeditation.defer(due(), now)

        assertEquals(EveningMeditationState.DEFERRED, deferred.state)
        assertNull("no snooze survives a deferral", deferred.snoozedUntilEpochMs)
    }

    @Test
    fun `defer clears a snooze`() {
        val snoozed = EveningMeditation.snooze(due(), now)

        val deferred = EveningMeditation.defer(snoozed, now)

        assertEquals(EveningMeditationState.DEFERRED, deferred.state)
        assertNull(deferred.snoozedUntilEpochMs)
    }

    @Test
    fun `defer is a no-op outside due and snoozed`() {
        val completed = EveningMeditation.markCompleted(
            due(EveningMeditationState.IN_PROGRESS).copy(sessionId = "evening-$dayId")
        )

        assertSame(completed, EveningMeditation.defer(completed, now))
    }

    // ------------------------------------------------------------------
    // markInProgress / markCompleted
    // ------------------------------------------------------------------

    @Test
    fun `markInProgress binds the evening session id`() {
        val record = EveningMeditation.markInProgress(due(), "evening-$dayId")

        assertEquals(EveningMeditationState.IN_PROGRESS, record.state)
        assertEquals("evening-$dayId", record.sessionId)
    }

    @Test
    fun `markInProgress rebinds a fresh attempt while in progress`() {
        val inProgress = EveningMeditation.markInProgress(due(), "evening-$dayId")

        val rebound = EveningMeditation.markInProgress(inProgress, "evening-$dayId-2")

        assertEquals(EveningMeditationState.IN_PROGRESS, rebound.state)
        assertEquals("evening-$dayId-2", rebound.sessionId)
    }

    @Test
    fun `markInProgress never revives a terminal decision`() {
        val deferred = EveningMeditation.defer(due(), now)
        val completed = EveningMeditation.markCompleted(
            due(EveningMeditationState.IN_PROGRESS).copy(sessionId = "evening-$dayId")
        )

        assertSame(deferred, EveningMeditation.markInProgress(deferred, "evening-$dayId"))
        assertSame(completed, EveningMeditation.markInProgress(completed, "evening-$dayId"))
    }

    @Test
    fun `markCompleted completes from in progress`() {
        val inProgress = EveningMeditation.markInProgress(due(), "evening-$dayId")

        val record = EveningMeditation.markCompleted(inProgress)

        assertEquals(EveningMeditationState.COMPLETED, record.state)
    }

    @Test
    fun `markCompleted is idempotent`() {
        val completed = EveningMeditation.markCompleted(
            EveningMeditation.markInProgress(due(), "evening-$dayId")
        )

        assertSame(completed, EveningMeditation.markCompleted(completed))
    }

    @Test
    fun `markCompleted is a no-op outside in progress`() {
        val deferred = EveningMeditation.defer(due(), now)

        assertSame(deferred, EveningMeditation.markCompleted(deferred))
    }

    // ------------------------------------------------------------------
    // Session identity + transitions end to end
    // ------------------------------------------------------------------

    @Test
    fun `session id is deterministic per attention day`() {
        assertEquals("evening-$dayId", EveningMeditation.eveningMeditationSessionId(dayId))
    }

    @Test
    fun `next attempt id suffixes after a cancelled attempt`() {
        assertEquals("evening-$dayId", EveningMeditation.nextSessionId(emptyList(), dayId))
        assertEquals(
            "evening-$dayId-1",
            EveningMeditation.nextSessionId(listOf("evening-$dayId"), dayId)
        )
        assertEquals(
            "evening-$dayId-2",
            EveningMeditation.nextSessionId(listOf("evening-$dayId", "evening-$dayId-1"), dayId)
        )
    }

    @Test
    fun `evening requirement is the standard thirty minutes`() {
        assertEquals(1_800, EveningMeditation.REQUIRED_SECONDS)
    }

    @Test
    fun `a full evening flows due to snooze to due to complete`() {
        val dueRecord = EveningMeditation.markDue(null, dayId, now)
        val snoozed = EveningMeditation.snooze(dueRecord, now)
        val backDue = snoozed.copy(
            state = EveningMeditation.effectiveState(snoozed, now + 15 * 60_000L)
        )
        val inProgress = EveningMeditation.markInProgress(backDue, "evening-$dayId")
        val completed = EveningMeditation.markCompleted(inProgress)

        assertEquals(
            listOf(
                EveningMeditationState.DUE,
                EveningMeditationState.SNOOZED,
                EveningMeditationState.DUE,
                EveningMeditationState.IN_PROGRESS,
                EveningMeditationState.COMPLETED
            ),
            listOf(
                dueRecord.state,
                snoozed.state,
                backDue.state,
                inProgress.state,
                completed.state
            )
        )
    }

    @Test
    fun `state wire values round trip`() {
        EveningMeditationState.entries.forEach { state ->
            assertEquals(state, EveningMeditationState.fromWire(state.name))
        }
        assertNull(EveningMeditationState.fromWire("NOT_A_STATE"))
        assertNull(EveningMeditationState.fromWire(null))
    }
}
