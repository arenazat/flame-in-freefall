package org.example.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import org.example.project.ui.theme.*

/**
 * Risk seviyesi rozeti.
 * Açık zeminler üzerinde:
 * - Düşük risk: Yeşil (#2E9E6C), Zemin (#EAF6F0)
 * - Orta risk: Amber (#C98A1A), Zemin (#FAF3E6)
 * - Yüksek / Kritik risk: Kırmızı (#D64545), Zemin (#FBECEC)
 */
@Composable
fun FlameRiskBadge(
    score: Int,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val (textColor, bgColor, defaultLabel) = when {
        score < 35 -> Triple(RiskLowGreen, RiskLowGreenBg, "LOW RISK")
        score < 70 -> Triple(RiskMediumAmber, RiskMediumAmberBg, "MEDIUM RISK")
        else -> Triple(RiskHighRed, RiskHighRedBg, "HIGH RISK")
    }

    val displayLabel = label ?: defaultLabel

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(textColor)
        )
        Text(
            text = "$score/100",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            fontSize = 11.sp
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.labelSmall,
            color = textColor.copy(alpha = 0.5f)
        )
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}

/**
 * NASA Deney serisi rozeti (FLEX, SAFFIRE, BASS, ACME vb.)
 * Zemin: Açık camgöbeği (#E8F4FA), Yazı: Koyu camgöbeği (#1E88C7)
 */
@Composable
fun FlameSeriesBadge(
    series: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(AccentTealMuted)
            .border(BorderStroke(1.dp, AccentTeal.copy(alpha = 0.2f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = series.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AccentTeal,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * Yanıcı malzeme rozeti.
 * Zemin: Açık panel (#F1F3F6), Kenarlık: (#E2E5EA), Metin: (#1B1F27)
 */
@Composable
fun FlameMaterialBadge(
    material: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LabSurfaceSubtle)
            .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = material,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = TextDarkAnthracite,
            fontSize = 11.sp
        )
    }
}

/**
 * Genel durum / inceleme rozeti.
 */
@Composable
fun FlameStatusBadge(
    text: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val textColor = if (isWarning) RiskMediumAmber else RiskLowGreen
    val bgColor = if (isWarning) RiskMediumAmberBg else RiskLowGreenBg

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(BorderStroke(1.dp, textColor.copy(alpha = 0.25f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 10.sp
        )
    }
}
