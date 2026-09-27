package com.terinit.rhythmicmeditation.domain.protocol

/**
 * Protocol constants for Routine <-> Meditation coordination.
 *
 * Rhythmic Routine is the policy authority; Meditation only exposes evidence
 * about meditation sessions through this contract.
 */
object MeditationProtocol {
    const val PROTOCOL_VERSION = 1

    const val ACTION_START_MEDITATION_RECOVERY =
        "com.terinit.rhythmicmeditation.action.START_MEDITATION_RECOVERY"

    const val EXTRA_REQUEST_PAYLOAD = "extra_request_payload"

    const val STATUS_PROVIDER_AUTHORITY =
        "com.terinit.rhythmicmeditation.status"

    /** Column names exposed by the status provider. */
    object StatusColumns {
        const val SESSION_ID = "sessionId"
        const val PROTOCOL_VERSION = "protocolVersion"
        const val STATUS = "status"
        const val REQUIRED_QUALIFIED_SECONDS = "requiredQualifiedSeconds"
        const val COMPLETED_QUALIFIED_SECONDS = "completedQualifiedSeconds"
        const val COMPLETED_AT_EPOCH_MS = "completedAtEpochMs"
        const val LAST_UPDATED_AT_EPOCH_MS = "lastUpdatedAtEpochMs"
    }
}
