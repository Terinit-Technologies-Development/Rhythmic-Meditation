package com.terinit.rhythmicmeditation.runtime

/**
 * Screen interactivity is the boundary that decides whether meditation time
 * keeps qualifying:
 *
 * - screen off (isInteractive == false): the phone is put down — KEEP QUALIFYING
 * - screen on + meditation not in foreground: the user is using another app —
 *   CLOSE INTERVAL and PAUSE
 */
interface ScreenStateReader {
    fun isInteractive(): Boolean
}

/**
 * Production implementation backed by [android.os.PowerManager].
 */
class SystemScreenStateReader(
    private val powerManager: android.os.PowerManager
) : ScreenStateReader {
    override fun isInteractive(): Boolean = powerManager.isInteractive
}
