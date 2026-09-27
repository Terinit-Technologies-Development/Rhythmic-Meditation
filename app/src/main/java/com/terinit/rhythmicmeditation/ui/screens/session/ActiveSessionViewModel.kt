package com.terinit.rhythmicmeditation.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.session.MeditationSessionService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Active Session screen.
 *
 * The timer display is a placeholder in Pass 1 (fixed demo values when no
 * session exists). Real qualified-time ticking lands in Pass 2 on top of
 * MeditationSessionService + intervals.
 */
data class ActiveSessionUiState(
    val session: MeditationSession? = null,
    val sessionLabel: String = "MORNING SESSION",
    val elapsedSeconds: Int = 768,
    val requiredSeconds: Int = 1800,
    val progress: Float = 0.43f
) {
    val progressPercent: Int get() = (progress * 100).toInt()
}

/**
 * State holder for the Active Session screen. Pause / end controls call into
 * the real session lifecycle service; timing accumulation is completed in Pass 2.
 */
class ActiveSessionViewModel(
    private val sessionService: MeditationSessionService,
    sessionRepository: MeditationSessionRepository
) : ViewModel() {

    val uiState: StateFlow<ActiveSessionUiState> =
        sessionRepository.observeActiveSession()
            .map { session -> session.toUiState() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = ActiveSessionUiState()
            )

    fun onPause() {
        val sessionId = uiState.value.session?.sessionId ?: return
        viewModelScope.launch {
            sessionService.pauseSession(sessionId, InterruptionType.MANUAL_PAUSE)
        }
    }

    fun onResume() {
        val sessionId = uiState.value.session?.sessionId ?: return
        viewModelScope.launch {
            sessionService.resumeSession(sessionId)
        }
    }

    /** Ends the session. Records completion when the requirement is met, otherwise cancels. */
    fun onEndSession(onFinished: () -> Unit) {
        val session = uiState.value.session
        if (session == null) {
            onFinished()
            return
        }
        viewModelScope.launch {
            if (session.isRequirementMet) {
                sessionService.completeSession(session.sessionId)
            } else {
                sessionService.cancelSession(session.sessionId)
            }
            onFinished()
        }
    }

    private fun MeditationSession?.toUiState(): ActiveSessionUiState {
        if (this == null) return ActiveSessionUiState()
        return ActiveSessionUiState(
            session = this,
            sessionLabel = kind.toLabel(),
            elapsedSeconds = completedQualifiedSeconds,
            requiredSeconds = requiredSeconds,
            progress = progress
        )
    }

    private fun MeditationSessionKind.toLabel(): String = when (this) {
        MeditationSessionKind.MORNING_REQUIRED -> "MORNING SESSION"
        MeditationSessionKind.COOLDOWN_RESTORATIVE -> "RESTORATIVE SESSION"
        MeditationSessionKind.EVENING_WIND_DOWN -> "EVENING SESSION"
        MeditationSessionKind.STANDALONE -> "OPEN SESSION"
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                ActiveSessionViewModel(
                    sessionService = app.container.sessionService,
                    sessionRepository = app.container.sessionRepository
                )
            }
        }
    }
}
