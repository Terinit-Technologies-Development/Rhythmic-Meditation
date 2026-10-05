package com.terinit.rhythmicmeditation.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferences
import com.terinit.rhythmicmeditation.data.local.prefs.AppPreferencesStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State holder for the Settings screen. Preferences are persisted in
 * DataStore; session evidence never lives here.
 */
class SettingsViewModel(
    private val preferencesStore: AppPreferencesStore
) : ViewModel() {

    val uiState: StateFlow<AppPreferences> = preferencesStore.preferences
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppPreferences()
        )

    fun setSessionSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setSessionSoundEnabled(enabled) }
    }

    fun setSubtleHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesStore.setSubtleHapticsEnabled(enabled) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                SettingsViewModel(preferencesStore = app.container.preferencesStore)
            }
        }
    }
}
