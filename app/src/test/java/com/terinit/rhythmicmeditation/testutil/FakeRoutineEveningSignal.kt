package com.terinit.rhythmicmeditation.testutil

import com.terinit.rhythmicmeditation.integration.contract.EveningSignal
import com.terinit.rhythmicmeditation.integration.contract.RoutineEveningSignal

/**
 * In-memory [RoutineEveningSignal]: set [signal] to simulate a connected
 * Routine; leave it null for standalone behavior.
 */
class FakeRoutineEveningSignal(
    var signal: EveningSignal? = null
) : RoutineEveningSignal {

    override fun currentEveningSignal(): EveningSignal? = signal
}
