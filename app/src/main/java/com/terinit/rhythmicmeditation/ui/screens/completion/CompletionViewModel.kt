package com.terinit.rhythmicmeditation.ui.screens.completion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the Completion screen.
 *
 * Cooldown minutes are a placeholder in Pass 1: Rhythmic Routine owns the
 * cooldown and will supply the remaining time later.
 */
data class CompletionUiState(
    val completedSession: MeditationSession? = null,
    val requirementComplete: Boolean = true,
    val cooldownMinutesLeft: Int = 38,
    val cooldownTotalMinutes: Int = 90
)

/**
 * State holder for the Completion screen.
 */
class CompletionViewModel(
    sessionRepository: MeditationSessionRepository
) : ViewModel() {

    val uiState: StateFlow<CompletionUiState> =
        sessionRepository.observeSessions()
            .map { sessions ->
                val completed = sessions.firstOrNull {
                    it.completedAtEpochMs != null
                }
                CompletionUiState(completedSession = completed)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CompletionUiState()
            )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                CompletionViewModel(sessionRepository = app.container.sessionRepository)
            }
        }
    }
}
