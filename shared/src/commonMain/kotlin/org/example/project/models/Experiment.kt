package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
enum class SourceType(val value: String) {
    PSI("psi"),
    NTRS("ntrs")
}

@Serializable
enum class SummaryStatus(val value: String) {
    OK("ok"),
    INSUFFICIENT_DATA("insufficient_data")
}

@Serializable
data class Experiment(
    val id: String,
    val title: String,
    val series: String,
    val material: String,
    val gravityLevel: String,
    val oxygenConcentration: Double?,
    val year: Int?,
    val sourceUrl: String,
    val sourceType: SourceType,
    val rawText: String? = null,
    val needsManualReview: Boolean = false,
    val createdAt: String? = null,
    val summary: ExperimentSummary? = null
)

@Serializable
data class ExperimentSummary(
    val id: String,
    val experimentId: String,
    val aiSummary: String,
    val riskScore: Int,
    val riskLabel: String,
    val keyFinding: String,
    val status: SummaryStatus,
    val createdAt: String? = null,
    val oneGValue: Double? = null,
    val microGValue: Double? = null,
    val comparisonUnit: String? = null,
    val comparisonMetricName: String? = null,
    val comparisonNotes: String? = null
) {
    val hasComparativeData: Boolean
        get() = oneGValue != null && microGValue != null && oneGValue > 0.0 && microGValue > 0.0
}

@Serializable
data class ExperimentChunk(
    val id: String,
    val experimentId: String,
    val chunkText: String,
    val createdAt: String? = null
)

@Serializable
data class RagResponse(
    val answer: String,
    val sources: List<RagSourceItem> = emptyList(),
    val matchCount: Int = 0,
    val error: String? = null
)

@Serializable
data class RagSourceItem(
    val chunkId: String = "",
    val experimentId: String = "",
    val experimentTitle: String = "",
    val series: String = "",
    val sourceUrl: String = "",
    val similarity: Double = 0.0,
    val previewText: String = ""
)
