package com.terinit.rhythmicmeditation.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightClient
import com.terinit.rhythmicmeditation.integration.contract.AttentionInsightProjection
import com.terinit.rhythmicmeditation.integration.contract.ReadingEvidence
import com.terinit.rhythmicmeditation.integration.contract.ReadingInsightClient
import com.terinit.rhythmicmeditation.runtime.EveningMeditationController
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * UI state for the Insights screen.
 *
 * Truthful and descriptive only: every number is real local evidence or a
 * read-only cross-app projection. No scores, no streaks, no judgment — a
 * missed day is never framed as a failure. Missing Routine or Reader renders
 * as partial data with quiet "not connected" copy.
 */
data class InsightsUiState(
    val todayDateKey: String = "",
    val todayMeditationMinutes: Int = 0,
    val morningStatus: MorningStatus = MorningStatus.REQUIRED,
    val eveningStatus: EveningMeditationState = EveningMeditationState.NOT_DUE,
    val weeklyMinutes: List<Int> = List(7) { 0 },
    val weekDateKeys: List<String> = emptyList(),
    val daysWithCompletedMeditation: Int = 0,
    val weeklyCompletedSessionCount: Int = 0,
    // Routine projection (read-only; null = not connected)
    val routineConnected: Boolean = false,
    val gatesCreatedToday: Int = 0,
    val gatesSatisfiedToday: Int = 0,
    val gatesSatisfiedWeek: Int = 0,
    val substitutionChoicesRemaining: Int = 0,
    // Reader projection (read-only; null = not connected)
    val readerConnected: Boolean = false,
    val readingVerifiedSecondsToday: Int = 0,
    val readingQualifiedPagesToday: Int = 0,
    // Behavioral observation (shown only at >= 3 / >= 3 samples)
    val observation: String? = null
) {
    val readingMinutes: Int get() = readingVerifiedSecondsToday / 60

    val totalMinutes: Int get() = readingMinutes + todayMeditationMinutes

    /** Inward-attention share of today's deliberate attention time. */
    val meditationShare: Float
        get() = if (totalMinutes == 0) 0f else todayMeditationMinutes.toFloat() / totalMinutes
}

/** Read-only projection bundle (Routine + Reader), or nulls when not connected. */
data class InsightProjections(
    val routineToday: AttentionInsightProjection?,
    val routineWeek: List<AttentionInsightProjection>,
    val readingToday: ReadingEvidence?
)

/**
 * Pure assembly of [InsightsUiState] — kept out of the ViewModel so partial
 * (missing Routine / missing Reader) rendering is unit-testable without
 * Android infrastructure. Never fabricates: unavailable projections leave
 * their fields at zero and [routineConnected] / [readerConnected] false.
 */
fun buildInsightsUiState(
    sessions: List<MeditationSession>,
    eveningRecord: EveningMeditationRecord?,
    todayDateKey: String,
    weekDateKeys: List<String>,
    nowEpochMs: Long,
    routineToday: AttentionInsightProjection?,
    routineWeek: List<AttentionInsightProjection>,
    readingToday: ReadingEvidence?,
    zone: ZoneId = ZoneId.systemDefault()
): InsightsUiState {
    val sample = MeditationInsights.morningComparisonSample(sessions, weekDateKeys, zone)
    return InsightsUiState(
        todayDateKey = todayDateKey,
        todayMeditationMinutes = MeditationInsights.meditationMinutesForDate(sessions, todayDateKey, zone),
        morningStatus = MeditationInsights.morningStatus(sessions, todayDateKey),
        eveningStatus = MeditationInsights.eveningStatus(eveningRecord, nowEpochMs),
        weeklyMinutes = MeditationInsights.weeklyMinutes(sessions, weekDateKeys, zone),
        weekDateKeys = weekDateKeys,
        daysWithCompletedMeditation = MeditationInsights.daysWithCompletedMeditation(sessions, weekDateKeys, zone),
        weeklyCompletedSessionCount = MeditationInsights.weeklyCompletedSessionCount(sessions, weekDateKeys, zone),
        routineConnected = routineToday != null,
        gatesCreatedToday = routineToday?.restorativeGatesCreated ?: 0,
        gatesSatisfiedToday = routineToday?.restorativeGatesSatisfied ?: 0,
        gatesSatisfiedWeek = routineWeek.sumOf { it.restorativeGatesSatisfied },
        substitutionChoicesRemaining = routineToday
            ?.let { MeditationInsights.substitutionChoicesRemaining(it.meditationSubstitutionsUsed) }
            ?: 0,
        readerConnected = readingToday != null,
        readingVerifiedSecondsToday = readingToday?.verifiedActiveSeconds ?: 0,
        readingQualifiedPagesToday = readingToday?.qualifiedPages ?: 0,
        observation = MeditationInsights.morningCorrelationObservation(
            morningDays = sample.morningDays,
            comparisonDays = sample.comparisonDays
        )
    )
}

/**
 * State holder for the Insights screen. Local data is authoritative; the
 * Routine and Reader projections are read-only and fail-open — they can only
 * ever ADD context, never change policy or block meditation. Projections are
 * refreshed when Insights enters or resumes.
 */
class InsightsViewModel(
    sessionRepository: MeditationSessionRepository,
    private val eveningController: EveningMeditationController,
    private val attentionInsightClient: AttentionInsightClient,
    private val readingInsightClient: ReadingInsightClient,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val projections = MutableStateFlow<InsightProjections?>(null)
    private var refreshJob: Job? = null

    init {
        refreshProjections()
    }

    val uiState: StateFlow<InsightsUiState> = combine(
        sessionRepository.observeSessions(),
        eveningController.observeRecord(),
        projections
    ) { sessions, eveningRecord, loaded ->
        val now = timeProvider.currentTimeMillis()
        val todayDateKey = MeditationInsights.dateKeyOf(now)
        buildInsightsUiState(
            sessions = sessions,
            eveningRecord = eveningRecord,
            todayDateKey = todayDateKey,
            weekDateKeys = MeditationInsights.weekDateKeys(todayDateKey),
            nowEpochMs = now,
            routineToday = loaded?.routineToday,
            routineWeek = loaded?.routineWeek.orEmpty(),
            readingToday = loaded?.readingToday
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = InsightsUiState()
    )

    /** Refreshes the cross-app projections without affecting local session state. */
    fun refreshProjections() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            projections.value = withContext(Dispatchers.IO) {
                runCatching { loadProjections() }
                    .getOrElse { InsightProjections(null, emptyList(), null) }
            }
        }
    }

    /** Missing apps, denied providers, and malformed rows resolve to partial data. */
    private fun loadProjections(): InsightProjections {
        val now = timeProvider.currentTimeMillis()
        val todayDateKey = MeditationInsights.dateKeyOf(now)
        val todayAttentionDayId = eveningController.currentAttentionDayId()
        val queryDays = (MeditationInsights.weekDateKeys(todayDateKey) + todayAttentionDayId)
            .distinct()
        val routineByDay = queryDays
            .mapNotNull { day -> attentionInsightClient.attentionDay(day) }
            .associateBy { it.attentionDayId }
        return InsightProjections(
            routineToday = routineByDay[todayAttentionDayId],
            routineWeek = routineByDay.values.toList(),
            readingToday = readingInsightClient.dailyEvidence(todayDateKey)
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                InsightsViewModel(
                    sessionRepository = app.container.sessionRepository,
                    eveningController = app.container.eveningMeditationController,
                    attentionInsightClient = app.container.attentionInsightClient,
                    readingInsightClient = app.container.readingInsightClient,
                    timeProvider = app.container.timeProvider
                )
            }
        }
    }
}
