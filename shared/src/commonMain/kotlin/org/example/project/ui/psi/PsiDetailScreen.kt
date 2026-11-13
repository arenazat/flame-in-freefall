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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.HabitatKind
import org.example.project.models.PsiInvestigation
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PsiDetailScreen(
    psiId: String,
    onBack: () -> Unit,
    onOpenRelated: (String) -> Unit,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PsiDetailViewModel = koinInject()
) {
    LaunchedEffect(psiId) { viewModel.load(psiId) }
    val state by viewModel.uiState.collectAsState()
    val uri = LocalUriHandler.current

    Column(
        modifier.fillMaxSize().background(LabBackground).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        FlameOutlinedButton("← Back", onClick = onBack)
        val inv = state.investigation
        when {
            state.isLoading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                CircularProgressIndicator(color = AccentOrangeRed)
            }
            inv == null -> NoticeBox("Experiment not found", "No Supabase record for $psiId.")
            else -> {
                // 1. Başlık şeridi
                FlameCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            FlameSeriesBadge(inv.displayName)
                            InfoChip(inv.psiId)
                            CategoryBadge(inv.safetyCategory)
                            inv.fuelState?.let { InfoChip(it.label) }
                        }
                        Text(inv.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            MetaItem("Type", inv.projectType)
                            MetaItem("Platform", inv.platform)
                            MetaItem("Hardware", inv.hardware)
                            MetaItem("Dates", listOfNotNull(inv.startDate, inv.endDate).joinToString(" → ").ifBlank { null })
                            MetaItem("NASA center", inv.nasaCenter)
                            MetaItem("Data size", formatBytes(inv.dataSizeBytes))
                            MetaItem("Sub-area", inv.subResearchArea)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FlameSecondaryButton("Open in NASA PSI", onClick = { uri.openUri(inv.sourceUrl) })
                            inv.doiUrl?.let { FlameOutlinedButton("DOI: ${inv.doi}", onClick = { uri.openUri(it) }) }
                            FlamePrimaryButton("✦ Ask AI about this experiment", onClick = {
                                onAskAi("What are the key fire-safety findings of ${inv.psiId} ${inv.displayName} (${inv.title}) for spacecraft and lunar/Mars habitats?")
                            })
                        }
                        Text(
                            "Citation: PSI data must be cited by DOI when used (NASA SMD Open Source Science).",
                            fontSize = 11.sp, color = TextLightMuted
                        )
                    }
                }

                // 2. Test koşulları
                FlameSectionCard(title = "Test Conditions", subtitle = "Fuel, atmosphere, flow and diagnostics") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KeyValue("Fuels", inv.fuels.joinToString(", ").ifBlank { null })
                        KeyValue("Diluents", inv.diluents.joinToString(", ").ifBlank { null })
                        KeyValue("Sample / burner", inv.sampleGeometry)
                        KeyValue("O₂", inv.o2Text())
                        KeyValue("Pressure", inv.pressureText())
                        KeyValue("Air flow", inv.flowText())
                        KeyValue("Hardware group", inv.hardwareGroup)
                        if (inv.sensors.isNotEmpty()) {
                            Text("Sensors / measurements", fontSize = 12.sp, color = TextMediumGray)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                inv.sensors.forEach { InfoChip(it, emphasized = true) }
                            }
                        }
                    }
                }

                // 3. Habitat uygunluğu
                FlameSectionCard(title = "Habitat Relevance", subtitle = "Fire-safety relevance for crewed environments") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitatKind.entries.forEach { RelevanceBar(it.label, inv.relevanceFor(it)) }
                        inv.relevanceNote?.let { Text(it, fontSize = 13.sp, color = TextDarkAnthracite) }
                        Text(HABITAT_SCORE_NOTE, fontSize = 11.sp, color = TextLightMuted)
                    }
                }

                // 4. Metin bölümleri
                TextSection("Objectives", inv.objective)
                TextSection("Approach", inv.approach)
                TextSection("Hypothesis", inv.hypothesis)
                TextSection("Research Impacts / Earth Benefits", inv.researchImpacts)
                if (inv.earthApplications.isNotEmpty()) {
                    FlameSectionCard(title = "Applications on Earth") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            inv.earthApplications.forEach { InfoChip(it) }
                        }
                    }
                }

                // 5. Deney matrisi
                TestMatrixCard(state.tables)

                // 6. İlişkili deneyler
                val related = inv.relatedTo.map { it to true } + inv.relatedFrom.map { it to false }
                if (related.isNotEmpty()) {
                    FlameSectionCard(title = "Related Experiments", subtitle = "Ground studies, follow-on experiments and shared hardware") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            related.forEach { (ref, outgoing) ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LinkText(ref.psiId) { onOpenRelated(ref.psiId) }
                                    Text(
                                        if (outgoing) "← this experiment: ${ref.typeLabel}" else "→ ${ref.typeLabel} (of this experiment)",
                                        fontSize = 12.sp, color = TextMediumGray
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Yayınlar
                FlameSectionCard(title = "Publications", subtitle = "${state.publications.size} publications") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.publications.forEach { p ->
                            Column {
                                Text(p.title, fontSize = 13.sp, color = TextDarkAnthracite)
                                p.doiUrl?.let { url -> LinkText("doi.org/${p.doi}") { uri.openUri(url) } }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Column {
        Text(label, fontSize = 11.sp, color = TextLightMuted)
        Text(value, fontSize = 13.sp, color = TextDarkAnthracite, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun KeyValue(label: String, value: String?) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, Modifier.width(140.dp), fontSize = 13.sp, color = TextMediumGray)
        Text(value ?: "Not specified in the PSI record", fontSize = 13.sp, color = if (value == null) TextLightMuted else TextDarkAnthracite)
    }
}

@Composable
private fun TextSection(title: String, text: String?) {
    if (text.isNullOrBlank()) return
    var expanded by remember(title) { mutableStateOf(false) }
    val toggle: (@Composable () -> Unit)? = if (text.length > 500) {
        { LinkText(if (expanded) "Show less" else "Show all") { expanded = !expanded } }
    } else null
    FlameSectionCard(title = title, action = toggle) {
        Text(
            text, fontSize = 14.sp, color = TextDarkAnthracite, lineHeight = 20.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 8, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TestMatrixCard(tables: List<TestTable>) {
    val clipboard = LocalClipboardManager.current
    var selected by remember(tables) { mutableStateOf(0) }
    var page by remember(tables, selected) { mutableStateOf(0) }
    var copied by remember { mutableStateOf<String?>(null) }
    val pageSize = 50

    FlameSectionCard(
        title = "Test Matrix",
        subtitle = if (tables.isEmpty()) "No PSI experimental table for this study"
        else "PSI Experimental Table — ${tables.sumOf { it.rows.size }} test rows"
    ) {
        if (tables.isEmpty()) {
            Text("Ground and modeling studies may have no experimental table; the data is in the related flight experiment.", fontSize = 13.sp, color = TextMediumGray)
            return@FlameSectionCard
        }
        val table = tables[selected.coerceIn(0, tables.lastIndex)]
        if (tables.size > 1) {
            FlameTabs(
                tabs = tables.mapIndexed { i, t -> FlameTabItem(i, t.shortName, count = t.rows.size) },
                selectedTab = selected,
                onTabSelected = { selected = it }
            )
            Spacer(Modifier.height(10.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FlameOutlinedButton("Copy CSV", onClick = { clipboard.setText(AnnotatedString(table.toCsv())); copied = "CSV copied to clipboard" })
            FlameOutlinedButton("Copy JSON", onClick = { clipboard.setText(AnnotatedString(table.toJson())); copied = "JSON copied to clipboard" })
            copied?.let { Text(it, fontSize = 12.sp, color = RiskLowGreen, fontWeight = FontWeight.Bold) }
        }
        Spacer(Modifier.height(10.dp))
        val from = page * pageSize
        val pageRows = table.rows.drop(from).take(pageSize)
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(8.dp))
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                Row(Modifier.background(LabSurfaceSubtle)) {
                    table.columns.forEach { c ->
                        Text(c, Modifier.width(130.dp).padding(8.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDarkAnthracite, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                pageRows.forEachIndexed { i, r ->
                    Row(Modifier.background(if (i % 2 == 0) LabSurface else LabBackground)) {
                        r.forEach { v ->
                            Text(v, Modifier.width(130.dp).padding(horizontal = 8.dp, vertical = 6.dp), fontSize = 11.sp, color = TextDarkAnthracite, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        val pages = (table.rows.size + pageSize - 1) / pageSize
        if (pages > 1) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FlameOutlinedButton("‹ Previous", onClick = { page-- }, enabled = page > 0)
                Text("Page ${page + 1} / $pages", fontSize = 12.sp, color = TextMediumGray)
                FlameOutlinedButton("Next ›", onClick = { page++ }, enabled = page < pages - 1)
            }
        }
    }
}
