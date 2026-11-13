package org.example.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.example.project.models.Experiment
import org.example.project.models.ExperimentFilter
import org.example.project.models.FilterOptions

import org.example.project.models.RagResponse

interface ExperimentRepository {
    fun getExperiments(filter: ExperimentFilter = ExperimentFilter()): Flow<List<Experiment>>
    fun getRiskRankings(filter: ExperimentFilter = ExperimentFilter()): Flow<List<Experiment>>
    suspend fun getFilterOptions(): FilterOptions
    suspend fun queryExperiments(naturalLanguageQuery: String): List<Experiment>
    suspend fun askRagQuestion(question: String): RagResponse
    fun isSupabaseConnected(): Boolean
}
