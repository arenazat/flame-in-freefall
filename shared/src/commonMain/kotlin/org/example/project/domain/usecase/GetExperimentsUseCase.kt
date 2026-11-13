package org.example.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment

import org.example.project.models.ExperimentFilter

class GetExperimentsUseCase(
    private val repository: ExperimentRepository
) {
    operator fun invoke(filter: ExperimentFilter = ExperimentFilter()): Flow<List<Experiment>> =
        repository.getExperiments(filter)
}
