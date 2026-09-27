package com.terinit.rhythmicmeditation.runtime

import android.content.Context
import android.provider.Settings

/**
 * Optional secondary reboot guard.
 *
 * `SystemClock.elapsedRealtime()` regression is the primary reboot signal (it
 * needs no permission at all). `Settings.Global.BOOT_COUNT` is readable without
 * any additional permission and is used as a corroborating guard when the
 * device exposes it.
 */
interface BootIdentityReader {
    /** Monotonic-ish boot counter, or null when unavailable. */
    fun bootCount(): Int?
}

class SystemBootIdentityReader(private val context: Context) : BootIdentityReader {
    override fun bootCount(): Int? = try {
        Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT)
    } catch (_: Throwable) {
        null
    }
}
