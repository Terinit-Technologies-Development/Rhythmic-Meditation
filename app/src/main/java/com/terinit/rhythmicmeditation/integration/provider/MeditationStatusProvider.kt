package com.terinit.rhythmicmeditation.integration.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.domain.protocol.MeditationProtocol
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus
import kotlinx.coroutines.runBlocking

/**
 * Pure projection description for one status row.
 *
 * Kept separate from [MatrixCursor] so column selection and identity values
 * are unit-testable without Android infrastructure. A null [row] means the
 * session is unknown, while [columns] still advertises the requested schema —
 * the discoverability behaviour existing V1 clients rely on.
 */
internal data class MeditationStatusProjection(
    val columns: List<String>,
    val row: List<Any?>?
)

/**
 * Selects a stable projection.
 *
 * - no projection, a legacy projection, or only unknown columns -> the exact
 *   frozen seven-column V1 shape (the pre-metadata behaviour)
 * - any additive identity column requested -> the full ten-column V1 shape
 *
 * The metadata columns are never added implicitly, so a pre-metadata client
 * sees exactly the same seven columns it did before. A metadata-capable client
 * always receives the complete legacy tuple plus all identity fields, which
 * keeps the row self-describing and impossible to half-read.
 */
internal fun meditationStatusProjection(
    status: MeditationRecoveryStatus?,
    projection: Array<out String>?
): MeditationStatusProjection {
    val columns = resolveStatusColumns(projection)
    return MeditationStatusProjection(
        columns = columns,
        row = status?.let { session ->
            columns.map { column -> session.valueFor(column) }
        }
    )
}

private fun resolveStatusColumns(projection: Array<out String>?): List<String> {
    val requestsIdentity = projection
        ?.mapNotNull { it.trim().takeIf { value -> value.isNotEmpty() } }
        ?.any { it in MeditationProtocol.StatusColumns.IDENTITY_COLUMNS }
        ?: false
    return if (requestsIdentity) {
        MeditationProtocol.StatusColumns.ALL_COLUMNS
    } else {
        MeditationProtocol.StatusColumns.LEGACY_COLUMNS
    }
}

private fun MeditationRecoveryStatus.valueFor(column: String): Any? = when (column) {
    MeditationProtocol.StatusColumns.SESSION_ID -> sessionId
    MeditationProtocol.StatusColumns.PROTOCOL_VERSION -> protocolVersion
    MeditationProtocol.StatusColumns.STATUS -> status
    MeditationProtocol.StatusColumns.REQUIRED_QUALIFIED_SECONDS -> requiredQualifiedSeconds
    MeditationProtocol.StatusColumns.COMPLETED_QUALIFIED_SECONDS -> completedQualifiedSeconds
    MeditationProtocol.StatusColumns.COMPLETED_AT_EPOCH_MS -> completedAtEpochMs
    MeditationProtocol.StatusColumns.LAST_UPDATED_AT_EPOCH_MS -> lastUpdatedAtEpochMs
    MeditationProtocol.StatusColumns.SESSION_KIND -> sessionKind
    MeditationProtocol.StatusColumns.SOURCE_COOLDOWN_ID -> sourceCooldownId
    MeditationProtocol.StatusColumns.SOURCE_RHYTHMIC_DAY_ID -> sourceRhythmicDayId
    else -> null
}

/**
 * Keeps the V1 schema discoverable even when a requested session is missing.
 * Metadata columns appear only when explicitly requested; see
 * [meditationStatusProjection].
 */
internal fun meditationStatusCursor(
    status: MeditationRecoveryStatus?,
    projection: Array<out String>? = null
): MatrixCursor {
    val projected = meditationStatusProjection(status, projection)
    return MatrixCursor(projected.columns.toTypedArray()).apply {
        projected.row?.let { addRow(it.toTypedArray()) }
    }
}

/**
 * Status provider for Rhythmic Routine (authority
 * [MeditationProtocol.STATUS_PROVIDER_AUTHORITY]).
 *
 * Exposes ONLY session status evidence — the frozen seven columns listed in
 * [MeditationProtocol.StatusColumns.LEGACY_COLUMNS] plus the additive identity
 * columns when requested. No private app history is reachable through this
 * endpoint.
 *
 * Access control is layered:
 * 1. the manifest signature permission, and
 * 2. [com.terinit.rhythmicmeditation.integration.contract.CallerVerifier]
 *    (deny-by-default package + signing-certificate verification).
 *
 * Read-only by design: Routine can query evidence for a specific session id;
 * it cannot write or enumerate Meditation's private history.
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
        }

        // The unknown-id probe uses this empty cursor to discover protocol V1.
        // A null cursor means the provider itself is unavailable or rejected
        // the caller, so do not conflate a missing session with that condition.
        return meditationStatusCursor(status, projection)
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
