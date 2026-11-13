package org.example.project.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalUriHandler
import org.example.project.models.Experiment
import org.example.project.models.SummaryStatus
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 3.2 & Faz 4.3 — Ana Ekran — Arama, Filtreleme ve Rapor Oluşturucu (Adaptive)
 *
 * Geniş ekranda (web/masaüstü): Sol yan panelde filtreler + sağda çok sütunlu kart grid'i.
 * Dar ekranda (mobil): Üstte açılır filtre paneli + tek sütun kart listesi.
 * Faz 4.3: Kullanıcı birden fazla GERÇEK deney seçip "Seçilenlerden Rapor Oluştur" diyebilir;
 * Supabase Edge Function üzerinden oluşturulan PDF/HTML rapor indirilebilir.
 */
@Composable
fun ExperimentListScreen(
    modifier: Modifier = Modifier,
    viewModel: ExperimentListViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current
    var selectedExperiment by remember { mutableStateOf<Experiment?>(null) }

    // Seçili deney varsa doğrudan Detay Ekranını göster
    if (selectedExperiment != null) {
        ExperimentDetailScreen(
            experiment = selectedExperiment!!,
            onBack = { selectedExperiment = null },
            onExperimentSelected = { selectedExperiment = it },
            modifier = modifier
        )
        return
    }

    // Faz 4.3: Başarılı Rapor İndirme Diyaloğu
    if (uiState.lastGeneratedReport != null) {
        val report = uiState.lastGeneratedReport!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissReportNotification() },
            containerColor = LabSurface,
            titleContentColor = TextDarkAnthracite,
            textContentColor = TextMediumGray,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AccentTealMuted),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✦", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Text(
                        text = "Report Ready",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = report.displayTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkAnthracite
                    )
                    Text(
                        text = "${report.experimentCount} NASA records with AI summaries, risk assessments and source links were compiled and saved to the Supabase report store.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumGray,
                        lineHeight = 18.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LabBackground,
                        border = BorderStroke(1.dp, LabBorderSubtle),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Created:",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextLightMuted
                            )
                            Text(
                                text = report.createdAt.take(19).replace("T", " "),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal
                            )
                        }
                    }
                }
            },
            confirmButton = {
                FlamePrimaryButton(
                    text = "Open / Download Report ↗",
                    onClick = {
                        viewModel.dismissReportNotification()
                        if (report.reportUrl.isNotBlank()) {
                            uriHandler.openUri(report.reportUrl)
                        }
                    }
                )
            },
            dismissButton = {
                FlameOutlinedButton(
                    text = "Close",
                    onClick = { viewModel.dismissReportNotification() }
                )
            }
        )
    }

    // Faz 4.3: Hata Bildirimi
    if (uiState.reportErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissReportNotification() },
            containerColor = LabSurface,
            title = {
                Text(
                    text = "Report Error",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RiskHighRed
                )
            },
            text = {
                Text(
                    text = uiState.reportErrorMessage ?: "An error occurred while generating the report.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDarkAnthracite
                )
            },
            confirmButton = {
                FlamePrimaryButton(
                    text = "OK",
                    onClick = { viewModel.dismissReportNotification() }
                )
            }
        )
    }

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
            // 1. Üst Panel: Geniş Arama Çubuğu + Sayaç + Filtre Açma Butonu (Mobilde)
            TopSearchHeader(
                searchQuery = uiState.searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                totalCount = uiState.experiments.size,
                isLoading = uiState.isLoading,
                isWideScreen = isWideScreen,
                isFilterOpen = uiState.isFilterPanelOpen,
                onToggleFilter = { viewModel.toggleFilterPanel() },
                onRefresh = { viewModel.refresh() }
            )

            // 2. Dar Ekran (Mobil) için Açılır Filtre Paneli
            if (!isWideScreen) {
                AnimatedVisibility(
                    visible = uiState.isFilterPanelOpen,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FlameCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            FilterContent(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            // 3. İçerik Alanı: Geniş ekranda sol panel + sağ grid; dar ekranda tek sütun
            if (isWideScreen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Sol Yan Filtre Paneli
                    FlameCard(
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "FILTERS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkAnthracite,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            FilterContent(
                                uiState = uiState,
                                viewModel = viewModel
                            )
                        }
                    }

                    // Sağ Çok Sütunlu Kart Grid'i
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        ExperimentGridContent(
                            uiState = uiState,
                            onClearFilters = { viewModel.clearFilters() },
                            onRefresh = { viewModel.refresh() },
                            onExperimentClick = { selectedExperiment = it },
                            onToggleSelect = { viewModel.toggleExperimentSelection(it) },
                            isWide = true
                        )
                    }
                }
            } else {
                // Dar Ekran Tek Sütun Kart Listesi
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    ExperimentGridContent(
                        uiState = uiState,
                        onClearFilters = { viewModel.clearFilters() },
                        onRefresh = { viewModel.refresh() },
                        onExperimentClick = { selectedExperiment = it },
                        onToggleSelect = { viewModel.toggleExperimentSelection(it) },
                        isWide = false
                    )
                }
            }
        }

        // Faz 4.3: Seçilen Deneylerden Rapor Oluşturma Kayan Alt Paneli
        AnimatedVisibility(
            visible = uiState.selectedExperimentIds.isNotEmpty(),
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(if (isWideScreen) 0.65f else 1f)
                .padding(bottom = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = LabSurface,
                border = BorderStroke(1.5.dp, AccentOrangeRed),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentOrangeRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${uiState.selectedExperimentIds.size}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Column {
                            Text(
                                text = "${uiState.selectedExperimentIds.size} selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkAnthracite
                            )
                            Text(
                                text = "Report with AI summaries, risk scores and source links",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FlameOutlinedButton(
                            text = "Clear",
                            onClick = { viewModel.clearSelection() }
                        )

                        FlamePrimaryButton(
                            text = if (uiState.isGeneratingReport) "Compiling report..." else "Create Report from Selection",
                            onClick = {
                                if (!uiState.isGeneratingReport) {
                                    viewModel.generateReportForSelected { url ->
                                        uriHandler.openUri(url)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Üst Arama ve Bilgi Başlığı
 */
@Composable
private fun TopSearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    totalCount: Int,
    isLoading: Boolean,
    isWideScreen: Boolean,
    isFilterOpen: Boolean,
    onToggleFilter: () -> Unit,
    onRefresh: () -> Unit
) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Geniş Arama Alanı
            FlameSearchField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search NASA experiments, papers or materials (e.g. FLEX, PMMA, heptane)...",
                modifier = Modifier.weight(1f)
            )

            // Dar ekranda Filtre Aç/Kapa Butonu
            if (!isWideScreen) {
                FlameOutlinedButton(
                    text = if (isFilterOpen) "Close" else "Filters",
                    onClick = onToggleFilter,
                    borderColor = if (isFilterOpen) AccentTeal else LabBorder
                )
            }

            // Canlı Kayıt Sayacı
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentTealMuted,
                border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(AccentTeal)
                    )
                    Text(
                        text = if (isLoading) "Loading..." else "$totalCount records",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal
                    )
                }
            }

            // Yenile Butonu
            FlameOutlinedButton(
                text = "Refresh",
                onClick = onRefresh
            )
        }
    }
}

/**
 * Dinamik Filtreleme Seçenekleri (Sol panel veya mobil açılır panel)
 * Veriler Supabase'den dinamik olarak çekilir (hardcoded liste içermez).
 */
@Composable
private fun FilterContent(
    uiState: ExperimentListUiState,
    viewModel: ExperimentListViewModel
) {
    val options = uiState.filterOptions
    val current = uiState.currentFilter

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Seri Filtresi (FLEX, SAFFIRE, BASS vb.)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Experiment series",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(
                    label = "All",
                    isSelected = current.series == null,
                    onClick = { viewModel.onSeriesSelected(null) }
                )
                options.series.take(4).forEach { s ->
                    FilterChipItem(
                        label = s,
                        isSelected = current.series == s,
                        onClick = { viewModel.onSeriesSelected(s) }
                    )
                }
            }
        }

        HorizontalDivider(color = LabBorderSubtle)

        // Malzeme Filtresi (PMMA, Heptane vb.)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Fuel material / sample",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(
                    label = "All",
                    isSelected = current.material == null,
                    onClick = { viewModel.onMaterialSelected(null) }
                )
                options.materials.take(3).forEach { m ->
                    FilterChipItem(
                        label = m.take(12),
                        isSelected = current.material == m,
                        onClick = { viewModel.onMaterialSelected(m) }
                    )
                }
            }
        }

        HorizontalDivider(color = LabBorderSubtle)

        // Yerçekimi Seviyesi Filtresi
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Gravity environment",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChipItem(
                    label = "All",
                    isSelected = current.gravityLevel == null,
                    onClick = { viewModel.onGravityLevelSelected(null) }
                )
                options.gravityLevels.forEach { g ->
                    FilterChipItem(
                        label = g,
                        isSelected = current.gravityLevel == g,
                        onClick = { viewModel.onGravityLevelSelected(g) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filtreleri Temizle Butonu
        FlameOutlinedButton(
            text = "Reset filters",
            onClick = { viewModel.clearFilters() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) AccentTealMuted else LabSurface)
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) AccentTeal else LabBorder
                ),
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AccentTeal else TextDarkAnthracite,
            fontSize = 11.sp
        )
    }
}

/**
 * Deney Kartları Grid / Liste Görünümü
 */
@Composable
private fun ExperimentGridContent(
    uiState: ExperimentListUiState,
    onClearFilters: () -> Unit,
    onRefresh: () -> Unit,
    onExperimentClick: (Experiment) -> Unit,
    onToggleSelect: (String) -> Unit,
    isWide: Boolean
) {
    if (uiState.isLoading && uiState.experiments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
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
                    text = "Querying the Supabase database...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMediumGray
                )
            }
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FlameCard(modifier = Modifier.widthIn(max = 420.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Connection Error",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = RiskHighRed
                    )
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumGray
                    )
                    FlamePrimaryButton(
                        text = "Try Again",
                        onClick = onRefresh
                    )
                }
            }
        }
        return
    }

    if (uiState.experiments.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FlameCard(modifier = Modifier.widthIn(max = 460.dp)) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "No Records Found",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite
                    )
                    Text(
                        text = "No NASA records match the selected filters or search query.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMediumGray
                    )
                    FlamePrimaryButton(
                        text = "Clear Filters",
                        onClick = onClearFilters
                    )
                }
            }
        }
        return
    }

    val bottomPadding = if (uiState.selectedExperimentIds.isNotEmpty()) 92.dp else 24.dp

    if (isWide) {
        // Geniş ekran: 2-3 sütunlu adaptive grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 340.dp),
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(uiState.experiments, key = { it.id }) { exp ->
                val isSelected = uiState.selectedExperimentIds.contains(exp.id)
                ExperimentItemCard(
                    experiment = exp,
                    isSelected = isSelected,
                    onToggleSelect = { onToggleSelect(exp.id) },
                    onClick = { onExperimentClick(exp) }
                )
            }
        }
    } else {
        // Dar ekran: Tek sütun dikey liste
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = bottomPadding)
        ) {
            items(uiState.experiments, key = { it.id }) { exp ->
                val isSelected = uiState.selectedExperimentIds.contains(exp.id)
                ExperimentItemCard(
                    experiment = exp,
                    isSelected = isSelected,
                    onToggleSelect = { onToggleSelect(exp.id) },
                    onClick = { onExperimentClick(exp) }
                )
            }
        }
    }
}

/**
 * Tek Bir NASA Deney Kaydı Kartı
 * Gösterilen gerçek veriler: Başlık, seri rozeti, malzeme rozeti, yıl, AI özeti / metin durumu, risk rozeti, rapor seçim kutusu.
 */
@Composable
fun ExperimentItemCard(
    experiment: Experiment,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onToggleSelect: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    FlameCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        borderColor = if (isSelected) AccentOrangeRed else LabBorder,
        borderWidth = if (isSelected) 1.5.dp else 1.dp,
        backgroundColor = if (isSelected) AccentOrangeRedMuted.copy(alpha = 0.12f) else LabSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Üst Satır: Seri Etiketi + Malzeme + Yıl + Seçim Kutusu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FlameSeriesBadge(series = experiment.series)
                    FlameMaterialBadge(material = experiment.material)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (experiment.year != null) {
                        Text(
                            text = "${experiment.year}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMediumGray,
                            fontSize = 11.sp
                        )
                    }

                    if (onToggleSelect != null) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) AccentOrangeRed else LabBackground)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentOrangeRed else LabBorder
                                    ),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickable { onToggleSelect() },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Deney Başlığı (Gerçek NTRS / PSI Başlığı)
            Text(
                text = experiment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 3. AI Özeti / Durum Bilgisi
            val summary = experiment.summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LabBackground)
                    .border(BorderStroke(1.dp, LabBorderSubtle), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                when {
                    summary != null && summary.status == SummaryStatus.OK && summary.aiSummary.isNotBlank() -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "✦ AI SUMMARY",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentOrangeRed,
                                    fontSize = 10.sp
                                )
                            }
                            Text(
                                text = summary.aiSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextDarkAnthracite,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    summary != null && summary.status == SummaryStatus.INSUFFICIENT_DATA -> {
                        Text(
                            text = "Not enough content to summarize this document (insufficient data).",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumGray,
                            fontSize = 12.sp
                        )
                    }

                    experiment.rawText != null && experiment.rawText.length > 300 -> {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "DOCUMENT TEXT EXTRACTED (AI summary pending)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentTeal,
                                fontSize = 10.sp
                            )
                            Text(
                                text = experiment.rawText.take(160).replace("\n", " ") + "...",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 12.sp
                            )
                        }
                    }

                    else -> {
                        Text(
                            text = "PDF text analysis pending...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = LabBorderSubtle)

            // 4. Alt Satır: Risk Rozeti + Kaynak Linki Bilgisi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Risk Badge veya Durum Etiketi
                if (summary != null && summary.status == SummaryStatus.OK) {
                    FlameRiskBadge(
                        score = summary.riskScore,
                        label = summary.riskLabel
                    )
                } else if (summary != null && summary.status == SummaryStatus.INSUFFICIENT_DATA) {
                    FlameStatusBadge(text = "Insufficient data", isWarning = true)
                } else {
                    FlameStatusBadge(text = "Risk pending", isWarning = false)
                }

                // NASA Kaynak URL Göstergesi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "NASA NTRS ↗",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentTeal,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
