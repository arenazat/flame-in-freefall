package org.example.project.models

import kotlinx.serialization.Serializable

@Serializable
data class ExperimentFilter(
    val query: String? = null,
    val series: String? = null,
    val material: String? = null,
    val gravityLevel: String? = null,
    val minYear: Int? = null,
    val maxYear: Int? = null,
    val minRiskScore: Int? = null,
    val maxRiskScore: Int? = null
)

@Serializable
data class FilterOptions(
    val materials: List<String> = emptyList(),
    val series: List<String> = emptyList(),
    val gravityLevels: List<String> = emptyList(),
    val minYear: Int? = null,
    val maxYear: Int? = null
)
