package com.terinit.rhythmicmeditation.data.mapper

import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus

/**
 * Domain <-> protocol contract mapping.
 */

/**
 * Best-effort "last updated" timestamp from known session timestamps.
 * Wall clock is acceptable here: this value is diagnostic for Routine.
 */
fun MeditationSession.lastUpdatedAtEpochMs(): Long =
    listOfNotNull(createdAtEpochMs, startedAtEpochMs, completedAtEpochMs).max()

fun MeditationSession.toRecoveryStatus(): MeditationRecoveryStatus = MeditationRecoveryStatus(
    sessionId = sessionId,
    protocolVersion = protocolVersion,
    status = status.name,
    requiredQualifiedSeconds = requiredSeconds,
    completedQualifiedSeconds = completedQualifiedSeconds,
    completedAtEpochMs = completedAtEpochMs,
    lastUpdatedAtEpochMs = lastUpdatedAtEpochMs()
)
