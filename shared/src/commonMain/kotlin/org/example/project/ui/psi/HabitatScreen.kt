package org.example.project.ui.psi

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.HabitatRankingItem
import org.example.project.models.PsiInvestigation
import org.example.project.models.SafetyCategory
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/** NASA hedef 1: mürettebatlı habitatlar için en ilgili deneyleri bul ve sırala. */
@Composable
fun HabitatHubScreen(onAskAi: (String) -> Unit, modifier: Modifier = Modifier) {
    var detailId by remember { mutableStateOf<String?>(null) }
    val id = detailId
    if (id != null) {
        PsiDetailScreen(psiId = id, onBack = { detailId = null }, onOpenRelated = { detailId = it }, onAskAi = onAskAi, modifier = modifier)
    } else {
        HabitatScreen(onOpen = { detailId = it }, onAskAi = onAskAi, modifier = modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitatScreen(
    onOpen: (String) -> Unit,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HabitatViewModel = koinInject()
) {
    val s by viewModel.uiState.collectAsState()

    BoxWithConstraints(modifier.fillMaxSize().background(LabBackground)) {
        val wide = maxWidth >= 1000.dp
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Habitat seçimi
            FlameSectionCard(title = "Habitat Atmosphere", subtitle = "Pick a preset or set O₂ / pressure manually") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        s.profiles.forEach { p ->
                            SelectableChip(p.name, !s.isCustom && p.id == s.selectedProfileId, { viewModel.selectProfile(p) }, AccentOrangeRed)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoChip("O₂ %${s.o2Percent.fmt(1)}", emphasized = true)
                        InfoChip("${s.pressurePsi.fmt(1)} psi = ${s.pressureAtm.fmt(3)} atm", emphasized = true)
                        s.selectedProfile?.gravityG?.let { g -> if (!s.isCustom) InfoChip("Gravity ${g.fmt(2)} g") }
                        if (s.isCustom) InfoChip("Custom atmosphere (scores: ${s.selectedProfile?.name ?: "-"})")
                    }
                    s.selectedProfile?.note?.takeIf { !s.isCustom }?.let { Text(it, fontSize = 12.sp, color = TextMediumGray) }
                    Text("O₂ (%)", fontSize = 12.sp, color = TextMediumGray)
                    Slider(
                        value = s.o2Percent.toFloat(), onValueChange = { viewModel.setO2(it.toDouble()) }, valueRange = 15f..100f,
                        colors = SliderDefaults.colors(thumbColor = AccentOrangeRed, activeTrackColor = AccentOrangeRed, inactiveTrackColor = LabBorder)
                    )
                    Text("Pressure (psi)", fontSize = 12.sp, color = TextMediumGray)
                    Slider(
                        value = s.pressurePsi.toFloat(), onValueChange = { viewModel.setPressurePsi(it.toDouble()) }, valueRange = 3f..15f,
                        colors = SliderDefaults.colors(thumbColor = AccentTeal, activeTrackColor = AccentTeal, inactiveTrackColor = LabBorder)
                    )
                }
            }

            NoticeBox("No partial-gravity data", PARTIAL_GRAVITY_NOTE)

            val chart = @Composable {
                FlameSectionCard(
                    title = "O₂ – Pressure Map",
                    subtitle = "Boxes: experiment test ranges • Red +: selected atmosphere • Dashed area: lunar base zone (≥30% O₂, ≤10.2 psi)"
                ) {
                    O2PressureChart(s.investigations, s.o2Percent, s.pressureAtm, Modifier.fillMaxWidth().height(380.dp))
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SafetyCategory.entries.forEach { c ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(c.color()))
                                Text(c.label, fontSize = 11.sp, color = TextMediumGray)
                            }
                        }
                    }
                }
            }
            val ranking = @Composable {
                FlameSectionCard(
                    title = "Relevant Experiments — ${s.title}",
                    subtitle = "Score = habitat relevance ×10 + O₂/pressure coverage bonus + safety category",
                    action = {
                        FlamePrimaryButton("✦ AI explanation", onClick = {
                            val top = s.ranking.take(5).joinToString(", ") { "${it.psiId} ${it.displayName}" }
                            onAskAi(
                                "Which NASA PSI experiments matter most for fire safety in ${s.title} (O₂ ${s.o2Percent.fmt(1)}%, ${s.pressurePsi.fmt(1)} psi), and why? Consider these first: $top"
                            )
                        }, enabled = s.ranking.isNotEmpty())
                    }
                ) {
                    if (s.isLoading && s.ranking.isEmpty()) {
                        CircularProgressIndicator(color = AccentOrangeRed)
                    } else if (s.ranking.isEmpty()) {
                        Text("Could not load the ranking. Make sure the rank_investigations_for_habitat function exists in Supabase.", color = TextMediumGray, fontSize = 13.sp)
                    }
                    val maxScore = (s.ranking.maxOfOrNull { it.score } ?: 1.0).coerceAtLeast(1.0)
                    s.ranking.forEachIndexed { i, item -> RankingRow(i + 1, item, maxScore, onClick = { onOpen(item.psiId) }) }
                    Spacer(Modifier.height(6.dp))
                    Text(HABITAT_SCORE_NOTE, fontSize = 11.sp, color = TextLightMuted)
                }
            }

            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.weight(1.1f)) { ranking() }
                    Box(Modifier.weight(1f)) { chart() }
                }
            } else {
                chart()
                ranking()
            }
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun RankingRow(rank: Int, item: HabitatRankingItem, maxScore: Double, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 8.dp, horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("#$rank", fontWeight = FontWeight.Black, color = AccentOrangeRed, modifier = Modifier.width(34.dp))
            FlameSeriesBadge(item.displayName)
            CategoryBadge(item.safetyCategory)
            Spacer(Modifier.weight(1f))
            Text(item.score.fmt(0), fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
        }
        Text(item.title, fontSize = 13.sp, color = TextDarkAnthracite, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 42.dp))
        Box(Modifier.padding(start = 42.dp).fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(LabSurfaceSubtle)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth((item.score / maxScore).toFloat().coerceIn(0f, 1f)).background(item.safetyCategory.color()))
        }
        Row(Modifier.padding(start = 42.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FlameStatusBadge(if (item.coversO2) "O₂ covered" else "O₂ not covered", isWarning = !item.coversO2)
            FlameStatusBadge(if (item.coversPressure) "Pressure covered" else "Pressure not covered", isWarning = !item.coversPressure)
        }
        item.relevanceNote?.let { Text(it, fontSize = 12.sp, color = TextMediumGray, modifier = Modifier.padding(start = 42.dp)) }
    }
}

/** x = basınç (atm, 0–3,5), y = O₂ (%, 0–100). Her deneyin test aralığı yarı saydam kutu. */
@Composable
private fun O2PressureChart(
    items: List<PsiInvestigation>,
    o2: Double,
    pressureAtm: Double,
    modifier: Modifier
) {
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = TextMediumGray)
    val tagStyle = TextStyle(fontSize = 9.sp, color = TextDarkAnthracite, fontWeight = FontWeight.Bold)
    val xMax = 3.5
    val yMax = 100.0
    Canvas(modifier) {
        val left = 40f
        val bottom = size.height - 26f
        val w = size.width - left - 10f
        val h = bottom - 8f
        fun x(p: Double) = left + (p.coerceIn(0.0, xMax) / xMax * w).toFloat()
        fun y(o: Double) = bottom - (o.coerceIn(0.0, yMax) / yMax * h).toFloat()

        // Izgara + eksen etiketleri
        listOf(0.0, 20.0, 40.0, 60.0, 80.0, 100.0).forEach { v ->
            drawLine(LabBorderSubtle, Offset(left, y(v)), Offset(left + w, y(v)))
            drawText(measurer, "${v.toInt()}%", Offset(2f, y(v) - 7f), labelStyle)
        }
        listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5).forEach { v ->
            drawLine(LabBorderSubtle, Offset(x(v), 8f), Offset(x(v), bottom))
            drawText(measurer, v.fmt(1), Offset(x(v) - 8f, bottom + 6f), labelStyle)
        }
        drawText(measurer, "atm", Offset(left + w - 22f, bottom - 16f), labelStyle)

        // Ay üssü bölgesi: O₂ ≥ 30, basınç ≤ 10,2 psi
        val lunarP = 10.2 / PSI_PER_ATM
        drawRect(
            AccentOrangeRed.copy(alpha = 0.07f),
            topLeft = Offset(x(0.0), y(100.0)),
            size = Size(x(lunarP) - x(0.0), y(30.0) - y(100.0))
        )
        drawRect(
            AccentOrangeRed.copy(alpha = 0.5f),
            topLeft = Offset(x(0.0), y(100.0)),
            size = Size(x(lunarP) - x(0.0), y(30.0) - y(100.0)),
            style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
        )
        drawText(measurer, "Lunar base zone", Offset(x(0.02), y(98.0)), tagStyle.copy(color = AccentOrangeRed))

        // Deney kutuları
        items.forEach { inv ->
            val o1 = inv.o2Min ?: return@forEach
            val o2v = inv.o2Max ?: o1
            val p1 = inv.pressureMinAtm ?: return@forEach
            val p2 = inv.pressureMaxAtm ?: p1
            val c = inv.safetyCategory.color()
            val tl = Offset(x(p1) - 4f, y(o2v) - 4f)
            val sz = Size((x(p2) - x(p1)) + 8f, (y(o1) - y(o2v)) + 8f)
            drawRect(c.copy(alpha = 0.16f), tl, sz)
            drawRect(c.copy(alpha = 0.8f), tl, sz, style = Stroke(1.2f))
            drawText(measurer, inv.displayName.take(12), Offset(tl.x + 2f, tl.y - 12f), tagStyle.copy(color = c))
        }

        // Seçili atmosfer
        val cx = x(pressureAtm)
        val cy = y(o2)
        drawLine(RiskHighRed, Offset(cx - 10f, cy), Offset(cx + 10f, cy), strokeWidth = 3f)
        drawLine(RiskHighRed, Offset(cx, cy - 10f), Offset(cx, cy + 10f), strokeWidth = 3f)
    }
}
