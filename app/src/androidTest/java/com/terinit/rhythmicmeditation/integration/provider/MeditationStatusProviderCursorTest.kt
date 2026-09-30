package com.terinit.rhythmicmeditation.integration.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.terinit.rhythmicmeditation.domain.protocol.MeditationRecoveryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeditationStatusProviderCursorTest {

    @Test
    fun missingSessionReturnsAnEmptyCursorWithTheProtocolSchema() {
        meditationStatusCursor(null).use { cursor ->
            assertEquals(
                listOf(
                    "sessionId",
                    "protocolVersion",
                    "status",
                    "requiredQualifiedSeconds",
                    "completedQualifiedSeconds",
                    "completedAtEpochMs",
                    "lastUpdatedAtEpochMs",
                ),
                cursor.columnNames.toList(),
            )
            assertFalse(cursor.moveToFirst())
        }
    }

    @Test
    fun existingSessionReturnsOneEvidenceRow() {
        val status = MeditationRecoveryStatus(
            sessionId = "bound-session",
            protocolVersion = 1,
            status = "COMPLETED",
            requiredQualifiedSeconds = 1800,
            completedQualifiedSeconds = 1800,
            completedAtEpochMs = 1_790_000_000_000L,
            lastUpdatedAtEpochMs = 1_790_000_000_000L,
        )

        meditationStatusCursor(status).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("bound-session", cursor.getString(0))
            assertEquals("COMPLETED", cursor.getString(2))
            assertEquals(1800, cursor.getInt(4))
            assertFalse(cursor.moveToNext())
        }
    }
}
