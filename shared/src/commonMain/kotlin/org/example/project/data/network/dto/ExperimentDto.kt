package org.example.project.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.example.project.models.Experiment
import org.example.project.models.ExperimentChunk
import org.example.project.models.ExperimentSummary
import org.example.project.models.SourceType
import org.example.project.models.SummaryStatus

@Serializable
data class ExperimentDto(
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
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(summary: ExperimentSummary? = null): Experiment = Experiment(
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

@Serializable
data class ExperimentSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("experiment_id") val experimentId: String,
    @SerialName("ai_summary") val aiSummary: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_label") val riskLabel: String,
    @SerialName("key_finding") val keyFinding: String,
    @SerialName("status") val status: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): ExperimentSummary = ExperimentSummary(
        id = id,
        experimentId = experimentId,
        aiSummary = aiSummary,
        riskScore = riskScore,
        riskLabel = riskLabel,
        keyFinding = keyFinding,
        status = if (status.lowercase() == "insufficient_data") SummaryStatus.INSUFFICIENT_DATA else SummaryStatus.OK,
        createdAt = createdAt
    )
}

@Serializable
data class ExperimentChunkDto(
    @SerialName("id") val id: String,
    @SerialName("experiment_id") val experimentId: String,
    @SerialName("chunk_text") val chunkText: String,
    @SerialName("created_at") val createdAt: String? = null
) {
    fun toDomain(): ExperimentChunk = ExperimentChunk(
        id = id,
        experimentId = experimentId,
        chunkText = chunkText,
        createdAt = createdAt
    )
}
