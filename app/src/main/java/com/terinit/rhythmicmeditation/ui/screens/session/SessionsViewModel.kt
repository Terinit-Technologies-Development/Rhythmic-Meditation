package com.terinit.rhythmicmeditation.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationSessionRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationSession
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.runtime.MeditationRuntimeController
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<MeditationSession> = emptyList()
)

/** Session tab state: current-ledger history plus a standalone session action. */
class SessionsViewModel(
    sessionRepository: MeditationSessionRepository,
    private val runtimeController: MeditationRuntimeController
) : ViewModel() {

    val uiState: StateFlow<SessionsUiState> = sessionRepository.observeSessions()
        .map { sessions ->
            SessionsUiState(sessions.filterNot(MeditationSession::isActiveLifecycle))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SessionsUiState()
        )

    fun startStandaloneSession(onStarted: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            runtimeController.startSession(kind = MeditationSessionKind.STANDALONE)
                .onSuccess { onStarted() }
                .onFailure { onFailure() }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                SessionsViewModel(
                    sessionRepository = app.container.sessionRepository,
                    runtimeController = app.container.runtimeController
                )
            }
        }
    }
}
