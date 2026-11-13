package org.example.project.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.SourceType

data class AboutUiState(
    val isLoading: Boolean = true,
    val totalExperiments: Int = 0,
    val ntrsCount: Int = 0,
    val psiCount: Int = 0,
    val textExtractedCount: Int = 0,
    val summaryCount: Int = 0,
    val needsReviewCount: Int = 0,
    val seriesCounts: Map<String, Int> = emptyMap(),
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)

class AboutViewModel(
    private val repository: ExperimentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                repository.getExperiments().collect { experiments ->
                    val ntrs = experiments.count { it.sourceType == SourceType.NTRS }
                    val psi = experiments.count { it.sourceType == SourceType.PSI }
                    val extracted = experiments.count { !it.rawText.isNullOrBlank() }
                    val summarized = experiments.count { it.summary != null }
                    val review = experiments.count { it.needsManualReview }
                    val seriesMap = experiments.groupBy { it.series }
                        .mapValues { it.value.size }
                        .filterKeys { it.isNotBlank() }

                    _uiState.value = AboutUiState(
                        isLoading = false,
                        totalExperiments = experiments.size,
                        ntrsCount = ntrs,
                        psiCount = psi,
                        textExtractedCount = extracted,
                        summaryCount = summarized,
                        needsReviewCount = review,
                        seriesCounts = seriesMap,
                        isConnected = repository.isSupabaseConnected(),
                        errorMessage = null
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
}
