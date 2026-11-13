package org.example.project.ui.psi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.example.project.domain.usecase.GetPsiInvestigationUseCase
import org.example.project.models.HabitatKind
import org.example.project.models.PsiInvestigation
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

data class PsiCompareUiState(val isLoading: Boolean = true, val items: List<PsiInvestigation> = emptyList())

class PsiCompareViewModel(private val getInvestigation: GetPsiInvestigationUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(PsiCompareUiState())
    val uiState: StateFlow<PsiCompareUiState> = _uiState.asStateFlow()
    private var loaded: List<String>? = null

    fun load(ids: List<String>) {
        if (loaded == ids) return
        loaded = ids
        _uiState.value = PsiCompareUiState(isLoading = true)
        viewModelScope.launch { _uiState.value = PsiCompareUiState(false, getInvestigation.byIds(ids)) }
    }
}

private data class CompareRow(val label: String, val values: List<String>) {
    val allSame: Boolean get() = values.distinct().size == 1
}

private fun buildRows(items: List<PsiInvestigation>): List<CompareRow> {
    fun row(label: String, f: (PsiInvestigation) -> String?) = CompareRow(label, items.map { f(it)?.ifBlank { null } ?: "—" })
    return listOf(
        row("Safety category") { it.safetyCategory?.label },
        row("Fuel state") { it.fuelState?.label },
        row("Fuels") { it.fuels.joinToString(", ") },
        row("Diluents") { it.diluents.joinToString(", ") },
        row("Sample / burner") { it.sampleGeometry },
        row("O₂ range") { it.o2Text() },
        row("Pressure range") { it.pressureText() },
        row("Air flow") { it.flowText() },
        row("Sensors") { it.sensors.joinToString(", ") },
        row("Hardware group") { it.hardwareGroup },
        row("Platform") { if (it.isGroundStudy) "Ground study" else it.platform },
        row("Start year") { it.startYear?.toString() },
        row("Test rows") { it.testCount.toString() },
        row("Publications") { it.publicationCount.toString() }
    ) + HabitatKind.entries.map { k -> row("Relevance: ${k.label}") { it.relevanceFor(k)?.let { v -> "$v/10" } } }
}

@Composable
fun PsiCompareScreen(
    psiIds: List<String>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PsiCompareViewModel = koinInject()
) {
    LaunchedEffect(psiIds) { viewModel.load(psiIds) }
    val state by viewModel.uiState.collectAsState()
    val items = state.items
    val rows = remember(items) { buildRows(items) }

    Column(
        modifier.fillMaxSize().background(LabBackground).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FlameOutlinedButton("← Back", onClick = onBack)
            Text("Experiment Comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
        }
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) { CircularProgressIndicator(color = AccentOrangeRed) }
            return@Column
        }
        if (items.size < 2) {
            NoticeBox("Not enough selected", "Select at least 2 PSI experiments to compare.")
            return@Column
        }

        val common = rows.filter { it.allSame && it.values.first() != "—" }
        val diffs = rows.filter { !it.allSame }
        FlameSectionCard(title = "Summary", subtitle = "${common.size} shared traits • ${diffs.size} differences") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (common.isNotEmpty()) {
                    Text("Shared: " + common.joinToString(" • ") { "${it.label}: ${it.values.first()}" }, fontSize = 13.sp, color = RiskLowGreen)
                }
                Text("Different: " + diffs.joinToString(", ") { it.label }, fontSize = 13.sp, color = AccentOrangeRed)
                FlamePrimaryButton("✦ AI comparison summary", onClick = {
                    onAskAi(
                        "Compare these NASA PSI experiments from a fire-safety perspective; explain their similarities, differences and implications for space habitats: " +
                            items.joinToString("; ") { "${it.psiId} ${it.displayName} (${it.title})" }
                    )
                })
            }
        }

        val colW = 240.dp
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(LabSurface)
                .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(10.dp)).horizontalScroll(rememberScrollState())
        ) {
            Column {
                Row(Modifier.background(LabSurfaceSubtle)) {
                    Text("Attribute", Modifier.width(170.dp).padding(10.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextMediumGray)
                    items.forEach { inv ->
                        Column(Modifier.width(colW).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            FlameSeriesBadge(inv.displayName)
                            Text(inv.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkAnthracite, maxLines = 3)
                            LinkText("Open details →") { onOpen(inv.psiId) }
                        }
                    }
                }
                rows.forEach { r ->
                    Row(Modifier.border(BorderStroke(0.5.dp, LabBorderSubtle))) {
                        Row(Modifier.width(170.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (r.allSame) "✓ " else "≠ ", color = if (r.allSame) RiskLowGreen else AccentOrangeRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(r.label, fontSize = 12.sp, color = TextMediumGray)
                        }
                        r.values.forEach { v ->
                            Text(
                                v,
                                Modifier.width(colW).background(if (r.allSame) LabSurface else AccentOrangeRedMuted.copy(alpha = 0.45f)).padding(10.dp),
                                fontSize = 12.sp, color = TextDarkAnthracite
                            )
                        }
                    }
                }
            }
        }
        Text(HABITAT_SCORE_NOTE, fontSize = 11.sp, color = TextLightMuted)
        Spacer(Modifier.height(80.dp))
    }
}
