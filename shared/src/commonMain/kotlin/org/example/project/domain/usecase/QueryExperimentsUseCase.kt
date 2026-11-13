package org.example.project.domain.usecase

import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment

class QueryExperimentsUseCase(
    private val repository: ExperimentRepository
) {
    suspend operator fun invoke(query: String): List<Experiment> =
        repository.queryExperiments(query)
}
