package org.example.project.ui.psi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.GetEarthApplicationsUseCase
import org.example.project.domain.usecase.GetFireIncidentsUseCase
import org.example.project.domain.usecase.GetPsiInvestigationsUseCase
import org.example.project.models.EarthApplication
import org.example.project.models.FireIncident
import org.example.project.models.PsiInvestigation
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

data class HistoryEarthUiState(
    val isLoading: Boolean = true,
    val incidents: List<FireIncident> = emptyList(),
    val investigations: List<PsiInvestigation> = emptyList(),
    val earthApps: List<EarthApplication> = emptyList()
)

class HistoryEarthViewModel(
    private val getIncidents: GetFireIncidentsUseCase,
    private val getInvestigations: GetPsiInvestigationsUseCase,
    private val getEarthApps: GetEarthApplicationsUseCase
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryEarthUiState())
    val uiState: StateFlow<HistoryEarthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = HistoryEarthUiState(
                isLoading = false,
                incidents = getIncidents(),
                investigations = getInvestigations(),
                earthApps = getEarthApps()
            )
        }
    }
}

private enum class HistoryTab { TIMELINE, EARTH }

private sealed interface TimelineEntry {
    val year: Int
    data class Incident(val item: FireIncident) : TimelineEntry { override val year get() = item.year }
    data class Investigation(val item: PsiInvestigation) : TimelineEntry { override val year get() = item.startYear ?: 0 }
}

@Composable
fun HistoryEarthHubScreen(onAskAi: (String) -> Unit, modifier: Modifier = Modifier) {
    var detailId by remember { mutableStateOf<String?>(null) }
    val id = detailId
    if (id != null) {
        PsiDetailScreen(psiId = id, onBack = { detailId = null }, onOpenRelated = { detailId = it }, onAskAi = onAskAi, modifier = modifier)
    } else {
        HistoryEarthScreen(onOpen = { detailId = it }, modifier = modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryEarthScreen(
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HistoryEarthViewModel = koinInject()
) {
    val s by viewModel.uiState.collectAsState()
    val uri = LocalUriHandler.current
    var tab by remember { mutableStateOf(HistoryTab.TIMELINE) }
    var showIncidents by remember { mutableStateOf(true) }
    var showPsi by remember { mutableStateOf(true) }

    Column(modifier.fillMaxSize().background(LabBackground)) {
        FlameTabs(
            tabs = listOf(
                FlameTabItem(HistoryTab.TIMELINE, "Incidents & Experiment History", count = s.incidents.size + s.investigations.size),
                FlameTabItem(HistoryTab.EARTH, "Applications on Earth", count = s.earthApps.size)
            ),
            selectedTab = tab,
            onTabSelected = { tab = it }
        )
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (s.isLoading) {
                CircularProgressIndicator(color = AccentOrangeRed)
                return@Column
            }
            when (tab) {
                HistoryTab.TIMELINE -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SelectableChip("Space fire incidents", showIncidents, { showIncidents = !showIncidents }, RiskHighRed)
                        SelectableChip("PSI experiments", showPsi, { showPsi = !showPsi })
                    }
                    val entries = buildList {
                        if (showIncidents) addAll(s.incidents.map { TimelineEntry.Incident(it) })
                        if (showPsi) addAll(s.investigations.filter { it.startYear != null }.map { TimelineEntry.Investigation(it) })
                    }.sortedBy { it.year }
                    entries.forEach { e ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(Modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${e.year}", fontWeight = FontWeight.Black, color = TextDarkAnthracite, fontSize = 13.sp)
                                Box(
                                    Modifier.padding(top = 4.dp).size(14.dp).clip(CircleShape)
                                        .background(if (e is TimelineEntry.Incident) RiskHighRed else AccentTeal)
                                )
                            }
                            when (e) {
                                is TimelineEntry.Incident -> IncidentCard(e.item) { uri.openUri(e.item.sourceUrl) }
                                is TimelineEntry.Investigation -> FlameCard(Modifier.fillMaxWidth(), onClick = { onOpen(e.item.psiId) }) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            FlameSeriesBadge(e.item.displayName)
                                            CategoryBadge(e.item.safetyCategory)
                                            Text(listOfNotNull(e.item.startDate, e.item.endDate).joinToString(" → "), fontSize = 11.sp, color = TextMediumGray)
                                        }
                                        Text(e.item.title, fontSize = 13.sp, color = TextDarkAnthracite, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                    Text("${s.investigations.count { it.startYear == null }} PSI studies without dates are not shown on the timeline.", fontSize = 11.sp, color = TextLightMuted)
                }
                HistoryTab.EARTH -> {
                    NoticeBox(
                        "From NASA technology to Earth",
                        "Applications in fire detection, suppression and protective equipment that grew out of space research, with related PSI experiments.",
                        warning = false
                    )
                    s.earthApps.forEach { app ->
                        FlameSectionCard(title = app.title, action = { LinkText("Source") { uri.openUri(app.sourceUrl) } }) {
                            app.description?.let { Text(it, fontSize = 13.sp, color = TextDarkAnthracite) }
                            if (app.relatedPsiIds.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Related experiments:", fontSize = 12.sp, color = TextMediumGray)
                                    app.relatedPsiIds.forEach { pid -> LinkText(pid) { onOpen(pid) } }
                                }
                            }
                        }
                    }
                    val perInv = s.investigations.filter { it.earthApplications.isNotEmpty() }
                    if (perInv.isNotEmpty()) {
                        FlameSectionCard(title = "Earth Benefits by Experiment", subtitle = "${perInv.size} PSI experiments") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                perInv.forEach { inv ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        LinkText(inv.displayName) { onOpen(inv.psiId) }
                                        Text(inv.earthApplications.joinToString(" • "), fontSize = 12.sp, color = TextDarkAnthracite)
                                    }
                                }
                            }
                        }
                    }
                    NoticeBox(
                        "Scope note",
                        "Rocket and propellant fires are outside the scope of PSI data: because of the high velocities in rocket engines, combustion there is practically independent of gravity.",
                        warning = false
                    )
                }
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun IncidentCard(item: FireIncident, onSource: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(RiskHighRedBg)
            .border(BorderStroke(1.dp, RiskHighRed.copy(alpha = 0.3f)), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥 ${item.name}", fontWeight = FontWeight.Bold, color = RiskHighRed)
            item.mission?.let { Text(it, fontSize = 11.sp, color = TextMediumGray) }
        }
        item.cause?.let { Text("Cause: $it", fontSize = 12.sp, color = TextDarkAnthracite) }
        item.outcome?.let { Text("Outcome: $it", fontSize = 12.sp, color = TextDarkAnthracite) }
        item.lessons?.let { Text("Lesson: $it", fontSize = 12.sp, color = TextDarkAnthracite, fontWeight = FontWeight.Medium) }
        LinkText("Source (NASA)", onSource)
    }
}
