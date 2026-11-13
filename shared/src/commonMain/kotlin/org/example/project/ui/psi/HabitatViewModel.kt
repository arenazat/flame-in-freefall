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
import org.example.project.domain.usecase.GetHabitatProfilesUseCase
import org.example.project.domain.usecase.GetPsiInvestigationsUseCase
import org.example.project.domain.usecase.RankForHabitatUseCase
import org.example.project.models.HabitatProfile
import org.example.project.models.HabitatRankingItem
import org.example.project.models.PsiInvestigation

const val PSI_PER_ATM = 14.696

data class HabitatUiState(
    val isLoading: Boolean = true,
    val profiles: List<HabitatProfile> = emptyList(),
    val selectedProfileId: String? = "lunar_base",
    val o2Percent: Double = 30.0,
    val pressurePsi: Double = 10.2,
    val isCustom: Boolean = false,
    val ranking: List<HabitatRankingItem> = emptyList(),
    val investigations: List<PsiInvestigation> = emptyList()
) {
    val selectedProfile: HabitatProfile? get() = profiles.firstOrNull { it.id == selectedProfileId }
    val pressureAtm: Double get() = pressurePsi / PSI_PER_ATM
    val title: String
        get() = if (isCustom) "Custom atmosphere" else selectedProfile?.name ?: "Habitat"
}

class HabitatViewModel(
    private val getProfiles: GetHabitatProfilesUseCase,
    private val rank: RankForHabitatUseCase,
    private val getInvestigations: GetPsiInvestigationsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HabitatUiState())
    val uiState: StateFlow<HabitatUiState> = _uiState.asStateFlow()
    private var job: Job? = null

    init {
        viewModelScope.launch {
            val profiles = getProfiles()
            val invs = getInvestigations()
            val initial = profiles.firstOrNull { it.id == "lunar_base" } ?: profiles.firstOrNull()
            _uiState.update {
                it.copy(
                    profiles = profiles,
                    investigations = invs,
                    selectedProfileId = initial?.id,
                    o2Percent = initial?.o2Percent ?: it.o2Percent,
                    pressurePsi = initial?.pressurePsi ?: it.pressurePsi
                )
            }
            refresh(0)
        }
    }

    fun selectProfile(p: HabitatProfile) {
        _uiState.update { it.copy(selectedProfileId = p.id, o2Percent = p.o2Percent, pressurePsi = p.pressurePsi, isCustom = false) }
        refresh(0)
    }

    fun setO2(v: Double) {
        _uiState.update { it.copy(o2Percent = v, isCustom = true) }
        refresh()
    }

    fun setPressurePsi(v: Double) {
        _uiState.update { it.copy(pressurePsi = v, isCustom = true) }
        refresh()
    }

    private fun refresh(debounceMs: Long = 250) {
        job?.cancel()
        job = viewModelScope.launch {
            if (debounceMs > 0) delay(debounceMs)
            _uiState.update { it.copy(isLoading = true) }
            val s = _uiState.value
            // Özel atmosferde ilgi puanı için en yakın ön ayarın habitat türü kullanılır.
            val habitatId = s.selectedProfileId
            val list = rank(habitatId, s.o2Percent, s.pressureAtm)
            _uiState.update { it.copy(isLoading = false, ranking = list) }
        }
    }
}
