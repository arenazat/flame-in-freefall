package org.example.project.domain.usecase

import org.example.project.domain.repository.PsiRepository
import org.example.project.models.EarthApplication
import org.example.project.models.FireIncident
import org.example.project.models.HabitatProfile
import org.example.project.models.HabitatRankingItem
import org.example.project.models.PsiFilter
import org.example.project.models.PsiFilterOptions
import org.example.project.models.PsiInvestigation
import org.example.project.models.PsiPublication
import org.example.project.models.PsiTestCondition

class GetPsiInvestigationsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(filter: PsiFilter = PsiFilter()): List<PsiInvestigation> = repository.getInvestigations(filter)
}

class GetPsiInvestigationUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(psiId: String): PsiInvestigation? = repository.getInvestigation(psiId)
    suspend fun byIds(psiIds: List<String>): List<PsiInvestigation> = repository.getInvestigationsByIds(psiIds)
}

class GetPsiTestConditionsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(psiId: String): List<PsiTestCondition> = repository.getTestConditions(psiId)
}

class GetPsiPublicationsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(psiId: String): List<PsiPublication> = repository.getPublications(psiId)
}

class GetPsiFilterOptionsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(): PsiFilterOptions = repository.getFilterOptions()
}

class GetHabitatProfilesUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(): List<HabitatProfile> = repository.getHabitatProfiles()
}

class RankForHabitatUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(habitatId: String?, o2Percent: Double? = null, pressureAtm: Double? = null): List<HabitatRankingItem> =
        repository.rankForHabitat(habitatId, o2Percent, pressureAtm)
}

class GetFireIncidentsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(): List<FireIncident> = repository.getFireIncidents()
}

class GetEarthApplicationsUseCase(private val repository: PsiRepository) {
    suspend operator fun invoke(): List<EarthApplication> = repository.getEarthApplications()
}
