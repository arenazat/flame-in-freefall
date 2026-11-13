package org.example.project.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.Experiment
import org.example.project.ui.theme.*

/**
 * Faz 4.1 — Karşılaştırmalı Grafik Bileşeni (ComparativeChartCard)
 *
 * Kural:
 * - GERÇEK metinden LLM ile çıkarılabiliyorsa 1g vs mikro-yerçekimi karşılaştırma grafiği gösterir.
 * - Metinden güvenilir sayısal değer çıkarılamıyorsa grafiği göstermez,
 *   "bu deney için karşılaştırmalı sayısal veri çıkarılamadı" notu gösterir — ASLA sayı uydurmaz.
 */
@Composable
fun ComparativeChartCard(
    experiment: Experiment,
    modifier: Modifier = Modifier
) {
    val summary = experiment.summary
    val hasData = summary != null && summary.hasComparativeData

    FlameCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Başlık Çubuğu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (hasData) AccentOrangeRed else TextMediumGray)
                    )
                    Text(
                        text = "1g (EARTH) vs MICROGRAVITY NUMERICAL COMPARISON",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (hasData) AccentOrangeRed else TextMediumGray,
                        letterSpacing = 0.5.sp
                    )
                }

                if (hasData) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LabBackground,
                        border = BorderStroke(1.dp, LabBorder)
                    ) {
                        Text(
                            text = summary.comparisonMetricName ?: "Flame spread rate",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextDarkAnthracite,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (hasData) {
                // GERÇEK SAYISAL KARŞILAŞTIRMA GRAFİĞİ
                ComparativeChartContent(
                    oneGValue = summary.oneGValue!!,
                    microGValue = summary.microGValue!!,
                    unit = summary.comparisonUnit ?: "mm/s",
                    metricName = summary.comparisonMetricName ?: "Flame spread rate",
                    notes = summary.comparisonNotes
                )
            } else {
                // KURAL GEREĞİ SAYI UYDURMAYAN LABORATUVAR BİLDİRİMİ
                NoComparativeDataNotice()
            }
        }
    }
}

/**
 * Karşılaştırmalı Grafik İçeriği
 */
@Composable
private fun ComparativeChartContent(
    oneGValue: Double,
    microGValue: Double,
    unit: String,
    metricName: String,
    notes: String?
) {
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        animationPlayed = true
    }

    val maxVal = maxOf(oneGValue, microGValue)
    val oneGRatio = if (maxVal > 0) (oneGValue / maxVal).toFloat() else 0.5f
    val microGRatio = if (maxVal > 0) (microGValue / maxVal).toFloat() else 0.5f

    val animatedOneGRatio by animateFloatAsState(
        targetValue = if (animationPlayed) oneGRatio else 0f,
        animationSpec = tween(durationMillis = 800)
    )
    val animatedMicroGRatio by animateFloatAsState(
        targetValue = if (animationPlayed) microGRatio else 0f,
        animationSpec = tween(durationMillis = 800)
    )

    // Fark Yüzdesi
    val pctDiff = if (oneGValue > 0) ((microGValue - oneGValue) / oneGValue * 100.0) else 0.0
    val isDecrease = pctDiff < 0

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // İstatistiksel Özet Şeridi
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(LabBackground)
                .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Measured parameter",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray
                )
                Text(
                    text = "$metricName ($unit)",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite
                )
            }

            // Değişim Rozeti
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isDecrease) RiskLowGreenBg else RiskHighRedBg,
                border = BorderStroke(1.dp, if (isDecrease) RiskLowGreen.copy(alpha = 0.4f) else RiskHighRed.copy(alpha = 0.4f))
            ) {
                Text(
                    text = if (isDecrease) "▼ ${kotlin.math.abs(pctDiff).format1Dec()}% decrease"
                           else "▲ +${pctDiff.format1Dec()}% increase",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isDecrease) RiskLowGreen else RiskHighRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        // 2 Sütunlu Karşılaştırma Grafiği
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            // Sütun 1: 1g Dünya
            BarColumn(
                label = "1g (Earth)",
                environmentDesc = "Normal gravity",
                valueText = "${oneGValue.format1Dec()} $unit",
                ratio = animatedOneGRatio,
                barColor = AccentTeal,
                modifier = Modifier.weight(1f)
            )

            // Ayırıcı
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight(0.8f)
                    .background(LabBorderSubtle)
            )

            // Sütun 2: Mikroyerçekimi
            BarColumn(
                label = "Microgravity (μg)",
                environmentDesc = "ISS / free fall",
                valueText = "${microGValue.format1Dec()} $unit",
                ratio = animatedMicroGRatio,
                barColor = AccentOrangeRed,
                modifier = Modifier.weight(1f)
            )
        }

        // NASA Metin Alıntısı Notu
        if (!notes.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LabBackground,
                border = BorderStroke(1.dp, LabBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NUMERICAL FINDING FROM THE NASA REPORT:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDarkAnthracite,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Karşılaştırma Sütunu
 */
@Composable
private fun BarColumn(
    label: String,
    environmentDesc: String,
    valueText: String,
    ratio: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        // Değer Etiketi
        Text(
            text = valueText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = barColor,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        // Dikey Bar
        Box(
            modifier = Modifier
                .width(48.dp)
                .fillMaxHeight(0.65f * ratio.coerceIn(0.08f, 1f))
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(barColor, barColor.copy(alpha = 0.7f))
                    )
                )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Ortam Etiketi
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = TextDarkAnthracite,
            textAlign = TextAlign.Center
        )
        Text(
            text = environmentDesc,
            style = MaterialTheme.typography.labelSmall,
            color = TextMediumGray,
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Veri Bulunamadığında Gösterilen Kurala Uygun Bildirim
 * (Kesinlikle sayı uydurmaz, şeffaf biçimde veri eksikliğini açıklar)
 */
@Composable
private fun NoComparativeDataNotice() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = LabBackground,
        border = BorderStroke(1.dp, LabBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LabSurfaceSubtle)
                    .border(BorderStroke(1.dp, LabBorder), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "ℹ", fontSize = 14.sp, color = TextMediumGray, fontWeight = FontWeight.Bold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "No comparative numerical data could be extracted for this record",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite
                )
                Text(
                    text = "The NASA NTRS report text does not contain a directly comparable numerical measurement (e.g. flame spread rate, extinction diameter) for the same parameters at both 1g and microgravity. For scientific accuracy, no estimated or invented numbers are shown.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumGray,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

private fun Double.format1Dec(): String {
    val rounded = (this * 10.0).toLong() / 10.0
    return rounded.toString()
}
