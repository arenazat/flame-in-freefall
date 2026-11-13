package org.example.project.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportRequest(
    @SerialName("experiment_ids") val experimentIds: List<String>,
    @SerialName("title") val title: String? = null
)

@Serializable
data class ReportResult(
    @SerialName("report_id") val reportId: String = "",
    @SerialName("report_url") val reportUrl: String = "",
    @SerialName("filename") val filename: String = "",
    @SerialName("title") val title: String? = null,
    @SerialName("experiment_count") val experimentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("error") val error: String? = null
) {
    val displayTitle: String
        get() = title ?: filename.takeIf { it.isNotBlank() } ?: "NASA Deney Analiz Raporu"
}
