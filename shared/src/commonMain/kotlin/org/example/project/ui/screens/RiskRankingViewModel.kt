package org.example.project.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.GetExperimentsUseCase
import org.example.project.models.Experiment

enum class RiskSortCriteria(val label: String) {
    RISK_DESC("Highest risk"),
    RISK_ASC("Lowest risk"),
    COUNT_DESC("Record count"),
    NAME_ASC("Material name (A-Z)")
}

data class MaterialRiskSummary(
    val material: String,
    val experimentCount: Int,
    val averageRiskScore: Double?,
    val highestRiskExperiment: Experiment?,
    val highestRiskScore: Int?,
    val hasRiskScore: Boolean
)

data class RiskRankingUiState(
    val summaries: List<MaterialRiskSummary> = emptyList(),
    val filteredSummaries: List<MaterialRiskSummary> = emptyList(),
    val sortCriteria: RiskSortCriteria = RiskSortCriteria.RISK_DESC,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val totalMaterialsCount: Int = 0,
    val ratedMaterialsCount: Int = 0,
    val unratedMaterialsCount: Int = 0
)

class RiskRankingViewModel(
    private val getExperimentsUseCase: GetExperimentsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RiskRankingUiState())
    val uiState: StateFlow<RiskRankingUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                getExperimentsUseCase().collect { experiments ->
                    processExperiments(experiments)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Could not load risk ranking data: ${e.message}"
                    )
                }
            }
        }
    }

    private fun processExperiments(experiments: List<Experiment>) {
        if (experiments.isEmpty()) {
            _uiState.update {
                it.copy(
                    summaries = emptyList(),
                    filteredSummaries = emptyList(),
                    isLoading = false
                )
            }
            return
        }

        // Malzemeye göre grupla
        val grouped = experiments.groupBy {
            it.material.trim().ifBlank { "Other samples" }
        }

        val summaries = grouped.map { (mat, list) ->
            val scores = list.mapNotNull { it.summary?.riskScore }
            val avgScore = if (scores.isNotEmpty()) scores.average() else null
            val highestScore = scores.maxOrNull()
            val highestExp = if (highestScore != null) {
                list.firstOrNull { it.summary?.riskScore == highestScore }
            } else {
                list.firstOrNull()
            }

            MaterialRiskSummary(
                material = mat,
                experimentCount = list.size,
                averageRiskScore = avgScore,
                highestRiskExperiment = highestExp,
                highestRiskScore = highestScore,
                hasRiskScore = scores.isNotEmpty()
            )
        }

        val ratedCount = summaries.count { it.hasRiskScore }
        val unratedCount = summaries.count { !it.hasRiskScore }

        _uiState.update { current ->
            val sortedAndFiltered = applySortAndFilter(
                list = summaries,
                criteria = current.sortCriteria,
                query = current.searchQuery
            )
            current.copy(
                summaries = summaries,
                filteredSummaries = sortedAndFiltered,
                totalMaterialsCount = summaries.size,
                ratedMaterialsCount = ratedCount,
                unratedMaterialsCount = unratedCount,
                isLoading = false
            )
        }
    }

    fun onSortCriteriaChanged(criteria: RiskSortCriteria) {
        _uiState.update { current ->
            val sorted = applySortAndFilter(current.summaries, criteria, current.searchQuery)
            current.copy(sortCriteria = criteria, filteredSummaries = sorted)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = applySortAndFilter(current.summaries, current.sortCriteria, query)
            current.copy(searchQuery = query, filteredSummaries = filtered)
        }
    }

    private fun applySortAndFilter(
        list: List<MaterialRiskSummary>,
        criteria: RiskSortCriteria,
        query: String
    ): List<MaterialRiskSummary> {
        val trimmedQuery = query.trim().lowercase()
        val filtered = if (trimmedQuery.isNotBlank()) {
            list.filter { it.material.lowercase().contains(trimmedQuery) }
        } else {
            list
        }

        // Risk skoru olanlar ve olmayanlar
        val rated = filtered.filter { it.hasRiskScore }
        val unrated = filtered.filter { !it.hasRiskScore }

        return when (criteria) {
            RiskSortCriteria.RISK_DESC -> {
                // En yüksek risk üstte; skoru olmayanlar kural gereği listenin en sonunda
                rated.sortedByDescending { it.averageRiskScore ?: 0.0 } + unrated.sortedByDescending { it.experimentCount }
            }
            RiskSortCriteria.RISK_ASC -> {
                // En düşük risk üstte; skoru olmayanlar yine listenin sonunda
                rated.sortedBy { it.averageRiskScore ?: 100.0 } + unrated.sortedByDescending { it.experimentCount }
            }
            RiskSortCriteria.COUNT_DESC -> {
                filtered.sortedByDescending { it.experimentCount }
            }
            RiskSortCriteria.NAME_ASC -> {
                filtered.sortedBy { it.material.lowercase() }
            }
        }
    }
}
