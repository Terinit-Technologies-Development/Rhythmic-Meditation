package com.terinit.rhythmicmeditation.domain.timing

/**
 * Injectable time source.
 *
 * All session timing must go through this abstraction:
 * - [elapsedRealtimeMillis] is monotonic and authoritative for qualified-time
 *   computation and tamper resistance.
 * - [currentTimeMillis] is wall-clock and is used only for human-readable
 *   timestamps and diagnostics.
 *
 * Never qualify meditation time from wall-clock time alone.
 */
interface TimeProvider {
    /** Wall-clock time (epoch ms). Diagnostics only. */
    fun currentTimeMillis(): Long

    /** Monotonic time since boot (ms). Authoritative for qualification. */
    fun elapsedRealtimeMillis(): Long
}

/**
 * Production implementation backed by the platform clocks.
 */
class SystemTimeProvider : TimeProvider {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()

    override fun elapsedRealtimeMillis(): Long = android.os.SystemClock.elapsedRealtime()
}
