package com.terinit.rhythmicmeditation.data

import android.content.Context
import com.terinit.rhythmicmeditation.data.local.db.MeditationDatabase
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import com.terinit.rhythmicmeditation.data.repository.EveningMeditationRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationInsightsRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationIntervalRepository
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.RoomEveningMeditationRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationInsightsRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationIntervalRepository
import com.terinit.rhythmicmeditation.data.repository.RoomMeditationSessionRepository
import com.terinit.rhythmicmeditation.data.repository.RoomSessionCheckpointRepository
import com.terinit.rhythmicmeditation.data.repository.RoomSessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.data.repository.SessionCheckpointRepository
import com.terinit.rhythmicmeditation.data.repository.SessionInterruptionEventRepository
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import com.terinit.rhythmicmeditation.domain.timing.SystemTimeProvider
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightClient
import com.terinit.rhythmicmeditation.integration.contract.CallerTrustPolicy
import com.terinit.rhythmicmeditation.integration.contract.CallerVerifier
import com.terinit.rhythmicmeditation.integration.contract.SameSignerCallerTrustPolicy
import com.terinit.rhythmicmeditation.integration.contract.ContentResolverAttentionInsightClient
import com.terinit.rhythmicmeditation.integration.contract.ContentResolverReadingInsightClient
import com.terinit.rhythmicmeditation.integration.contract.LocalMeditationStatusRepository
import com.terinit.rhythmicmeditation.integration.contract.MeditationStatusRepository
import com.terinit.rhythmicmeditation.integration.contract.ReadingInsightClient
import com.terinit.rhythmicmeditation.integration.contract.ContentResolverRoutineEveningSignal
import com.terinit.rhythmicmeditation.integration.contract.RecoveryRequestHandler
import com.terinit.rhythmicmeditation.integration.contract.RoutineEveningSignal
import com.terinit.rhythmicmeditation.integration.contract.SystemCallerIdentityResolver
import com.terinit.rhythmicmeditation.integration.contract.UnavailableRoutineEveningSignal
import com.terinit.rhythmicmeditation.runtime.BootIdentityReader
import com.terinit.rhythmicmeditation.runtime.EveningMeditationController
import com.terinit.rhythmicmeditation.runtime.MeditationRuntimeController
import com.terinit.rhythmicmeditation.runtime.ScreenStateReader
import com.terinit.rhythmicmeditation.runtime.SystemBootIdentityReader
import com.terinit.rhythmicmeditation.runtime.SystemScreenStateReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container.
 *
 * Intentionally explicit (no Hilt/Koin): the graph is small, and plain wiring
 * keeps the architecture narrow and easy to test. Exposed on the Application
 * ([com.terinit.rhythmicmeditation.app.RhythmicMeditationApp.container]).
 */
class AppContainer(context: Context) {

    /** Long-lived application scope for the meditation runtime. */
    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val timeProvider: TimeProvider = SystemTimeProvider()

    val preferencesStore = AppPreferencesStore(context.applicationContext)

    val screenStateReader: ScreenStateReader = SystemScreenStateReader(
        context.applicationContext.getSystemService(Context.POWER_SERVICE)
            as android.os.PowerManager
    )

    val bootIdentityReader: BootIdentityReader =
        SystemBootIdentityReader(context.applicationContext)

    val callerIdentityResolver = SystemCallerIdentityResolver(context.applicationContext)

    private val database: MeditationDatabase = MeditationDatabase.getInstance(context)

    val sessionRepository: MeditationSessionRepository =
        RoomMeditationSessionRepository(database.meditationSessionDao())

    val intervalRepository: MeditationIntervalRepository =
        RoomMeditationIntervalRepository(database.meditationIntervalDao())

    val interruptionRepository: SessionInterruptionEventRepository =
        RoomSessionInterruptionEventRepository(database.sessionInterruptionEventDao())

    val insightsRepository: MeditationInsightsRepository =
        RoomMeditationInsightsRepository(database.meditationInsightSnapshotDao())

    val checkpointRepository: SessionCheckpointRepository =
        RoomSessionCheckpointRepository(database.sessionTimeCheckpointDao())

    val eveningRepository: EveningMeditationRepository =
        RoomEveningMeditationRepository(database.eveningMeditationDao())

    val sessionService: MeditationSessionService = MeditationSessionService(
        sessionRepository = sessionRepository,
        intervalRepository = intervalRepository,
        interruptionRepository = interruptionRepository,
        timeProvider = timeProvider
    )

    /**
     * Peer trust for IPC callers (Pass 3 handoff "final equivalent"): the
     * Routine package(s) are trusted only with signing certificates identical
     * to Meditation's own signer. Deny-by-default — until a companion with our
     * signer calls, every external caller is rejected. No debug bypass.
     */
    private val callerTrustPolicy = SameSignerCallerTrustPolicy(
        ownPackageName = context.applicationContext.packageName,
        identityResolver = SystemCallerIdentityResolver(context.applicationContext)
    )

    val callerVerifier = CallerVerifier(callerTrustPolicy)

    val meditationStatusRepository: MeditationStatusRepository =
        LocalMeditationStatusRepository(
            sessionService = sessionService,
            sessionRepository = sessionRepository
        )

    /**
     * The meditation engine. Owns qualified-time accounting, conservative
     * recovery, and translation of device/lifecycle events.
     */
    val runtimeController: MeditationRuntimeController = MeditationRuntimeController(
        sessionService = sessionService,
        sessionRepository = sessionRepository,
        checkpointRepository = checkpointRepository,
        timeProvider = timeProvider,
        screenStateReader = screenStateReader,
        bootIdentityReader = bootIdentityReader,
        scope = applicationScope
    )

    /**
     * Routine's live, narrow evening signal. The trigger is Routine-owned;
     * absent or unreadable Routine data resolves to no signal and leaves
     * standalone Meditation behavior available.
     */
    val routineEveningSignal: RoutineEveningSignal =
        ContentResolverRoutineEveningSignal(context.applicationContext.contentResolver)

    /**
     * The optional Evening Wind-Down runtime. Local-first and standalone: the
     * state model works without any Routine connection.
     */
    val eveningMeditationController: EveningMeditationController = EveningMeditationController(
        eveningRepository = eveningRepository,
        sessionRepository = sessionRepository,
        runtimeController = runtimeController,
        routineEveningSignal = routineEveningSignal,
        timeProvider = timeProvider,
        scope = applicationScope
    )

    /**
     * Read-only cross-app projections for Insights. Fail-open: unavailable or
     * untrusted projections render as "not connected" and never affect policy
     * or block meditation.
     */
    val attentionInsightClient: AttentionInsightClient =
        ContentResolverAttentionInsightClient(context.applicationContext.contentResolver)

    val readingInsightClient: ReadingInsightClient =
        ContentResolverReadingInsightClient(context.applicationContext.contentResolver)

    val recoveryRequestHandler: RecoveryRequestHandler = RecoveryRequestHandler(
        callerVerifier = callerVerifier,
        sessionService = sessionService,
        sessionStatusRepository = meditationStatusRepository
    )
}
