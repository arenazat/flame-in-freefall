package org.example.project.ui.psi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.GetPsiFilterOptionsUseCase
import org.example.project.domain.usecase.GetPsiInvestigationsUseCase
import org.example.project.models.FuelState
import org.example.project.models.PsiFilter
import org.example.project.models.PsiFilterOptions
import org.example.project.models.PsiInvestigation
import org.example.project.models.SafetyCategory

data class PsiCatalogUiState(
    val isLoading: Boolean = true,
    val items: List<PsiInvestigation> = emptyList(),
    val totalCount: Int = 0,
    val options: PsiFilterOptions = PsiFilterOptions(),
    val filter: PsiFilter = PsiFilter(),
    val selectedIds: List<String> = emptyList()
) {
    val activeFilterCount: Int
        get() = filter.categories.size + filter.fuelStates.size + filter.hardwareGroups.size +
            (if (filter.flightOnly != null) 1 else 0) + (if (filter.o2Range != null) 1 else 0) +
            (if (filter.pressureRangeAtm != null) 1 else 0)
}

class PsiCatalogViewModel(
    private val getInvestigations: GetPsiInvestigationsUseCase,
    private val getOptions: GetPsiFilterOptionsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PsiCatalogUiState())
    val uiState: StateFlow<PsiCatalogUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val options = getOptions()
            _uiState.update { it.copy(options = options) }
            val all = getInvestigations(PsiFilter())
            _uiState.update { it.copy(totalCount = all.size) }
            reload()
        }
    }

    fun reload() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val items = getInvestigations(_uiState.value.filter)
            _uiState.update { it.copy(isLoading = false, items = items) }
        }
    }

    private fun setFilter(f: PsiFilter, debounce: Boolean = false) {
        _uiState.update { it.copy(filter = f) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(300)
            reload()
        }
    }

    fun onQuery(q: String) = setFilter(_uiState.value.filter.copy(query = q), debounce = true)

    fun toggleCategory(c: SafetyCategory) {
        val f = _uiState.value.filter
        setFilter(f.copy(categories = if (c in f.categories) f.categories - c else f.categories + c))
    }

    fun toggleFuel(s: FuelState) {
        val f = _uiState.value.filter
        setFilter(f.copy(fuelStates = if (s in f.fuelStates) f.fuelStates - s else f.fuelStates + s))
    }

    fun toggleHardware(h: String) {
        val f = _uiState.value.filter
        setFilter(f.copy(hardwareGroups = if (h in f.hardwareGroups) f.hardwareGroups - h else f.hardwareGroups + h))
    }

    fun setFlightOnly(v: Boolean?) = setFilter(_uiState.value.filter.copy(flightOnly = v))

    fun setO2Range(r: ClosedFloatingPointRange<Double>?) = setFilter(_uiState.value.filter.copy(o2Range = r), debounce = true)

    fun setPressureRange(r: ClosedFloatingPointRange<Double>?) =
        setFilter(_uiState.value.filter.copy(pressureRangeAtm = r), debounce = true)

    fun clearFilters() = setFilter(PsiFilter(query = _uiState.value.filter.query))

    fun toggleSelection(psiId: String) {
        _uiState.update {
            val sel = it.selectedIds
            it.copy(
                selectedIds = when {
                    psiId in sel -> sel - psiId
                    sel.size >= 3 -> sel
                    else -> sel + psiId
                }
            )
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptyList()) }
}
