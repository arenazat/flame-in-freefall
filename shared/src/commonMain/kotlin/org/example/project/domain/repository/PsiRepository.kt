package org.example.project.domain.repository

import org.example.project.models.EarthApplication
import org.example.project.models.FireIncident
import org.example.project.models.HabitatProfile
import org.example.project.models.HabitatRankingItem
import org.example.project.models.PsiFilter
import org.example.project.models.PsiFilterOptions
import org.example.project.models.PsiInvestigation
import org.example.project.models.PsiPublication
import org.example.project.models.PsiTestCondition

interface PsiRepository {
    suspend fun getInvestigations(filter: PsiFilter = PsiFilter()): List<PsiInvestigation>
    suspend fun getInvestigation(psiId: String): PsiInvestigation?
    suspend fun getInvestigationsByIds(psiIds: List<String>): List<PsiInvestigation>
    suspend fun getTestConditions(psiId: String): List<PsiTestCondition>
    suspend fun getPublications(psiId: String): List<PsiPublication>
    suspend fun getFilterOptions(): PsiFilterOptions
    suspend fun getHabitatProfiles(): List<HabitatProfile>
    suspend fun rankForHabitat(habitatId: String?, o2Percent: Double? = null, pressureAtm: Double? = null): List<HabitatRankingItem>
    suspend fun getFireIncidents(): List<FireIncident>
    suspend fun getEarthApplications(): List<EarthApplication>
}
