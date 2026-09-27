package com.terinit.rhythmicmeditation.ui.screens.evening

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditation
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationRecord
import com.terinit.rhythmicmeditation.domain.evening.EveningMeditationState
import com.terinit.rhythmicmeditation.domain.insights.MeditationInsights
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.timing.TimeProvider
import com.terinit.rhythmicmeditation.runtime.EveningMeditationController
import java.time.ZoneId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Evening Wind-Down screen.
 *
 * Everything renders from real local state; nothing is fabricated and no
 * wording is ever punitive. [state] is the EFFECTIVE state (an expired snooze
 * is DUE again).
 */
data class EveningUiState(
    val attentionDayId: String = "",
    val state: EveningMeditationState = EveningMeditationState.NOT_DUE,
    val snoozedUntilEpochMs: Long? = null,
    val eveningQualifiedSeconds: Int = 0,
    val eveningRequiredSeconds: Int = EveningMeditation.REQUIRED_SECONDS,
    val todaySessionCount: Int = 0,
    val todayMeditationMinutes: Int = 0
)

/**
 * Pure assembly of [EveningUiState] — kept out of the ViewModel so state
 * rendering is unit-testable without Android infrastructure.
 */
fun buildEveningUiState(
    record: EveningMeditationRecord?,
    sessions: List<MeditationSession>,
    attentionDayId: String,
    todayDateKey: String,
    nowEpochMs: Long,
    zone: ZoneId = ZoneId.systemDefault()
): EveningUiState {
    val bound = record?.sessionId?.let { id -> sessions.firstOrNull { it.sessionId == id } }
    return EveningUiState(
        attentionDayId = attentionDayId,
        state = EveningMeditation.effectiveState(record, nowEpochMs),
        snoozedUntilEpochMs = record?.snoozedUntilEpochMs,
        eveningQualifiedSeconds = bound?.completedQualifiedSeconds ?: 0,
        eveningRequiredSeconds = bound?.requiredSeconds ?: EveningMeditation.REQUIRED_SECONDS,
        todaySessionCount = sessions.count {
            MeditationInsights.isCountedForMinutes(it) &&
                it.completedQualifiedSeconds > 0 &&
                MeditationInsights.activityDateKeyOf(it, zone) == todayDateKey
        },
        todayMeditationMinutes = MeditationInsights.meditationMinutesForDate(sessions, todayDateKey, zone)
    )
}

/**
 * State holder for the Evening Wind-Down screen.
 *
 * The trigger for DUE is Routine-owned (narrow signal surface on
 * [EveningMeditationController]); the state model, snooze, defer, and the
 * session itself are local-first and work standalone.
 */
class EveningViewModel(
    private val eveningController: EveningMeditationController,
    sessionRepository: MeditationSessionRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val tick = MutableStateFlow(0L)

    init {
        // Routine owns the trigger; a present signal marks the day DUE. With
        // no signal nothing is ever fabricated.
        eveningController.syncFromSignal()

        // Render tick only: keeps the snooze countdown/expire honest. No
        // persistence happens per tick.
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                tick.value++
            }
        }
    }

    val uiState: StateFlow<EveningUiState> = combine(
        eveningController.observeRecord(),
        sessionRepository.observeSessions(),
        tick
    ) { record, sessions, _ ->
        val now = timeProvider.currentTimeMillis()
        buildEveningUiState(
            record = record,
            sessions = sessions,
            attentionDayId = eveningController.currentAttentionDayId(),
            todayDateKey = MeditationInsights.dateKeyOf(now),
            nowEpochMs = now
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EveningUiState()
    )

    /** Starts (or continues) tonight's evening session; idempotent per day. */
    fun onStartNow(onReady: () -> Unit) {
        viewModelScope.launch {
            eveningController.startEveningSession()
            onReady()
        }
    }

    /** Snoozes the offer for 15 minutes ("Snoozed · back at <time>"). */
    fun onSnooze15() {
        viewModelScope.launch { eveningController.snooze15() }
    }

    /** Defers tonight's practice. Non-punitive; tomorrow is unaffected. */
    fun onDeferTonight() {
        viewModelScope.launch { eveningController.deferTonight() }
    }

    /** Picks a snoozed offer back up early. */
    fun onResumeFromSnooze() {
        viewModelScope.launch { eveningController.resumeFromSnooze() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                EveningViewModel(
                    eveningController = app.container.eveningMeditationController,
                    sessionRepository = app.container.sessionRepository,
                    timeProvider = app.container.timeProvider
                )
            }
        }
    }
}
