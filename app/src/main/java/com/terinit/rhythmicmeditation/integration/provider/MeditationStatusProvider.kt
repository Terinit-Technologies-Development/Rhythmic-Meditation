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
 * Exposes ONLY session status evidence — the columns listed in
 * [MeditationProtocol.StatusColumns]. No private app history is reachable
 * through this endpoint.
 *
 * Access control is layered:
 * 1. the manifest signature permission, and
 * 2. [com.terinit.rhythmicmeditation.integration.contract.CallerVerifier]
 *    (deny-by-default package + signing-certificate verification).
 *
 * PASS 2 SHELL: read-only. Write semantics and richer queries land with the
 * full Routine integration (Pass 3).
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
        val container = app.container

        // Deny-by-default caller verification. Never bypassed.
        val caller = container.callerIdentityResolver.identityOf(callingPackage)
        container.callerVerifier.verifyCaller(
            callingPackage = caller.packageName,
            callingSigningCertificateDigests = caller.signingCertificateDigests
        ).getOrElse { return null }

        val sessionId = selectionArgs?.firstOrNull() ?: uri.lastPathSegment
        if (sessionId.isNullOrBlank()) return null

        val status = runBlocking {
            container.meditationStatusRepository.getSessionStatus(sessionId)
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

    // Write operations are intentionally unsupported.
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
