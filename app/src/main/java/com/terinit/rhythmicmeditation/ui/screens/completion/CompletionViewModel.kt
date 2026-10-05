package com.terinit.rhythmicmeditation.ui.screens.completion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.util.SessionCues
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Completion screen.
 *
 * The completed session and its qualified minutes are REAL. Cooldown time is
 * owned by Rhythmic Routine; Meditation never invents or shortens it.
 */
data class CompletionUiState(
    val completedSession: MeditationSession? = null,
    val qualifiedMinutes: Int = 0,
    val isCooldownRestorative: Boolean = false
)

/**
 * State holder for the Completion screen.
 */
class CompletionViewModel(
    private val preferencesStore: AppPreferencesStore,
    sessionRepository: MeditationSessionRepository
) : ViewModel() {

    val uiState: StateFlow<CompletionUiState> =
        sessionRepository.observeSessions()
            .map { sessions ->
                val completed = sessions.firstOrNull {
                    it.status == MeditationSessionStatus.COMPLETED
                }
                CompletionUiState(
                    completedSession = completed,
                    qualifiedMinutes = (completed?.completedQualifiedSeconds ?: 0) / 60,
                    isCooldownRestorative =
                        completed?.kind == MeditationSessionKind.COOLDOWN_RESTORATIVE
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = CompletionUiState()
            )

    /** Optional completion bell — one soft tone, never part of qualification. */
    fun playCompletionCueIfEnabled() {
        viewModelScope.launch {
            val preferences = preferencesStore.preferences.first()
            if (preferences.sessionSoundEnabled) {
                SessionCues.playBell(frequencyHz = 660.0, durationMs = 900)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                CompletionViewModel(
                    preferencesStore = app.container.preferencesStore,
                    sessionRepository = app.container.sessionRepository
                )
            }
        }
    }
}
