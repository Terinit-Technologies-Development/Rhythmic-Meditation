package com.terinit.rhythmicmeditation.domain.session

import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus

/**
 * Legal session status transitions.
 *
 * Terminal states (COMPLETED / CANCELLED / EXPIRED / INVALID) never transition.
 * All lifecycle mutations must be checked against this table before being
 * persisted — see MeditationSessionService.
 */
object MeditationSessionStateMachine {

    private val transitions: Map<MeditationSessionStatus, Set<MeditationSessionStatus>> =
        mapOf(
            MeditationSessionStatus.PENDING to setOf(
                MeditationSessionStatus.ACTIVE,
                MeditationSessionStatus.CANCELLED,
                MeditationSessionStatus.INVALID,
                MeditationSessionStatus.EXPIRED
            ),
            MeditationSessionStatus.ACTIVE to setOf(
                MeditationSessionStatus.PAUSED,
                MeditationSessionStatus.COMPLETED,
                MeditationSessionStatus.CANCELLED,
                MeditationSessionStatus.EXPIRED
            ),
            MeditationSessionStatus.PAUSED to setOf(
                MeditationSessionStatus.ACTIVE,
                MeditationSessionStatus.CANCELLED,
                MeditationSessionStatus.EXPIRED
            )
        )

    /** True when moving [from] -> [to] is allowed. */
    fun canTransition(
        from: MeditationSessionStatus,
        to: MeditationSessionStatus
    ): Boolean = transitions[from]?.contains(to) == true

    /** All states reachable from [from]. Empty for terminal states. */
    fun allowedTargets(from: MeditationSessionStatus): Set<MeditationSessionStatus> =
        transitions[from].orEmpty()

    /** True when no further transitions are possible. */
    fun isTerminal(status: MeditationSessionStatus): Boolean =
        allowedTargets(status).isEmpty()
}
