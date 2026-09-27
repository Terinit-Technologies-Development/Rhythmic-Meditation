package com.terinit.rhythmicmeditation.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.terinit.rhythmicmeditation.app.RhythmicMeditationApp
import com.terinit.rhythmicmeditation.data.repository.MeditationInsightsRepository
import com.terinit.rhythmicmeditation.domain.model.MeditationInsightSnapshot
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * UI state for the Insights screen.
 *
 * PLACEHOLDER (Pass 1): chart values are illustrative demo data. Real insight
 * generation and aggregation land in a later pass on top of
 * [MeditationInsightsRepository] snapshots.
 */
data class InsightsUiState(
    val snapshots: List<MeditationInsightSnapshot> = emptyList(),
    val todayMeditationMinutes: Int = 20,
    val weeklySessionsCompleted: Int = 5,
    val weeklySessionsTarget: Int = 7,
    val weeklyMinutes: List<Int> = listOf(38, 60, 38, 22, 45, 20, 0),
    val restorativeGatesCompleted: Int = 3,
    val restorativeGatesTarget: Int = 5,
    val readingMinutes: Int = 140,
    val meditationMinutes: Int = 20
) {
    val totalMinutes: Int get() = readingMinutes + meditationMinutes
    val meditationShare: Float
        get() = if (totalMinutes == 0) 0f else meditationMinutes.toFloat() / totalMinutes
}

/**
 * State holder for the Insights screen.
 */
class InsightsViewModel(
    insightsRepository: MeditationInsightsRepository
) : ViewModel() {

    val uiState: StateFlow<InsightsUiState> =
        insightsRepository.observeLatestInsightSnapshots()
            .map { snapshots -> InsightsUiState(snapshots = snapshots) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = InsightsUiState()
            )

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
                    as RhythmicMeditationApp
                InsightsViewModel(insightsRepository = app.container.insightsRepository)
            }
        }
    }
}
