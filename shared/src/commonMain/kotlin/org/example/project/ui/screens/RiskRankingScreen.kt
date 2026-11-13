package org.example.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 3.4 — Risk Sıralama Ekranı (Adaptive)
 *
 * - GERÇEK veri setindeki malzemeleri risk skoruna göre azalan sırada listeler.
 * - Geniş ekranda tam bir tablo (Malzeme, Ortalama Risk Skoru renkli bar+sayı, Gerçek Deney Sayısı, En Yüksek Riskli Senaryo).
 * - Dar ekranda aynı bilgiyi taşıyan kart listesi.
 * - Hiç risk skoru olmayan malzemeler "veri yetersiz" notuyla listenin sonunda gösterilir.
 * - Sıralama kriteri değiştirilebilir (En Yüksek Risk, En Düşük Risk, Deney Sayısı, İsim).
 */
@Composable
fun RiskRankingScreen(
    modifier: Modifier = Modifier,
    viewModel: RiskRankingViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp)
    ) {
        val isWideScreen = maxWidth >= 860.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Üst Başlık & İstatistik Kartları Şeridi
            HeaderAndStatSection(uiState = uiState)

            // 2. Arama ve Sıralama Kontrol Paneli
            ControlBar(
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                selectedCriteria = uiState.sortCriteria,
                onCriteriaChange = { viewModel.onSortCriteriaChanged(it) }
            )

            // 3. İçerik Alanı: Geniş Ekranda Tablo, Dar Ekranda Kart Listesi
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = AccentTeal,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Computing material risk matrix...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMediumGray
                        )
                    }
                }
            } else if (uiState.errorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    FlameCard(modifier = Modifier.widthIn(max = 420.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "An Error Occurred",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = RiskHighRed
                            )
                            Text(
                                text = uiState.errorMessage ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumGray
                            )
                            FlamePrimaryButton(
                                text = "Try Again",
                                onClick = { viewModel.loadData() }
                            )
                        }
                    }
                }
            } else if (uiState.filteredSummaries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No materials match the search.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMediumGray
                    )
                }
            } else if (isWideScreen) {
                // Geniş Ekran: Masaüstü / Web Tablosu
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    WideRiskTableView(summaries = uiState.filteredSummaries)
                }
            } else {
                // Dar Ekran: Mobil Kart Listesi
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    MobileRiskCardListView(summaries = uiState.filteredSummaries)
                }
            }
        }
    }
}

/**
 * Üst Başlık ve İstatistik Şeridi
 */
@Composable
private fun HeaderAndStatSection(uiState: RiskRankingUiState) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentOrangeRed)
                        )
                        Text(
                            text = "SAFETY & FLAMMABILITY MATRIX",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrangeRed,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "Microgravity Material Risk Ranking",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite
                    )
                    Text(
                        text = "Average hazard score (0–100) and riskiest scenarios for materials tested in NASA experiments.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumGray
                    )
                }
            }

            // Hızlı Metrik Kartları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatPill(
                    label = "Materials tested",
                    value = "${uiState.totalMaterialsCount}",
                    color = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Risk-assessed",
                    value = "${uiState.ratedMaterialsCount}",
                    color = RiskLowGreen,
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    label = "Pending / insufficient data",
                    value = "${uiState.unratedMaterialsCount}",
                    color = RiskMediumAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = LabBackground,
        border = BorderStroke(1.dp, LabBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMediumGray,
                fontSize = 11.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Arama ve Sıralama Kontrolleri
 */
@Composable
private fun ControlBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    selectedCriteria: RiskSortCriteria,
    onCriteriaChange: (RiskSortCriteria) -> Unit
) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Arama Kutusu
                FlameSearchField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    placeholder = "Filter by material (e.g. PMMA, heptane, cotton)...",
                    modifier = Modifier.weight(1f)
                )
            }

            // Sıralama Kriterleri Seçimi
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Sort by:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray
                )

                RiskSortCriteria.values().forEach { criteria ->
                    val isSelected = criteria == selectedCriteria
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) AccentTealMuted else LabBackground)
                            .border(
                                BorderStroke(1.dp, if (isSelected) AccentTeal else LabBorder),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { onCriteriaChange(criteria) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = criteria.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AccentTeal else TextDarkAnthracite,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Geniş Ekran: Masaüstü / Web Laboratuvar Tablosu
 */
@Composable
private fun WideRiskTableView(summaries: List<MaterialRiskSummary>) {
    FlameCard(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tablo Başlık Satırı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LabSurfaceSubtle)
                    .border(BorderStroke(1.dp, LabBorder))
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MATERIAL",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite,
                    modifier = Modifier.weight(2.5f)
                )
                Text(
                    text = "AVERAGE RISK SCORE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite,
                    modifier = Modifier.weight(3.0f)
                )
                Text(
                    text = "RECORDS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite,
                    modifier = Modifier.weight(2.0f)
                )
                Text(
                    text = "HIGHEST-RISK SCENARIO / TITLE",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite,
                    modifier = Modifier.weight(3.5f)
                )
            }

            // Tablo Veri Satırları
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(summaries, key = { it.material }) { item ->
                    TableRowItem(item = item)
                    HorizontalDivider(color = LabBorderSubtle)
                }
            }
        }
    }
}

@Composable
private fun TableRowItem(item: MaterialRiskSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Malzeme Adı
        Row(
            modifier = Modifier.weight(2.5f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FlameMaterialBadge(material = item.material)
        }

        // 2. Ortalama Risk Skoru (Renkli Bar + Sayı)
        Box(modifier = Modifier.weight(3.0f)) {
            if (item.hasRiskScore && item.averageRiskScore != null) {
                val score = item.averageRiskScore
                val (color, bgColor, label) = when {
                    score < 35 -> Triple(RiskLowGreen, RiskLowGreenBg, "Low")
                    score < 70 -> Triple(RiskMediumAmber, RiskMediumAmberBg, "Medium")
                    else -> Triple(RiskHighRed, RiskHighRedBg, "High")
                }

                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${score.toInt()}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            fontSize = 11.sp
                        )
                    }

                    // Renkli Çubuk (Progress Bar)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(bgColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((score / 100.0).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            } else {
                FlameStatusBadge(
                    text = "insufficient data",
                    isWarning = true
                )
            }
        }

        // 3. Gerçek Deney Sayısı
        Box(modifier = Modifier.weight(2.0f)) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = AccentTealMuted,
                border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.2f))
            ) {
                Text(
                    text = "${item.experimentCount} records",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = AccentTeal,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 11.sp
                )
            }
        }

        // 4. En Yüksek Riskli Senaryo
        Column(
            modifier = Modifier.weight(3.5f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val exp = item.highestRiskExperiment
            if (exp != null) {
                Text(
                    text = exp.title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDarkAnthracite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Series: ${exp.series} • Source: NTRS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontSize = 10.sp
                )
            } else {
                Text(
                    text = "No scenario extracted yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLightMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * Dar Ekran: Mobil Kart Listesi Görünümü
 */
@Composable
private fun MobileRiskCardListView(summaries: List<MaterialRiskSummary>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        items(summaries, key = { it.material }) { item ->
            MobileRiskCardItem(item = item)
        }
    }
}

@Composable
private fun MobileRiskCardItem(item: MaterialRiskSummary) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Üst Kısım: Malzeme Adı + Deney Sayısı Rozeti
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlameMaterialBadge(material = item.material)

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentTealMuted,
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.2f))
                ) {
                    Text(
                        text = "${item.experimentCount} records",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp
                    )
                }
            }

            // Orta Kısım: Ortalama Risk Skoru
            if (item.hasRiskScore && item.averageRiskScore != null) {
                val score = item.averageRiskScore
                val (color, bgColor, label) = when {
                    score < 35 -> Triple(RiskLowGreen, RiskLowGreenBg, "Low Risk")
                    score < 70 -> Triple(RiskMediumAmber, RiskMediumAmberBg, "Medium Risk")
                    else -> Triple(RiskHighRed, RiskHighRedBg, "High Risk")
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Average risk: ${score.toInt()}/100",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                        FlameRiskBadge(score = score.toInt(), label = label)
                    }

                    // Çubuk
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(bgColor)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((score / 100.0).toFloat().coerceIn(0.05f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(3.dp))
                                .background(color)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Average risk score:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumGray
                    )
                    FlameStatusBadge(
                        text = "insufficient data",
                        isWarning = true
                    )
                }
            }

            HorizontalDivider(color = LabBorderSubtle)

            // Alt Kısım: En Yüksek Riskli Senaryo
            val exp = item.highestRiskExperiment
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Highest-risk record / scenario:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontSize = 11.sp
                )
                Text(
                    text = exp?.title ?: "No scenario extracted yet",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkAnthracite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
