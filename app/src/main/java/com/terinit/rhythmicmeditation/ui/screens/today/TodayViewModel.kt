package com.terinit.rhythmicmeditation.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferences
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the Today screen.
 *
 * The morning/restorative numbers are local placeholders in Pass 1; real
 * requirement data comes from Rhythmic Routine via the protocol in a later pass.
 */
data class TodayUiState(
    val activeSession: MeditationSession? = null,
    val preferences: AppPreferences = AppPreferences(),
    val morningRequired: Boolean = true,
    val restorativeReadingMinutes: Int = 20,
    val restorativeMeditationCompleted: Int = 0,
    val restorativeMeditationTarget: Int = 1
)

/**
 * State holder for the Today screen.
 */
class TodayViewModel(
    sessionRepository: MeditationSessionRepository,
    preferencesStore: AppPreferencesStore
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        sessionRepository.observeActiveSession(),
        preferencesStore.preferences
    ) { session, preferences ->
        TodayUiState(
            activeSession = session,
            preferences = preferences
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                TodayViewModel(
                    sessionRepository = app.container.sessionRepository,
                    preferencesStore = app.container.preferencesStore
                )
            }
        }
    }
}
