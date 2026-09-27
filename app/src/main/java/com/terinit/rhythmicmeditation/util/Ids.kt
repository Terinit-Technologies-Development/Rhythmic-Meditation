package com.terinit.rhythmicmeditation.util

import java.util.UUID

/**
 * Identifier helpers. Session ids are opaque strings shared with Rhythmic
 * Routine; locally generated sessions use random UUIDs.
 */
object Ids {
    fun newSessionId(): String = UUID.randomUUID().toString()
}
