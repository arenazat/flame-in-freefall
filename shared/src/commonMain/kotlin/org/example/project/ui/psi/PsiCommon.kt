package org.example.project.ui.psi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.FuelState
import org.example.project.models.PsiInvestigation
import org.example.project.models.SafetyCategory
import org.example.project.ui.theme.*
import kotlin.math.round

// ---------------------------------------------------------------------------
// Ortak PSI yardımcıları — tüm platformlarda aynı tasarım dili (Faz 3.1 açık tema)
// ---------------------------------------------------------------------------

fun SafetyCategory?.color(): Color = when (this) {
    SafetyCategory.PREVENTION -> AccentOrangeRed
    SafetyCategory.DETECTION -> AccentTeal
    SafetyCategory.SUPPRESSION -> RiskLowGreen
    SafetyCategory.FUNDAMENTAL -> Color(0xFF7C5CC4)
    SafetyCategory.MODELING -> TextMediumGray
    null -> TextLightMuted
}

fun SafetyCategory?.bgColor(): Color = when (this) {
    SafetyCategory.PREVENTION -> AccentOrangeRedMuted
    SafetyCategory.DETECTION -> AccentTealMuted
    SafetyCategory.SUPPRESSION -> RiskLowGreenBg
    SafetyCategory.FUNDAMENTAL -> Color(0xFFF1ECFA)
    SafetyCategory.MODELING -> LabSurfaceSubtle
    null -> LabSurfaceSubtle
}

fun Double.fmt(decimals: Int = 1): String {
    var f = 1.0
    repeat(decimals) { f *= 10 }
    val v = round(this * f) / f
    val s = v.toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}

fun rangeText(min: Double?, max: Double?, unit: String, decimals: Int = 1): String? = when {
    min == null && max == null -> null
    min != null && max != null && min == max -> "${min.fmt(decimals)} $unit"
    else -> "${min?.fmt(decimals) ?: "?"}–${max?.fmt(decimals) ?: "?"} $unit"
}

fun PsiInvestigation.o2Text(): String? = rangeText(o2Min, o2Max, "% O₂")
fun PsiInvestigation.pressureText(): String? = rangeText(pressureMinAtm, pressureMaxAtm, "atm", 2)
fun PsiInvestigation.flowText(): String? = when {
    flowMinCms == null && flowMaxCms == null -> null
    flowMinCms == 0.0 && flowMaxCms == 0.0 -> "Quiescent (no flow)"
    else -> rangeText(flowMinCms, flowMaxCms, "cm/s")
}

fun formatBytes(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val gb = bytes / 1_000_000_000.0
    return if (gb >= 1000) "${(gb / 1000).fmt(2)} TB" else "${gb.fmt(1)} GB"
}

fun FuelState?.labelOrDash(): String = this?.label ?: "—"

@Composable
fun CategoryBadge(category: SafetyCategory?, modifier: Modifier = Modifier) {
    if (category == null) return
    val c = category.color()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(category.bgColor())
            .border(BorderStroke(1.dp, c.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c))
        Text(category.label, color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (emphasized) AccentTealMuted else LabSurfaceSubtle)
            .border(BorderStroke(1.dp, if (emphasized) AccentTeal.copy(alpha = 0.25f) else LabBorder), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text,
            fontSize = 11.sp,
            color = if (emphasized) AccentTeal else TextDarkAnthracite,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = AccentTeal) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) color.copy(alpha = 0.12f) else LabSurface)
            .border(BorderStroke(1.dp, if (selected) color else LabBorder), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            fontSize = 12.sp,
            color = if (selected) color else TextDarkAnthracite,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun NoticeBox(title: String, text: String, warning: Boolean = true, modifier: Modifier = Modifier) {
    val c = if (warning) RiskMediumAmber else AccentTeal
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (warning) RiskMediumAmberBg else AccentTealMuted)
            .border(BorderStroke(1.dp, c.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = c, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        Text(text, color = TextDarkAnthracite, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun RelevanceBar(label: String, value: Int?, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(130.dp), fontSize = 12.sp, color = TextMediumGray)
        Box(
            Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(LabSurfaceSubtle)
        ) {
            val v = (value ?: 0).coerceIn(0, 10)
            Box(
                Modifier.fillMaxHeight().fillMaxWidth(v / 10f).clip(RoundedCornerShape(4.dp))
                    .background(
                        when {
                            v >= 8 -> RiskHighRed
                            v >= 5 -> RiskMediumAmber
                            else -> RiskLowGreen
                        }
                    )
            )
        }
        Text(
            value?.let { "$it/10" } ?: "—",
            modifier = Modifier.width(48.dp).padding(start = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkAnthracite
        )
    }
}

@Composable
fun LinkText(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = AccentTeal,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

/** Kısmi yerçekimi kapsam notu (NASA challenge "Partial-gravity data"). */
const val PARTIAL_GRAVITY_NOTE =
    "PSI does not yet contain partial-gravity (Moon 0.16 g / Mars 0.38 g) combustion data. Results are limited to microgravity " +
        "and 1 g data; intermediate gravity can be bounded by these two extremes. ACME E-FIELD Flames (PSI-22) mimics partial " +
        "gravity with an electric field. Data from the LUCI and FM2 experiments will be added to PSI in the future."

const val HABITAT_SCORE_NOTE =
    "Habitat relevance scores (0–10) are editorial scores assigned for this panel based on each experiment's objectives and conditions; " +
        "O₂, pressure and flow ranges come from the NASA PSI experiment tables."
