package com.terinit.rhythmicmeditation.integration.contract

import android.content.ContentResolver
import android.net.Uri

/**
 * Android implementation of [RoutineEveningSignal] over Rhythmic Routine's
 * narrow, signature-protected evening projection
 * (`content://com.terinit.rhythmicroutine[.qa].evening-signal/current`).
 *
 * Defensive and fail-safe by design: unknown protocol versions, malformed
 * rows, or an absent Routine all resolve to "no signal" — which simply means
 * the Evening practice is NOT due. An unavailable optional projection never
 * blocks meditation and never fabricates an obligation.
 */
class ContentResolverRoutineEveningSignal(
    private val contentResolver: ContentResolver,
    private val authorities: List<String> = AUTHORITIES
) : RoutineEveningSignal {

    override fun currentEveningSignal(): EveningSignal? {
        for (authority in authorities) {
            val cursor = try {
                contentResolver.query(
                    Uri.Builder()
                        .scheme("content")
                        .authority(authority)
                        .appendPath(PATH_CURRENT)
                        .build(),
                    ALL_COLUMNS,
                    null,
                    null,
                    null
                )
            } catch (_: RuntimeException) {
                null
            } ?: continue

            cursor.use {
                if (!it.moveToFirst()) return@use
                val signal = parseSignal(it) ?: return@use
                return signal
            }
        }
        return null
    }

    private fun parseSignal(cursor: android.database.Cursor): EveningSignal? {
        val protocolIndex = cursor.getColumnIndex(COLUMN_PROTOCOL_VERSION)
        val dayIndex = cursor.getColumnIndex(COLUMN_ATTENTION_DAY_ID)
        val dueIndex = cursor.getColumnIndex(COLUMN_DUE_AT_EPOCH_MS)
        val transitionIndex = cursor.getColumnIndex(COLUMN_TRANSITION_AT_EPOCH_MS)
        if (protocolIndex < 0 || dayIndex < 0 || dueIndex < 0 || transitionIndex < 0) return null

        val protocolVersion = cursor.getInt(protocolIndex)
        if (protocolVersion != PROTOCOL_VERSION) return null

        val attentionDayId = cursor.getString(dayIndex) ?: return null
        if (attentionDayId.isBlank()) return null
        val dueAt = cursor.getLong(dueIndex)
        val transitionAt = cursor.getLong(transitionIndex)
        if (dueAt <= 0L || transitionAt <= dueAt) return null

        return EveningSignal(
            attentionDayId = attentionDayId,
            dueAtEpochMs = dueAt,
            transitionAtEpochMs = transitionAt
        )
    }

    companion object {
        const val PROTOCOL_VERSION = 1
        const val PATH_CURRENT = "current"

        const val COLUMN_PROTOCOL_VERSION = "protocolVersion"
        const val COLUMN_ATTENTION_DAY_ID = "attentionDayId"
        const val COLUMN_DUE_AT_EPOCH_MS = "dueAtEpochMs"
        const val COLUMN_TRANSITION_AT_EPOCH_MS = "transitionAtEpochMs"
        const val COLUMN_STATE = "state"

        val ALL_COLUMNS = arrayOf(
            COLUMN_PROTOCOL_VERSION,
            COLUMN_ATTENTION_DAY_ID,
            COLUMN_DUE_AT_EPOCH_MS,
            COLUMN_TRANSITION_AT_EPOCH_MS,
            COLUMN_STATE
        )

        val AUTHORITIES = listOf(
            "com.terinit.rhythmicroutine.evening-signal",
            "com.terinit.rhythmicroutine.qa.evening-signal"
        )
    }
}
