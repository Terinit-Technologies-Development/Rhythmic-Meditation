package com.terinit.rhythmicmeditation.data

import android.content.Context
import com.terinit.rhythmicmeditation.data.local.db.MeditationDatabase
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import com.terinit.rhythmicmeditation.data.repository.MeditationInsightsRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationIntervalRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationInsightsRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationIntervalRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.RoomSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.data.repository.SessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.domain.timing.SystemTimeProvider
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.integration.contract.CallerTrustPolicy
import com.terinit.rhythmicmeditation.integration.contract.CallerVerifier
import com.terinit.rhythmicmeditation.integration.contract.LocalMeditationStatusRepository
import com.terinit.rhythmicmeditation.integration.contract.MeditationStatusRepository

/**
 * Manual dependency container.
 *
 * Intentionally explicit (no Hilt/Koin): the graph is small, and plain wiring
 * keeps the architecture narrow and easy to test. Exposed on the Application
 * ([com.terinit.rhythmicmeditation.app.RhythmicMeditationApp.container]).
 */
class AppContainer(context: Context) {

    val timeProvider: TimeProvider = SystemTimeProvider()

    val preferencesStore = AppPreferencesStore(context.applicationContext)

    private val database: MeditationDatabase = MeditationDatabase.getInstance(context)

    val sessionRepository: MeditationSessionRepository =
        RoomMeditationSessionRepository(database.meditationSessionDao())

    val intervalRepository: MeditationIntervalRepository =
        RoomMeditationIntervalRepository(database.meditationIntervalDao())

    val interruptionRepository: SessionInterruptionEventRepository =
        RoomSessionInterruptionEventRepository(database.sessionInterruptionEventDao())

    val insightsRepository: MeditationInsightsRepository =
        RoomMeditationInsightsRepository(database.meditationInsightSnapshotDao())

    val sessionService: MeditationSessionService = MeditationSessionService(
        sessionRepository = sessionRepository,
        intervalRepository = intervalRepository,
        interruptionRepository = interruptionRepository,
        timeProvider = timeProvider
    )

    /**
     * Trust policy for IPC callers: the peer identity comes from the paired
     * package setting. Signature digests are configured when Routine pairing
     * is completed (later pass); until then every external caller is denied.
     */
    private val callerTrustPolicy = object : CallerTrustPolicy {
        override fun expectedPackageName(): String? = null // set from preferences when pairing lands
        override fun expectedSigningCertificateDigests(): Set<String> = emptySet()
    }

    val callerVerifier = CallerVerifier(callerTrustPolicy)

    val meditationStatusRepository: MeditationStatusRepository =
        LocalMeditationStatusRepository(
            sessionService = sessionService,
            sessionRepository = sessionRepository
        )
}
