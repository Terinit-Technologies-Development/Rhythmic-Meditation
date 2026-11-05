package com.terinit.rhythmicmeditation.integration.contract

import android.content.ContentResolver
import android.net.Uri

/**
 * [AttentionInsightClient] backed by the platform ContentResolver.
 *
 * Fail-open by contract: an absent Routine, a denied provider, an empty
 * response, or any malformed row never crashes the app, affects policy, or
 * blocks meditation. The additive typed queries preserve *why* a projection
 * was unavailable so Insights can show actionable state instead of a blanket
 * "not connected".
 *
 * Two read modes are intentionally distinct:
 * - exact-ID queries keep V1 exact-match semantics;
 * - [currentAttentionDayResult] uses the coordinated additive
 *   `attention-day/current` selector and accepts Routine's opaque real current
 *   identity. It never guesses a calendar day or creates a gate.
 */
class ContentResolverAttentionInsightClient(
    private val contentResolver: ContentResolver
) : AttentionInsightClient {

    override fun attentionDay(attentionDayId: String): AttentionInsightProjection? =
        (attentionDayResult(attentionDayId) as? AttentionInsightQueryResult.Connected)
            ?.projection

    override fun currentAttentionDay(): AttentionInsightProjection? =
        (currentAttentionDayResult() as? AttentionInsightQueryResult.Connected)
            ?.projection

    override fun attentionDayResult(attentionDayId: String): AttentionInsightQueryResult =
        mostInformativeAttentionInsightResult(
            AttentionInsightProtocol.AUTHORITIES.map { authority ->
                query(
                    authority = authority,
                    pathValue = attentionDayId,
                    current = false
                )
            }
        )

    override fun currentAttentionDayResult(): AttentionInsightQueryResult =
        mostInformativeAttentionInsightResult(
            AttentionInsightProtocol.AUTHORITIES.map { authority ->
                query(
                    authority = authority,
                    pathValue = AttentionInsightProtocol.CURRENT_SELECTOR,
                    current = true
                )
            }
        )

    private fun query(
        authority: String,
        pathValue: String,
        current: Boolean
    ): AttentionInsightQueryResult {
        val uri = Uri.parse(
            "content://$authority/${AttentionInsightProtocol.PATH_PREFIX}/$pathValue"
        )
        return try {
            val cursor = contentResolver.query(
                uri,
                AttentionInsightProtocol.COLUMNS.toTypedArray(),
                null,
                null,
                null
            )
            if (cursor == null) {
                classifyUnavailable(authority, pathValue, current)
            } else {
                cursor.use { open ->
                    if (!open.moveToFirst()) {
                        unavailable(
                            reason = if (current) {
                                AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE
                            } else {
                                AttentionInsightUnavailableReason.EMPTY_CURRENT_DAY
                            },
                            authority = authority,
                            pathValue = pathValue
                        )
                    } else {
                        val values = AttentionInsightProtocol.COLUMNS.associateWith { column ->
                            val index = open.getColumnIndex(column)
                            if (index < 0) null else open.getString(index)
                        }
                        val result = if (current) {
                            AttentionInsightProtocol.interpretCurrentRow(values)
                        } else {
                            AttentionInsightProtocol.interpretRow(
                                expectedAttentionDayId = pathValue,
                                values = values
                            )
                        }
                        when (result) {
                            is AttentionInsightQueryResult.Connected -> result
                            is AttentionInsightQueryResult.Unavailable ->
                                result.copy(authority = authority)
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            unavailable(
                reason = AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED,
                authority = authority,
                pathValue = pathValue
            )
        } catch (e: IllegalArgumentException) {
            if (current) {
                // An older Routine provider may reject the additive selector
                // as an unknown URL. Preserve that as a real compatibility
                // limitation, not "not installed".
                classifyUnavailable(authority, pathValue, current = true)
            } else {
                unavailable(
                    reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
                    authority = authority,
                    pathValue = pathValue
                )
            }
        } catch (e: Exception) {
            // Never surface exception text or stack traces across the boundary.
            unavailable(
                reason = AttentionInsightUnavailableReason.TRANSPORT_ERROR,
                authority = authority,
                pathValue = pathValue
            )
        }
    }

    /**
     * A null cursor (or a rejected unknown selector) alone cannot tell "not
     * installed" from "present but unsupported" or "denied". Probe the
     * authority to classify it honestly without exposing exception detail.
     */
    private fun classifyUnavailable(
        authority: String,
        pathValue: String,
        current: Boolean
    ): AttentionInsightQueryResult {
        val client = try {
            contentResolver.acquireContentProviderClient(authority)
        } catch (e: SecurityException) {
            return unavailable(
                reason = AttentionInsightUnavailableReason.SIGNATURE_OR_PERMISSION_DENIED,
                authority = authority,
                pathValue = pathValue
            )
        } catch (e: IllegalArgumentException) {
            return unavailable(
                reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
                authority = authority,
                pathValue = pathValue
            )
        } catch (e: Exception) {
            return unavailable(
                reason = AttentionInsightUnavailableReason.TRANSPORT_ERROR,
                authority = authority,
                pathValue = pathValue
            )
        }
        return if (client == null) {
            unavailable(
                reason = AttentionInsightUnavailableReason.PROVIDER_NOT_INSTALLED,
                authority = authority,
                pathValue = pathValue
            )
        } else {
            client.close()
            unavailable(
                reason = if (current) {
                    AttentionInsightUnavailableReason.CURRENT_PROJECTION_UNAVAILABLE
                } else {
                    AttentionInsightUnavailableReason.NULL_CURSOR
                },
                authority = authority,
                pathValue = pathValue
            )
        }
    }

    private fun unavailable(
        reason: AttentionInsightUnavailableReason,
        authority: String,
        pathValue: String
    ): AttentionInsightQueryResult.Unavailable = AttentionInsightQueryResult.Unavailable(
        reason = reason,
        authority = authority,
        expectedAttentionDayId = pathValue
    )
}
