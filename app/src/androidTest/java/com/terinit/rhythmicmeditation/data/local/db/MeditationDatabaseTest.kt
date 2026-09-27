package com.terinit.rhythmicmeditation.data.local.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.terinit.rhythmicmeditation.data.local.entity.MeditationIntervalEntity
import com.terinit.rhythmicmeditation.data.local.entity.MeditationSessionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Room roundtrip tests (run with: ./gradlew connectedAndroidTest).
 *
 * Covers insert/read, interval closing, qualified-seconds accumulation, and
 * session replacement semantics at the DAO level.
 */
@RunWith(AndroidJUnit4::class)
class MeditationDatabaseTest {

    private lateinit var database: MeditationDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MeditationDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun sessionEntity(
        sessionId: String = "session-1",
        status: String = "PENDING",
        completedQualifiedSeconds: Int = 0
    ) = MeditationSessionEntity(
        sessionId = sessionId,
        protocolVersion = 1,
        kind = "MORNING_REQUIRED",
        status = status,
        requiredSeconds = 1800,
        completedQualifiedSeconds = completedQualifiedSeconds,
        startedAtEpochMs = null,
        completedAtEpochMs = null,
        createdAtEpochMs = 1_700_000_000_000L,
        expiresAtEpochMs = null,
        sourceCooldownId = null,
        sourceRhythmicDayId = "day-1",
        interruptionCount = 0,
        pauseCount = 0
    )

    @Test
    fun sessionInsertAndReadRoundTrip() = runTest {
        val dao = database.meditationSessionDao()
        dao.upsert(sessionEntity())

        val loaded = dao.getById("session-1")
        assertNotNull(loaded)
        assertEquals("MORNING_REQUIRED", loaded!!.kind)
        assertEquals("PENDING", loaded.status)
        assertEquals(1800, loaded.requiredSeconds)
    }

    @Test
    fun sessionUpsertWithSameIdReplaces() = runTest {
        val dao = database.meditationSessionDao()
        dao.upsert(sessionEntity(status = "PENDING", completedQualifiedSeconds = 0))
        dao.upsert(sessionEntity(status = "ACTIVE", completedQualifiedSeconds = 42))

        val loaded = dao.getById("session-1")
        assertEquals("ACTIVE", loaded!!.status)
        assertEquals(42, loaded.completedQualifiedSeconds)
    }

    @Test
    fun observeCurrentSessionTracksActiveLifecycle() = runTest {
        val dao = database.meditationSessionDao()
        dao.upsert(sessionEntity())

        val current = dao.observeCurrentSession().first()
        assertEquals("session-1", current!!.sessionId)

        dao.updateStatus("session-1", "COMPLETED")
        assertNull(dao.observeCurrentSession().first())
    }

    @Test
    fun addQualifiedSecondsAccumulates() = runTest {
        val dao = database.meditationSessionDao()
        dao.upsert(sessionEntity())
        dao.addQualifiedSeconds("session-1", 30)
        dao.addQualifiedSeconds("session-1", 25)

        assertEquals(55, dao.getById("session-1")!!.completedQualifiedSeconds)
    }

    @Test
    fun intervalCloseSemantics() = runTest {
        val sessionDao = database.meditationSessionDao()
        val intervalDao = database.meditationIntervalDao()
        sessionDao.upsert(sessionEntity())

        intervalDao.insert(
            MeditationIntervalEntity(
                sessionId = "session-1",
                startedElapsedRealtimeMs = 1_000,
                endedElapsedRealtimeMs = null,
                startedWallClockMs = 1_700_000_000_000L,
                endedWallClockMs = null
            )
        )

        val open = intervalDao.getOpenInterval("session-1")
        assertNotNull(open)

        intervalDao.closeOpenIntervals(
            sessionId = "session-1",
            endedElapsedMs = 31_000,
            endedWallClockMs = 1_700_000_030_000L
        )

        assertNull(intervalDao.getOpenInterval("session-1"))
        val closed = intervalDao.getForSession("session-1").first()
        assertEquals(31_000L, closed.endedElapsedRealtimeMs)
        assertEquals(1_700_000_030_000L, closed.endedWallClockMs)

        // Closing again is a safe no-op.
        intervalDao.closeOpenIntervals("session-1", 99_000, 1_700_000_099_000L)
        assertEquals(31_000L, intervalDao.getForSession("session-1").first().endedElapsedRealtimeMs)
    }

    @Test
    fun intervalsAreScopedToTheirSession() = runTest {
        val sessionDao = database.meditationSessionDao()
        val intervalDao = database.meditationIntervalDao()
        sessionDao.upsert(sessionEntity(sessionId = "session-1"))
        sessionDao.upsert(sessionEntity(sessionId = "session-2"))

        intervalDao.insert(
            MeditationIntervalEntity(
                sessionId = "session-1",
                startedElapsedRealtimeMs = 1_000,
                endedElapsedRealtimeMs = 2_000,
                startedWallClockMs = 1L,
                endedWallClockMs = 2L
            )
        )
        intervalDao.insert(
            MeditationIntervalEntity(
                sessionId = "session-2",
                startedElapsedRealtimeMs = 5_000,
                endedElapsedRealtimeMs = 6_000,
                startedWallClockMs = 5L,
                endedWallClockMs = 6L
            )
        )

        assertEquals(1, intervalDao.getForSession("session-1").size)
        assertEquals(1, intervalDao.getForSession("session-2").size)
        assertTrue(intervalDao.getForSession("session-1").first().startedElapsedRealtimeMs == 1_000L)
    }

    @Test
    fun deletingSessionCascadesToItsIntervals() = runTest {
        val sessionDao = database.meditationSessionDao()
        val intervalDao = database.meditationIntervalDao()
        sessionDao.upsert(sessionEntity())
        intervalDao.insert(
            MeditationIntervalEntity(
                sessionId = "session-1",
                startedElapsedRealtimeMs = 1_000,
                endedElapsedRealtimeMs = null,
                startedWallClockMs = 1L,
                endedWallClockMs = null
            )
        )

        database.openHelper.writableDatabase.execSQL(
            "DELETE FROM meditation_sessions WHERE sessionId = 'session-1'"
        )

        assertTrue(intervalDao.getForSession("session-1").isEmpty())
    }
}
