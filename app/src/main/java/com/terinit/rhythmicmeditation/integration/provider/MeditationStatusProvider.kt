package com.terinit.rhythmicmeditation.integration.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import kotlinx.coroutines.runBlocking

/**
 * Status provider for Rhythmic Routine (authority
 * [MeditationProtocol.STATUS_PROVIDER_AUTHORITY]).
 *
 * PASS 1 SHELL: read-only exposure of session status. Write semantics, richer
 * query support, and full IPC hardening land in a later pass. The provider is
 * exported behind a signature permission and additionally verified through
 * [com.terinit.rhythmicmeditation.integration.contract.CallerVerifier].
 */
class MeditationStatusProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val app = context?.applicationContext as? RhythmicMeditationApp ?: return null
        val sessionId = selectionArgs?.firstOrNull() ?: uri.lastPathSegment
        if (sessionId.isNullOrBlank()) return null

        val status = runBlocking {
            app.container.meditationStatusRepository.getSessionStatus(sessionId)
        } ?: return null

        val columns = arrayOf(
            MeditationProtocol.StatusColumns.SESSION_ID,
            MeditationProtocol.StatusColumns.PROTOCOL_VERSION,
            MeditationProtocol.StatusColumns.STATUS,
            MeditationProtocol.StatusColumns.REQUIRED_QUALIFIED_SECONDS,
            MeditationProtocol.StatusColumns.COMPLETED_QUALIFIED_SECONDS,
            MeditationProtocol.StatusColumns.COMPLETED_AT_EPOCH_MS,
            MeditationProtocol.StatusColumns.LAST_UPDATED_AT_EPOCH_MS
        )
        val cursor = MatrixCursor(columns)
        cursor.addRow(
            arrayOf<Any?>(
                status.sessionId,
                status.protocolVersion,
                status.status,
                status.requiredQualifiedSeconds,
                status.completedQualifiedSeconds,
                status.completedAtEpochMs,
                status.lastUpdatedAtEpochMs
            )
        )
        return cursor
    }

    override fun getType(uri: Uri): String =
        "vnd.android.cursor.item/vnd.com.terinit.rhythmicmeditation.status"

    // Write operations are intentionally unsupported in Pass 1.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
