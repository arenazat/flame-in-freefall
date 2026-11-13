package org.example.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment

class GetRiskRankingsUseCase(
    private val repository: ExperimentRepository
) {
    operator fun invoke(): Flow<List<Experiment>> = repository.getRiskRankings()
}
