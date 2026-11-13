package org.example.project.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment

data class YearTimelineGroup(
    val year: Int,
    val experiments: List<Experiment>
)

data class TimelineUiState(
    val isLoading: Boolean = true,
    val allExperiments: List<Experiment> = emptyList(),
    val timelineGroups: List<YearTimelineGroup> = emptyList(),
    val undatedExperiments: List<Experiment> = emptyList(),
    val availableSeries: List<String> = emptyList(),
    val selectedSeries: String? = null,
    val isAscending: Boolean = false, // Varsayılan: Yeniden eskiye (azalan)
    val totalCount: Int = 0,
    val errorMessage: String? = null
)

class TimelineViewModel(
    private val repository: ExperimentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TimelineUiState())
    val uiState: StateFlow<TimelineUiState> = _uiState.asStateFlow()

    init {
        loadTimeline()
    }

    fun loadTimeline() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getExperiments().collect { experiments ->
                    val seriesList = experiments.map { it.series.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sorted()

                    processAndEmit(
                        experiments = experiments,
                        seriesList = seriesList,
                        selectedSeries = _uiState.value.selectedSeries,
                        isAscending = _uiState.value.isAscending
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun setSeriesFilter(series: String?) {
        val newSeries = if (series == "ALL" || series.isNullOrBlank()) null else series
        _uiState.value = _uiState.value.copy(selectedSeries = newSeries)
        processAndEmit(
            experiments = _uiState.value.allExperiments,
            seriesList = _uiState.value.availableSeries,
            selectedSeries = newSeries,
            isAscending = _uiState.value.isAscending
        )
    }

    fun toggleSortOrder() {
        val newOrder = !_uiState.value.isAscending
        _uiState.value = _uiState.value.copy(isAscending = newOrder)
        processAndEmit(
            experiments = _uiState.value.allExperiments,
            seriesList = _uiState.value.availableSeries,
            selectedSeries = _uiState.value.selectedSeries,
            isAscending = newOrder
        )
    }

    private fun processAndEmit(
        experiments: List<Experiment>,
        seriesList: List<String>,
        selectedSeries: String?,
        isAscending: Boolean
    ) {
        val filtered = if (selectedSeries != null) {
            experiments.filter { it.series.equals(selectedSeries, ignoreCase = true) }
        } else {
            experiments
        }

        // Yıla göre grupla
        val dated = filtered.filter { it.year != null }
        val undated = filtered.filter { it.year == null }

        val grouped = dated.groupBy { it.year!! }
            .map { (year, list) -> YearTimelineGroup(year, list) }

        val sortedGroups = if (isAscending) {
            grouped.sortedBy { it.year }
        } else {
            grouped.sortedByDescending { it.year }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            allExperiments = experiments,
            timelineGroups = sortedGroups,
            undatedExperiments = undated,
            availableSeries = seriesList,
            selectedSeries = selectedSeries,
            isAscending = isAscending,
            totalCount = filtered.size,
            errorMessage = null
        )
    }
}
