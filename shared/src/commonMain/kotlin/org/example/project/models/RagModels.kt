package org.example.project.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RagQuestionRequest(
    @SerialName("question") val question: String,
    @SerialName("match_threshold") val matchThreshold: Double = 0.25,
    @SerialName("match_count") val matchCount: Int = 8
)

@Serializable
data class RagSourceReference(
    @SerialName("experimentId") val experimentId: String,
    @SerialName("title") val title: String,
    @SerialName("series") val series: String,
    @SerialName("sourceUrl") val sourceUrl: String,
    @SerialName("similarity") val similarity: Double = 0.0,
    @SerialName("excerpt") val excerpt: String = ""
)

@Serializable
data class RagAnswer(
    @SerialName("answer") val answer: String,
    @SerialName("sources") val sources: List<RagSourceReference> = emptyList(),
    @SerialName("status") val status: String = "ok",
    @SerialName("matchCount") val matchCount: Int = 0,
    @SerialName("error") val error: String? = null
) {
    val isInsufficientData: Boolean
        get() = status == "insufficient_data" || sources.isEmpty()
}
