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
import com.terinit.rhythmicmeditation.domain.session.MorningSessionPolicy
import com.terinit.rhythmicmeditation.domain.session.MorningStatus
import com.terinit.rhythmicmeditation.runtime.MeditationRuntimeController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for the Today screen.
 *
 * The morning requirement is REAL local state (Pass 2 dogfood): REQUIRED until
 * today's morning meditation runs, COMPLETE once it has. Cooldown/restorative
 * numbers remain placeholders until Routine supplies policy in Pass 3.
 */
data class TodayUiState(
    val morningStatus: MorningStatus = MorningStatus.REQUIRED,
    val currentSession: MeditationSession? = null,
    val preferences: AppPreferences = AppPreferences(),
    val restorativeReadingMinutes: Int = 20,
    val restorativeMeditationCompleted: Int = 0,
    val restorativeMeditationTarget: Int = 1
)

/**
 * State holder for the Today screen. Begins today's morning meditation
 * through the runtime (idempotent — never two morning sessions per id).
 */
class TodayViewModel(
    private val runtimeController: MeditationRuntimeController,
    sessionRepository: MeditationSessionRepository,
    preferencesStore: AppPreferencesStore
) : ViewModel() {

    val uiState: StateFlow<TodayUiState> = combine(
        sessionRepository.observeSessions(),
        runtimeController.state,
        preferencesStore.preferences
    ) { sessions, runtime, preferences ->
        val dayId = runtimeController.currentRhythmicDayId()
        TodayUiState(
            morningStatus = MorningSessionPolicy.resolve(sessions, dayId),
            currentSession = runtime.session,
            preferences = preferences
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TodayUiState()
    )

    /**
     * Begins (or re-opens) today's morning session. Idempotent: the same
     * session id is reused; a cancelled attempt starts a fresh session with no
     * inherited progress.
     */
    fun onBeginMorningSession(onReady: () -> Unit) {
        viewModelScope.launch {
            runtimeController.startMorningSession()
            onReady()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                TodayViewModel(
                    runtimeController = app.container.runtimeController,
                    sessionRepository = app.container.sessionRepository,
                    preferencesStore = app.container.preferencesStore
                )
            }
        }
    }
}
