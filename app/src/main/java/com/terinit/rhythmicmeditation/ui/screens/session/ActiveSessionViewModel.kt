package com.terinit.rhythmicmeditation.ui.screens.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import com.terinit.rhythmicmeditation.domain.model.InterruptionType
import com.terinit.rhythmicmeditation.domain.model.MeditationMode
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionKind
import com.terinit.rhythmicmeditation.domain.model.MeditationSessionStatus
import com.terinit.rhythmicmeditation.runtime.MeditationRuntimeController
import com.terinit.rhythmicmeditation.runtime.RecoveryNotice
import com.terinit.rhythmicmeditation.util.SessionCues
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Rendering projection of the active session.
 *
 * The timer text is DERIVED from the monotonic ledger every tick; nothing here
 * mutates the ledger. Pause/Resume/End are the only writes, and they go through
 * the meditation runtime.
 */
data class ActiveSessionUiState(
    val status: MeditationSessionStatus? = null,
    val sessionLabel: String = "MORNING SESSION",
    val elapsedSeconds: Int = 0,
    val requiredSeconds: Int = MeditationRuntimeController.MORNING_REQUIRED_SECONDS,
    val progress: Float = 0f,
    val isPaused: Boolean = false,
    val requirementMet: Boolean = false,
    val mode: MeditationMode = MeditationMode.STILLNESS,
    val recoveryNotice: RecoveryNotice? = null
) {
    val progressPercent: Int get() = (progress * 100).toInt()
}

/**
 * State holder for the Active Session screen. All timing authority lives in
 * [MeditationRuntimeController]; this ViewModel only renders and forwards
 * user intent.
 */
class ActiveSessionViewModel(
    private val runtimeController: MeditationRuntimeController,
    private val preferencesStore: AppPreferencesStore
) : ViewModel() {

    private val tick = MutableStateFlow(0L)

    init {
        // Render tick only: redraws the derived timer. No persistence.
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                tick.value++
            }
        }
    }

    val uiState: StateFlow<ActiveSessionUiState> = combine(
        runtimeController.state,
        tick,
        preferencesStore.preferences
    ) { runtime, _, prefs ->
        val session = runtime.session
        ActiveSessionUiState(
            status = session?.status,
            sessionLabel = (session?.kind ?: MeditationSessionKind.MORNING_REQUIRED).toLabel(),
            elapsedSeconds = runtime.qualifiedSeconds(runtimeController.nowElapsedMs()),
            requiredSeconds = session?.requiredSeconds
                ?: MeditationRuntimeController.MORNING_REQUIRED_SECONDS,
            progress = session?.let {
                runtime.qualifiedSeconds(runtimeController.nowElapsedMs()).toFloat() /
                    it.requiredSeconds.coerceAtLeast(1)
            }?.coerceIn(0f, 1f) ?: 0f,
            isPaused = session?.status == MeditationSessionStatus.PAUSED,
            requirementMet = runtime.isRequirementMet(runtimeController.nowElapsedMs()),
            mode = runtime.mode,
            recoveryNotice = runtime.recoveryNotice
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ActiveSessionUiState()
    )

    fun onPauseResume() {
        viewModelScope.launch {
            val state = uiState.value
            val soundEnabled = preferencesStore.preferences.first().sessionSoundEnabled
            if (state.isPaused) {
                runtimeController.resume()
                if (soundEnabled) SessionCues.playBell(frequencyHz = 432.0, durationMs = 500)
            } else {
                runtimeController.pause(InterruptionType.MANUAL_PAUSE)
            }
        }
    }

    /** Ends the session. Completes when the requirement is met, else cancels. */
    fun onEndSessionConfirmed() {
        viewModelScope.launch {
            runtimeController.endSessionConfirmed()
        }
    }

    fun onSelectMode(mode: MeditationMode) {
        runtimeController.setMode(mode)
        viewModelScope.launch {
            preferencesStore.setLastSelectedMeditationMode(mode)
        }
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
                    runtimeController = app.container.runtimeController,
                    preferencesStore = app.container.preferencesStore
                )
            }
        }
    }
}
