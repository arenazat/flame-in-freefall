package org.example.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.Experiment
import org.example.project.models.ExperimentFilter

class GetSimilarExperimentsUseCase(
    private val repository: ExperimentRepository
) {
    /**
     * Hedef deneyle aynı serideki veya malzemedeki GERÇEK kayıtları Supabase'den sorgular.
     */
    operator fun invoke(target: Experiment): Flow<List<Experiment>> {
        val filter = ExperimentFilter(series = target.series)
        return repository.getExperiments(filter).map { list ->
            list.filter { it.id != target.id }.take(5)
        }
    }
}
