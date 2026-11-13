package org.example.project.ingestion.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NtrsSearchResponse(
    val results: List<NtrsCitationItem>? = null
)

@Serializable
data class NtrsCitationItem(
    val id: Long,
    val title: String,
    val abstract: String? = null,
    val distributionDate: String? = null,
    val created: String? = null,
    val modified: String? = null,
    val keywords: List<String>? = null,
    val downloads: List<NtrsDownloadItem>? = null
)

@Serializable
data class NtrsDownloadItem(
    val mimetype: String? = null,
    val links: NtrsLinksItem? = null
)

@Serializable
data class NtrsLinksItem(
    val pdf: String? = null,
    val original: String? = null
)

@Serializable
data class ExperimentInsertDto(
    @SerialName("title") val title: String,
    @SerialName("series") val series: String,
    @SerialName("material") val material: String,
    @SerialName("gravity_level") val gravityLevel: String,
    @SerialName("oxygen_concentration") val oxygenConcentration: Double?,
    @SerialName("year") val year: Int?,
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("source_type") val sourceType: String,
    @SerialName("raw_text") val rawText: String?,
    @SerialName("needs_manual_review") val needsManualReview: Boolean
)
