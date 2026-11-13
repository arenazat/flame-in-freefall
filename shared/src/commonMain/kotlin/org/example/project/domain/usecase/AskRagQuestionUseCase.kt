package org.example.project.domain.usecase

import org.example.project.domain.repository.RagRepository
import org.example.project.models.RagAnswer

class AskRagQuestionUseCase(
    private val ragRepository: RagRepository
) {
    suspend operator fun invoke(question: String): Result<RagAnswer> {
        return ragRepository.askQuestion(question)
    }
}
