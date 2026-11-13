package org.example.project.ingestion.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenAiChatRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<OpenAiChatMessage>,
    @SerialName("response_format") val responseFormat: OpenAiResponseFormat? = OpenAiResponseFormat("json_object"),
    val temperature: Double = 0.2
)

@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenAiResponseFormat(
    val type: String
)

@Serializable
data class OpenAiChatResponse(
    val choices: List<OpenAiChatChoice>? = null
)

@Serializable
data class OpenAiChatChoice(
    val message: OpenAiChatMessage? = null
)

@Serializable
data class LlmSummaryResult(
    val status: String = "ok", // "ok" or "insufficient_data"
    @SerialName("ai_summary") val aiSummary: String = "",
    @SerialName("key_finding") val keyFinding: String = "",
    @SerialName("safety_implication") val safetyImplication: String = "",
    @SerialName("risk_score") val riskScore: Int = 0,
    @SerialName("risk_label") val riskLabel: String = "Düşük Risk",
    @SerialName("one_g_value") val oneGValue: Double? = null,
    @SerialName("micro_g_value") val microGValue: Double? = null,
    @SerialName("comparison_unit") val comparisonUnit: String? = null,
    @SerialName("comparison_metric_name") val comparisonMetricName: String? = null,
    @SerialName("comparison_notes") val comparisonNotes: String? = null
)

@Serializable
data class ExperimentSummaryInsertDto(
    @SerialName("experiment_id") val experimentId: String,
    @SerialName("ai_summary") val aiSummary: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_label") val riskLabel: String,
    @SerialName("key_finding") val keyFinding: String,
    @SerialName("status") val status: String,
    @SerialName("one_g_value") val oneGValue: Double? = null,
    @SerialName("micro_g_value") val microGValue: Double? = null,
    @SerialName("comparison_unit") val comparisonUnit: String? = null,
    @SerialName("comparison_metric_name") val comparisonMetricName: String? = null,
    @SerialName("comparison_notes") val comparisonNotes: String? = null
)

@Serializable
data class ExistingSummaryReference(
    @SerialName("experiment_id") val experimentId: String
)
