package com.terinit.rhythmicmeditation.data.mapper

import com.terinit.rhythmicmeditation.data.local.entity.EveningMeditationEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationInsightSnapshotEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import com.terinit.rhythmicmeditation.data.local.entity.SessionInterruptionEventEntity
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationInsightSnapshot
import com.terinit.rhythmicmeditation.domain.model.MeditationInterval
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.domain.model.SessionInterruptionEvent

/**
 * Entity <-> domain mapping.
 *
 * Unknown enum wire values map to safe defaults (STANDALONE / INVALID /
 * UNKNOWN) so a corrupt row degrades gracefully instead of crashing.
 */

fun MeditationSessionEntity.toDomain(): MeditationSession = MeditationSession(
    sessionId = sessionId,
    protocolVersion = protocolVersion,
    kind = MeditationSessionKind.fromWire(kind) ?: MeditationSessionKind.STANDALONE,
    status = MeditationSessionStatus.fromWire(status) ?: MeditationSessionStatus.INVALID,
    requiredSeconds = requiredSeconds,
    completedQualifiedSeconds = completedQualifiedSeconds,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    expiresAtEpochMs = expiresAtEpochMs,
    sourceCooldownId = sourceCooldownId,
    sourceRhythmicDayId = sourceRhythmicDayId,
    interruptionCount = interruptionCount,
    pauseCount = pauseCount
)

fun MeditationSession.toEntity(): MeditationSessionEntity = MeditationSessionEntity(
    sessionId = sessionId,
    protocolVersion = protocolVersion,
    kind = kind.name,
    status = status.name,
    requiredSeconds = requiredSeconds,
    completedQualifiedSeconds = completedQualifiedSeconds,
    startedAtEpochMs = startedAtEpochMs,
    completedAtEpochMs = completedAtEpochMs,
    createdAtEpochMs = createdAtEpochMs,
    expiresAtEpochMs = expiresAtEpochMs,
    sourceCooldownId = sourceCooldownId,
    sourceRhythmicDayId = sourceRhythmicDayId,
    interruptionCount = interruptionCount,
    pauseCount = pauseCount
)

fun MeditationIntervalEntity.toDomain(): MeditationInterval = MeditationInterval(
    id = id,
    sessionId = sessionId,
    startedElapsedRealtimeMs = startedElapsedRealtimeMs,
    endedElapsedRealtimeMs = endedElapsedRealtimeMs,
    startedWallClockMs = startedWallClockMs,
    endedWallClockMs = endedWallClockMs
)

fun MeditationInterval.toEntity(): MeditationIntervalEntity = MeditationIntervalEntity(
    id = id,
    sessionId = sessionId,
    startedElapsedRealtimeMs = startedElapsedRealtimeMs,
    endedElapsedRealtimeMs = endedElapsedRealtimeMs,
    startedWallClockMs = startedWallClockMs,
    endedWallClockMs = endedWallClockMs
)

fun SessionInterruptionEventEntity.toDomain(): SessionInterruptionEvent = SessionInterruptionEvent(
    id = id,
    sessionId = sessionId,
    type = InterruptionType.fromWire(type) ?: InterruptionType.UNKNOWN,
    occurredAtEpochMs = occurredAtEpochMs,
    note = note
)

fun SessionInterruptionEvent.toEntity(): SessionInterruptionEventEntity =
    SessionInterruptionEventEntity(
        id = id,
        sessionId = sessionId,
        type = type.name,
        occurredAtEpochMs = occurredAtEpochMs,
        note = note
    )

fun MeditationInsightSnapshotEntity.toDomain(): MeditationInsightSnapshot = MeditationInsightSnapshot(
    id = id,
    rhythmicDayId = rhythmicDayId,
    meditationMinutes = meditationMinutes,
    completedMorningSession = completedMorningSession,
    completedEveningSession = completedEveningSession,
    restorativeMeditationCount = restorativeMeditationCount,
    createdAtEpochMs = createdAtEpochMs
)

fun MeditationInsightSnapshot.toEntity(): MeditationInsightSnapshotEntity =
    MeditationInsightSnapshotEntity(
        id = id,
        rhythmicDayId = rhythmicDayId,
        meditationMinutes = meditationMinutes,
        completedMorningSession = completedMorningSession,
        completedEveningSession = completedEveningSession,
        restorativeMeditationCount = restorativeMeditationCount,
        createdAtEpochMs = createdAtEpochMs
    )

fun EveningMeditationEntity.toDomain(): EveningMeditationRecord = EveningMeditationRecord(
    attentionDayId = attentionDayId,
    state = EveningMeditationState.fromWire(state) ?: EveningMeditationState.NOT_DUE,
    dueAtEpochMs = dueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    sessionId = sessionId,
    updatedAtEpochMs = updatedAtEpochMs
)

fun EveningMeditationRecord.toEntity(): EveningMeditationEntity = EveningMeditationEntity(
    attentionDayId = attentionDayId,
    state = state.name,
    dueAtEpochMs = dueAtEpochMs,
    snoozedUntilEpochMs = snoozedUntilEpochMs,
    sessionId = sessionId,
    updatedAtEpochMs = updatedAtEpochMs
)
