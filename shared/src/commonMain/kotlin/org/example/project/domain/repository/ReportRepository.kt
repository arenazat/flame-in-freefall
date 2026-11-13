package org.example.project.domain.repository

import org.example.project.models.ReportResult

interface ReportRepository {
    suspend fun generateReport(experimentIds: List<String>, title: String? = null): Result<ReportResult>
}
