package org.example.project.models

enum class NavTab(
    val title: String,
    val subtitle: String
) {
    PSI_CATALOG(
        title = "PSI Experiments",
        subtitle = "NASA PSI Combustion Experiment Catalog"
    ),
    HABITAT(
        title = "Habitat Panel",
        subtitle = "Moon / Mars / Spacecraft Fire-Safety Ranking"
    ),
    HISTORY_EARTH(
        title = "History & Earth",
        subtitle = "Space Fire Incidents and Applications on Earth"
    ),
    EXPLORE(
        title = "NTRS Papers",
        subtitle = "NASA Technical Report Catalog"
    ),
    TIMELINE(
        title = "Timeline",
        subtitle = "Experiment Chronology by Year"
    ),
    RISK_RANKING(
        title = "Material Risk",
        subtitle = "Material-Based Flammability Risk Analysis"
    ),
    ABOUT(
        title = "About",
        subtitle = "Microgravity Combustion Research"
    )
}
