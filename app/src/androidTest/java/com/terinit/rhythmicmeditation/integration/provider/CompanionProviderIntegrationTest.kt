package com.terinit.rhythmicmeditation.integration.provider

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProtocol
import com.terinit.rhythmicmeditation.integration.intent.RoutineAppLauncher
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Read-only device checks for the two companion projections consumed by Insights. */
@RunWith(AndroidJUnit4::class)
class CompanionProviderIntegrationTest {

    private val contentResolver
        get() = InstrumentationRegistry.getInstrumentation().targetContext.contentResolver

    @Test
    fun routineLauncherIsVisibleToMeditation() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = RoutineAppLauncher.findLaunchIntent(context.packageManager)

        assertNotNull("Installed Routine build should have a visible launcher", intent)
        assertTrue(
            "Resolved launcher must target a supported Routine package",
            RoutineAppLauncher.supportedPackages.any { it == intent?.component?.packageName }
        )
    }

    @Test
    fun readerDailyEvidenceV2IsAvailableToMeditation() {
        val dateKey = LocalDate.now().toString()
        val columns = listOf(
            "protocolVersion",
            "dateKey",
            "verifiedActiveSeconds",
            "qualifiedPages"
        )
        val uri = Uri.parse(
            "content://com.terinit.rhythmicreader.evidence/daily/$dateKey"
        )
        val cursor = contentResolver.query(uri, columns.toTypedArray(), null, null, null)

        assertNotNull("Reader Daily Evidence V2 provider should be reachable", cursor)
        cursor!!.use {
            assertEquals(columns, it.columnNames.toList())
            if (it.moveToFirst()) {
                assertEquals(2, it.getInt(it.getColumnIndexOrThrow("protocolVersion")))
                assertEquals(dateKey, it.getString(it.getColumnIndexOrThrow("dateKey")))
                assertTrue(it.getInt(it.getColumnIndexOrThrow("verifiedActiveSeconds")) >= 0)
                assertTrue(it.getInt(it.getColumnIndexOrThrow("qualifiedPages")) >= 0)
            }
        }
    }

    @Test
    fun routineAttentionInsightProjectionIsReachableAndReadOnly() {
        val columns = AttentionInsightProtocol.COLUMNS.toTypedArray()
        var cursor: android.database.Cursor? = null

        for (authority in AttentionInsightProtocol.AUTHORITIES) {
            cursor = runCatching {
                contentResolver.query(
                    Uri.parse("content://$authority/attention-day/integration-probe"),
                    columns,
                    null,
                    null,
                    null
                )
            }.getOrNull()
            if (cursor != null) break
        }

        assertNotNull("Routine attention-insight provider should be reachable", cursor)
        cursor!!.use {
            assertEquals(columns.toList(), it.columnNames.toList())
            // The probe id is intentionally unknown: querying is side-effect-free
            // and must not disclose another day's policy projection.
            assertFalse(it.moveToFirst())
        }
    }
}
