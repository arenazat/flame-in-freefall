package org.example.project.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.GenerateReportUseCase
import org.example.project.domain.usecase.GetExperimentsUseCase
import org.example.project.domain.usecase.GetFilterOptionsUseCase
import org.example.project.models.Experiment
import org.example.project.models.ExperimentFilter
import org.example.project.models.FilterOptions
import org.example.project.models.ReportResult

data class ExperimentListUiState(
    val experiments: List<Experiment> = emptyList(),
    val filterOptions: FilterOptions = FilterOptions(),
    val currentFilter: ExperimentFilter = ExperimentFilter(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isFilterPanelOpen: Boolean = false, // Mobil için açılır filtre paneli
    val selectedExperimentIds: Set<String> = emptySet(),
    val isGeneratingReport: Boolean = false,
    val lastGeneratedReport: ReportResult? = null,
    val reportErrorMessage: String? = null,
    val errorMessage: String? = null
)

class ExperimentListViewModel(
    private val getExperimentsUseCase: GetExperimentsUseCase,
    private val getFilterOptionsUseCase: GetFilterOptionsUseCase,
    private val generateReportUseCase: GenerateReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExperimentListUiState())
    val uiState: StateFlow<ExperimentListUiState> = _uiState.asStateFlow()

    private var searchDebounceJob: Job? = null

    init {
        loadFilterOptions()
        loadExperiments()
    }

    fun loadFilterOptions() {
        viewModelScope.launch {
            try {
                val options = getFilterOptionsUseCase()
                _uiState.update { it.copy(filterOptions = options) }
            } catch (e: Exception) {
                println("[HATA] Filtre seçenekleri yüklenemedi: ${e.message}")
            }
        }
    }

    fun loadExperiments() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                getExperimentsUseCase(_uiState.value.currentFilter)
                    .collect { list ->
                        _uiState.update {
                            it.copy(
                                experiments = list,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load records: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(300) // Debounce
            val updatedFilter = _uiState.value.currentFilter.copy(
                query = query.trim().takeIf { it.isNotBlank() }
            )
            _uiState.update { it.copy(currentFilter = updatedFilter) }
            loadExperiments()
        }
    }

    fun onSeriesSelected(series: String?) {
        val updated = _uiState.value.currentFilter.copy(
            series = if (series == "ALL" || series == null) null else series
        )
        _uiState.update { it.copy(currentFilter = updated) }
        loadExperiments()
    }

    fun onMaterialSelected(material: String?) {
        val updated = _uiState.value.currentFilter.copy(
            material = if (material == "ALL" || material == null) null else material
        )
        _uiState.update { it.copy(currentFilter = updated) }
        loadExperiments()
    }

    fun onGravityLevelSelected(gravity: String?) {
        val updated = _uiState.value.currentFilter.copy(
            gravityLevel = if (gravity == "ALL" || gravity == null) null else gravity
        )
        _uiState.update { it.copy(currentFilter = updated) }
        loadExperiments()
    }

    fun toggleFilterPanel() {
        _uiState.update { it.copy(isFilterPanelOpen = !it.isFilterPanelOpen) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                currentFilter = ExperimentFilter()
            )
        }
        loadExperiments()
    }

    fun refresh() {
        loadFilterOptions()
        loadExperiments()
    }

    // --- Rapor Modu & Deney Seçimi (Faz 4.3) ---

    fun toggleExperimentSelection(id: String) {
        _uiState.update { current ->
            val set = current.selectedExperimentIds.toMutableSet()
            if (set.contains(id)) {
                set.remove(id)
            } else {
                set.add(id)
            }
            current.copy(selectedExperimentIds = set)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedExperimentIds = emptySet()) }
    }

    fun selectAll() {
        val allIds = _uiState.value.experiments.map { it.id }.toSet()
        _uiState.update { it.copy(selectedExperimentIds = allIds) }
    }

    fun generateReportForSelected(onSuccess: ((String) -> Unit)? = null) {
        val selectedIds = _uiState.value.selectedExperimentIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingReport = true, reportErrorMessage = null) }
            val result = generateReportUseCase(
                experimentIds = selectedIds,
                title = "NASA Microgravity Experiment Report (${selectedIds.size} records)"
            )

            result.fold(
                onSuccess = { report ->
                    _uiState.update {
                        it.copy(
                            isGeneratingReport = false,
                            lastGeneratedReport = report,
                            reportErrorMessage = null
                        )
                    }
                    if (report.reportUrl.isNotBlank()) {
                        onSuccess?.invoke(report.reportUrl)
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isGeneratingReport = false,
                            reportErrorMessage = "Could not generate the report: ${err.message}"
                        )
                    }
                }
            )
        }
    }

    fun dismissReportNotification() {
        _uiState.update { it.copy(lastGeneratedReport = null, reportErrorMessage = null) }
    }
}
