package org.example.project.domain.usecase

import org.example.project.domain.repository.ReportRepository
import org.example.project.models.ReportResult

class GenerateReportUseCase(
    private val reportRepository: ReportRepository
) {
    suspend operator fun invoke(experimentIds: List<String>, title: String? = null): Result<ReportResult> {
        return reportRepository.generateReport(experimentIds, title)
    }
}
