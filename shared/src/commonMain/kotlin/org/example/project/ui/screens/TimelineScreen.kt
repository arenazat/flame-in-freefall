package org.example.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.example.project.models.Experiment
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 4.2 — Zaman Çizelgesi Ekranı (TimelineScreen)
 *
 * - GERÇEK deney kayıtlarının yıl bilgisine göre dikey/yatay zaman çizelgesi.
 * - Her deney gerçek yılına yerleştirilmiş bir nokta/kart olarak, program/seriye göre filtrelenebilir.
 * - Tıklandığında doğrudan Deney Detay Ekranını açar.
 * - Gerçek Supabase verisiyle çalışır; sıfır mock/hardcoded veri.
 */
@Composable
fun TimelineScreen(
    modifier: Modifier = Modifier,
    viewModel: TimelineViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedExperiment by remember { mutableStateOf<Experiment?>(null) }

    // Deney seçildiyse doğrudan Detay Ekranını aç
    if (selectedExperiment != null) {
        ExperimentDetailScreen(
            experiment = selectedExperiment!!,
            onBack = { selectedExperiment = null },
            onExperimentSelected = { selectedExperiment = it },
            modifier = modifier
        )
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp)
    ) {
        val isWideScreen = maxWidth >= 840.dp

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Üst Filtre & Navigasyon Başlığı
            TimelineHeader(
                totalCount = uiState.totalCount,
                availableSeries = uiState.availableSeries,
                selectedSeries = uiState.selectedSeries,
                isAscending = uiState.isAscending,
                onSelectSeries = { viewModel.setSeriesFilter(it) },
                onToggleSort = { viewModel.toggleSortOrder() },
                onRefresh = { viewModel.loadTimeline() }
            )

            // 2. Yıl Hızlı Erişim Şeridi (Jump Pills)
            if (uiState.timelineGroups.isNotEmpty()) {
                YearJumpRow(
                    years = uiState.timelineGroups.map { it.year },
                    onYearClick = { year ->
                        val index = uiState.timelineGroups.indexOfFirst { it.year == year }
                        if (index != -1) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                    }
                )
            }

            // 3. Yükleniyor veya İçerik Akışı
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
                            color = AccentOrangeRed,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = "Loading NASA experiment chronology...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumGray
                        )
                    }
                }
            } else if (uiState.timelineGroups.isEmpty() && uiState.undatedExperiments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No records match the selected filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMediumGray
                    )
                }
            } else {
                // Zaman Çizelgesi Akışı (LazyColumn)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(uiState.timelineGroups, key = { it.year }) { group ->
                        TimelineYearSection(
                            group = group,
                            isWideScreen = isWideScreen,
                            onExperimentClick = { selectedExperiment = it }
                        )
                    }

                    // Yılı Belirtilmemiş Kayıtlar (Varsa)
                    if (uiState.undatedExperiments.isNotEmpty()) {
                        item {
                            UndatedSection(
                                experiments = uiState.undatedExperiments,
                                onExperimentClick = { selectedExperiment = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Zaman Çizelgesi Üst Başlık ve Filtreleri
 */
@Composable
private fun TimelineHeader(
    totalCount: Int,
    availableSeries: List<String>,
    selectedSeries: String?,
    isAscending: Boolean,
    onSelectSeries: (String?) -> Unit,
    onToggleSort: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(AccentTeal)
                    )
                    Column {
                        Text(
                            text = "EXPERIMENT CHRONOLOGY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkAnthracite
                        )
                        Text(
                            text = "NASA microgravity combustion research by year",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumGray
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Kayıt Sayısı Rozeti
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = AccentTealMuted,
                        border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "$totalCount records",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }

                    // Sıralama Yönü Değiştirici
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LabBackground,
                        border = BorderStroke(1.dp, LabBorder),
                        modifier = Modifier.clickable(onClick = onToggleSort)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (isAscending) "Oldest first ⬆" else "Newest first ⬇",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDarkAnthracite
                            )
                        }
                    }
                }
            }

            // Seri / Program Filtre Çipleri
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Program:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontWeight = FontWeight.SemiBold
                )

                FilterChipItem(
                    label = "All",
                    isSelected = selectedSeries == null,
                    onClick = { onSelectSeries(null) }
                )

                availableSeries.forEach { series ->
                    FilterChipItem(
                        label = series,
                        isSelected = selectedSeries.equals(series, ignoreCase = true),
                        onClick = { onSelectSeries(series) }
                    )
                }
            }
        }
    }
}

/**
 * Yıl Hızlı Atlama Şeridi
 */
@Composable
private fun YearJumpRow(
    years: List<Int>,
    onYearClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Jump to year:",
            style = MaterialTheme.typography.labelSmall,
            color = TextMediumGray,
            fontWeight = FontWeight.Bold
        )

        years.forEach { year ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = LabSurface,
                border = BorderStroke(1.dp, LabBorder),
                modifier = Modifier.clickable { onYearClick(year) }
            ) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDarkAnthracite,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Bir Yıla Ait Zaman Çizelgesi Bölümü (Nod + Dikey Çizgi + Deney Kartları)
 */
@Composable
private fun TimelineYearSection(
    group: YearTimelineGroup,
    isWideScreen: Boolean,
    onExperimentClick: (Experiment) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sol: Timeline Omurgası (Node + Çizgi)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Parlayan Timeline Düğümü
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AccentOrangeRedMuted)
                    .border(BorderStroke(2.dp, AccentOrangeRed), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentOrangeRed)
                )
            }

            // Dikey Bağlantı Çizgisi
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(LabBorder)
            )
        }

        // Sağ: Yıl Başlığı ve Deney Kartları
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Yıl Başlık Şeridi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = group.year.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = TextDarkAnthracite
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = LabSurface,
                    border = BorderStroke(1.dp, LabBorder)
                ) {
                    Text(
                        text = "${group.experiments.size} records",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMediumGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // O Yıl Gerçekleştirilen Deneylerin Kartları
            group.experiments.forEach { experiment ->
                TimelineExperimentCard(
                    experiment = experiment,
                    onClick = { onExperimentClick(experiment) }
                )
            }
        }
    }
}

/**
 * Zaman Çizelgesi Deney Kartı
 */
@Composable
private fun TimelineExperimentCard(
    experiment: Experiment,
    onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    FlameCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Rozetler & Yıl
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FlameSeriesBadge(series = experiment.series)
                    FlameMaterialBadge(material = experiment.material)
                    if (experiment.summary != null) {
                        FlameRiskBadge(
                            score = experiment.summary.riskScore,
                            label = experiment.summary.riskLabel
                        )
                    }
                }

                Text(
                    text = "Details →",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal,
                    fontWeight = FontWeight.Bold
                )
            }

            // Başlık
            Text(
                text = experiment.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            // AI Özeti veya Bilimsel Bulgu
            val summaryText = experiment.summary?.aiSummary
                ?: experiment.rawText?.take(160)?.let { "$it..." }
                ?: "NASA NTRS record (summary pending)"

            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            HorizontalDivider(color = LabBorderSubtle)

            // Alt Meta Şeridi: Kaynak URL & Parametreler
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gravity: ${experiment.gravityLevel} | O2: ${experiment.oxygenConcentration ?: 21.0}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightMuted,
                    fontSize = 11.sp
                )

                Text(
                    text = "NASA NTRS ↗",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        try {
                            uriHandler.openUri(experiment.sourceUrl)
                        } catch (e: Exception) {
                            println("[HATA] URL açılamadı: ${e.message}")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Yılı Belirtilmemiş Deneyler Bölümü
 */
@Composable
private fun UndatedSection(
    experiments: List<Experiment>,
    onExperimentClick: (Experiment) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LabBackground,
        border = BorderStroke(1.dp, LabBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Records without a date (${experiments.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextMediumGray
            )

            experiments.forEach { exp ->
                TimelineExperimentCard(
                    experiment = exp,
                    onClick = { onExperimentClick(exp) }
                )
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AccentTealMuted else LabBackground,
        border = BorderStroke(1.dp, if (isSelected) AccentTeal else LabBorder),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) AccentTeal else TextDarkAnthracite,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
