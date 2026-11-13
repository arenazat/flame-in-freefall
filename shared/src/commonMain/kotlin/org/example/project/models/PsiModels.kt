package org.example.project.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** NASA PSI yanma deneyleri — supabase/migrations/20261113000000_psi_investigations.sql */

enum class SafetyCategory(val value: String, val label: String) {
    PREVENTION("prevention", "Prevention"),
    DETECTION("detection", "Detection"),
    SUPPRESSION("suppression", "Suppression"),
    FUNDAMENTAL("fundamental", "Fundamental Research"),
    MODELING("modeling", "Modeling");

    companion object {
        fun from(value: String?): SafetyCategory? = entries.firstOrNull { it.value == value }
    }
}

enum class FuelState(val value: String, val label: String) {
    SOLID("solid", "Solid"),
    LIQUID_DROPLET("liquid_droplet", "Liquid droplet"),
    GAS("gas", "Gas"),
    AEROSOL("aerosol", "Aerosol");

    companion object {
        fun from(value: String?): FuelState? = entries.firstOrNull { it.value == value }
    }
}

enum class HabitatKind(val label: String) {
    SPACECRAFT("Spacecraft"),
    STATION("Space station"),
    LUNAR_BASE("Lunar base"),
    MARS_BASE("Mars base"),
    ROVER("Pressurized rover")
}

@Serializable
data class PsiRelationRef(
    @SerialName("psi_id") val psiId: String,
    @SerialName("type") val type: String
) {
    val typeLabel: String
        get() = when (type) {
            "analyzes_data_of" -> "analyzes data of"
            "follow_on_of" -> "follow-on study"
            "reflight_of" -> "reflight"
            "risk_mitigation_for" -> "risk-mitigation study"
            "shares_hardware" -> "shared hardware"
            else -> type
        }
}

@Serializable
data class PsiInvestigation(
    @SerialName("psi_id") val psiId: String,
    @SerialName("acronym") val acronym: String? = null,
    @SerialName("title") val title: String,
    @SerialName("project_type") val projectType: String? = null,
    @SerialName("sub_research_area") val subResearchArea: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("hardware") val hardware: String? = null,
    @SerialName("hardware_group") val hardwareGroup: String? = null,
    @SerialName("start_date") val startDate: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("start_year") val startYear: Int? = null,
    @SerialName("doi") val doi: String? = null,
    @SerialName("data_size_bytes") val dataSizeBytes: Long? = null,
    @SerialName("nasa_center") val nasaCenter: String? = null,
    @SerialName("objective") val objective: String? = null,
    @SerialName("approach") val approach: String? = null,
    @SerialName("hypothesis") val hypothesis: String? = null,
    @SerialName("research_impacts") val researchImpacts: String? = null,
    @SerialName("safety_category") val safetyCategoryRaw: String? = null,
    @SerialName("fuel_state") val fuelStateRaw: String? = null,
    @SerialName("fuels") val fuels: List<String> = emptyList(),
    @SerialName("diluents") val diluents: List<String> = emptyList(),
    @SerialName("sample_geometry") val sampleGeometry: String? = null,
    @SerialName("o2_min") val o2Min: Double? = null,
    @SerialName("o2_max") val o2Max: Double? = null,
    @SerialName("pressure_min_atm") val pressureMinAtm: Double? = null,
    @SerialName("pressure_max_atm") val pressureMaxAtm: Double? = null,
    @SerialName("flow_min_cms") val flowMinCms: Double? = null,
    @SerialName("flow_max_cms") val flowMaxCms: Double? = null,
    @SerialName("sensors") val sensors: List<String> = emptyList(),
    @SerialName("relevance_spacecraft") val relevanceSpacecraft: Int? = null,
    @SerialName("relevance_station") val relevanceStation: Int? = null,
    @SerialName("relevance_lunar_base") val relevanceLunarBase: Int? = null,
    @SerialName("relevance_mars_base") val relevanceMarsBase: Int? = null,
    @SerialName("relevance_rover") val relevanceRover: Int? = null,
    @SerialName("relevance_note") val relevanceNote: String? = null,
    @SerialName("earth_applications") val earthApplications: List<String> = emptyList(),
    @SerialName("source_url") val sourceUrl: String,
    @SerialName("test_count") val testCount: Int = 0,
    @SerialName("publication_count") val publicationCount: Int = 0,
    @SerialName("related_to") val relatedTo: List<PsiRelationRef> = emptyList(),
    @SerialName("related_from") val relatedFrom: List<PsiRelationRef> = emptyList()
) {
    val safetyCategory: SafetyCategory? get() = SafetyCategory.from(safetyCategoryRaw)
    val fuelState: FuelState? get() = FuelState.from(fuelStateRaw)
    val displayName: String get() = acronym?.takeIf { it.isNotBlank() } ?: psiId
    val isGroundStudy: Boolean get() = projectType == "Ground Investigation"
    val doiUrl: String? get() = doi?.takeIf { it.isNotBlank() }?.let { "https://doi.org/$it" }

    fun relevanceFor(kind: HabitatKind): Int? = when (kind) {
        HabitatKind.SPACECRAFT -> relevanceSpacecraft
        HabitatKind.STATION -> relevanceStation
        HabitatKind.LUNAR_BASE -> relevanceLunarBase
        HabitatKind.MARS_BASE -> relevanceMarsBase
        HabitatKind.ROVER -> relevanceRover
    }

    /** Test aralığı verilen aralıkla kesişiyor mu (aralık bilinmiyorsa filtre dışı bırakılmaz). */
    fun overlapsO2(min: Double, max: Double): Boolean =
        o2Min == null || o2Max == null || (o2Max >= min && o2Min <= max)

    fun overlapsPressure(min: Double, max: Double): Boolean =
        pressureMinAtm == null || pressureMaxAtm == null || (pressureMaxAtm >= min && pressureMinAtm <= max)
}

@Serializable
data class PsiTestCondition(
    @SerialName("id") val id: Long,
    @SerialName("psi_id") val psiId: String,
    @SerialName("source_file") val sourceFile: String,
    @SerialName("row_index") val rowIndex: Int,
    @SerialName("data") val data: JsonObject
)

@Serializable
data class PsiPublication(
    @SerialName("id") val id: Long,
    @SerialName("psi_id") val psiId: String,
    @SerialName("title") val title: String,
    @SerialName("doi") val doi: String? = null
) {
    val doiUrl: String? get() = doi?.takeIf { it.isNotBlank() }?.let { "https://doi.org/$it" }
}

@Serializable
data class HabitatProfile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("o2_percent") val o2Percent: Double,
    @SerialName("pressure_psi") val pressurePsi: Double,
    @SerialName("pressure_atm") val pressureAtm: Double,
    @SerialName("gravity_g") val gravityG: Double? = null,
    @SerialName("note") val note: String? = null
)

@Serializable
data class HabitatRankingItem(
    @SerialName("psi_id") val psiId: String,
    @SerialName("acronym") val acronym: String? = null,
    @SerialName("title") val title: String,
    @SerialName("safety_category") val safetyCategoryRaw: String? = null,
    @SerialName("fuel_state") val fuelStateRaw: String? = null,
    @SerialName("o2_min") val o2Min: Double? = null,
    @SerialName("o2_max") val o2Max: Double? = null,
    @SerialName("pressure_min_atm") val pressureMinAtm: Double? = null,
    @SerialName("pressure_max_atm") val pressureMaxAtm: Double? = null,
    @SerialName("relevance") val relevance: Int? = null,
    @SerialName("covers_o2") val coversO2: Boolean = false,
    @SerialName("covers_pressure") val coversPressure: Boolean = false,
    @SerialName("score") val score: Double = 0.0,
    @SerialName("relevance_note") val relevanceNote: String? = null,
    @SerialName("source_url") val sourceUrl: String
) {
    val safetyCategory: SafetyCategory? get() = SafetyCategory.from(safetyCategoryRaw)
    val displayName: String get() = acronym?.takeIf { it.isNotBlank() } ?: psiId
}

@Serializable
data class FireIncident(
    @SerialName("id") val id: Long,
    @SerialName("name") val name: String,
    @SerialName("year") val year: Int,
    @SerialName("mission") val mission: String? = null,
    @SerialName("cause") val cause: String? = null,
    @SerialName("outcome") val outcome: String? = null,
    @SerialName("lessons") val lessons: String? = null,
    @SerialName("source_url") val sourceUrl: String
)

@Serializable
data class EarthApplication(
    @SerialName("id") val id: Long,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("related_psi_ids") val relatedPsiIds: List<String> = emptyList(),
    @SerialName("source_url") val sourceUrl: String
)

data class PsiFilter(
    val query: String? = null,
    val categories: Set<SafetyCategory> = emptySet(),
    val fuelStates: Set<FuelState> = emptySet(),
    val hardwareGroups: Set<String> = emptySet(),
    val flightOnly: Boolean? = null,          // true: uçuş, false: yer çalışması, null: hepsi
    val o2Range: ClosedFloatingPointRange<Double>? = null,
    val pressureRangeAtm: ClosedFloatingPointRange<Double>? = null
)

data class PsiFilterOptions(
    val categories: List<SafetyCategory> = emptyList(),
    val fuelStates: List<FuelState> = emptyList(),
    val hardwareGroups: List<String> = emptyList(),
    val o2Bounds: ClosedFloatingPointRange<Double>? = null,
    val pressureBoundsAtm: ClosedFloatingPointRange<Double>? = null
)
