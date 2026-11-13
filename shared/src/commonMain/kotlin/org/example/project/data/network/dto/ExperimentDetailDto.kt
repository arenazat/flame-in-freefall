package org.example.project.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.models.Experiment
import org.example.project.models.ExperimentSummary
import org.example.project.models.SourceType
import org.example.project.models.SummaryStatus

@Serializable
data class ExperimentDetailDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("series") val series: String,
    @SerialName("material") val material: String,
    @SerialName("gravity_level") val gravityLevel: String,
    @SerialName("oxygen_concentration") val oxygenConcentration: Double? = null,
    @SerialName("year") val year: Int? = null,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("raw_text") val rawText: String? = null,
    @SerialName("needs_manual_review") val needsManualReview: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("summary_id") val summaryId: String? = null,
    @SerialName("ai_summary") val aiSummary: String? = null,
    @SerialName("risk_score") val riskScore: Int? = null,
    @SerialName("risk_label") val riskLabel: String? = null,
    @SerialName("key_finding") val keyFinding: String? = null,
    @SerialName("summary_status") val summaryStatus: String? = null,
    @SerialName("one_g_value") val oneGValue: Double? = null,
    @SerialName("micro_g_value") val microGValue: Double? = null,
    @SerialName("comparison_unit") val comparisonUnit: String? = null,
    @SerialName("comparison_metric_name") val comparisonMetricName: String? = null,
    @SerialName("comparison_notes") val comparisonNotes: String? = null
) {
    fun toDomain(): Experiment {
        val summary = if (summaryId != null && aiSummary != null && riskScore != null) {
            ExperimentSummary(
                id = summaryId,
                experimentId = id,
                aiSummary = aiSummary,
                riskScore = riskScore,
                riskLabel = riskLabel ?: "Düşük Risk",
                keyFinding = keyFinding ?: "",
                status = if (summaryStatus?.lowercase() == "insufficient_data") {
                    SummaryStatus.INSUFFICIENT_DATA
                } else {
                    SummaryStatus.OK
                },
                createdAt = createdAt,
                oneGValue = oneGValue,
                microGValue = microGValue,
                comparisonUnit = comparisonUnit,
                comparisonMetricName = comparisonMetricName,
                comparisonNotes = comparisonNotes
            )
        } else null

        return Experiment(
            id = id,
            title = title,
            series = series,
            material = material,
            gravityLevel = gravityLevel,
            oxygenConcentration = oxygenConcentration,
            year = year,
            sourceUrl = sourceUrl,
            sourceType = if (sourceType.lowercase() == "ntrs") SourceType.NTRS else SourceType.PSI,
            rawText = rawText,
            needsManualReview = needsManualReview,
            createdAt = createdAt,
            summary = summary
        )
    }
}
