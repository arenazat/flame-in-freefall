package org.example.project.domain.usecase

import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.FilterOptions

class GetFilterOptionsUseCase(
    private val repository: ExperimentRepository
) {
    suspend operator fun invoke(): FilterOptions = repository.getFilterOptions()
}
