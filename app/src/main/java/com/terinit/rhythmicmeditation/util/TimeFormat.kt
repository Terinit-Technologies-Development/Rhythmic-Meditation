package com.terinit.rhythmicmeditation.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Formatting helpers for the calm UI. Pure functions; unit testable.
 */
object TimeFormat {

    /** "12:48" style for session timers (supports > 60 min as "62:07"). */
    fun mmSs(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val minutes = safe / 60
        val seconds = safe % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    /** "12:48" style from milliseconds. */
    fun mmSsFromMillis(totalMillis: Long): String = mmSs((totalMillis / 1000L).toInt())

    /** "30:00" style for session requirements. */
    fun mmSsDuration(seconds: Int): String = mmSs(seconds)

    /** Compact duration like "1 hr 32 min" or "45 min". */
    fun humanMinutes(totalSeconds: Int): String {
        val safe = totalSeconds.coerceAtLeast(0)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "$hours hr $minutes min"
            hours > 0 -> "$hours hr"
            minutes > 0 -> "$minutes min"
            else -> "less than a minute"
        }
    }

    /** Whole minutes from seconds, rounded down (insight display). */
    fun wholeMinutes(totalSeconds: Int): Int = (totalSeconds.coerceAtLeast(0)) / 60

    /** "9:15 PM" style clock time for snooze/deferral copy (display only). */
    fun timeOfDay(epochMs: Long): String =
        SimpleDateFormat("h:mm a", Locale.US).format(Date(epochMs))
}
