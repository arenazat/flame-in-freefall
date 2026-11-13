package org.example.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.components.FlameCard
import org.example.project.ui.components.FlameRiskBadge
import org.example.project.ui.components.FlameSeriesBadge
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 3.6 — "Hakkında" Ekranı (AboutScreen)
 *
 * - Açık Tema Tasarım Sistemi (Light Theme: LabBackground, LabSurface, LabBorder, AccentOrangeRed, AccentTeal)
 * - Veri kaynaklarını (NTRS, PSI) gösterir.
 * - Toplam kaç GERÇEK deney kaydının işlendiğini (ExperimentRepository'den canlı sayı çekerek) gösterir.
 * - Gerçek Supabase verisiyle çalışır; sıfır mock/hardcoded veri.
 */
@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    viewModel: AboutViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Üst Misyon Başlığı
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentOrangeRed)
                )
                Text(
                    text = "MISSION & DATA SCIENCE DOCUMENTATION",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentOrangeRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Text(
                text = "About Flame in Freefall",
                style = MaterialTheme.typography.headlineMedium,
                color = TextDarkAnthracite,
                fontWeight = FontWeight.Black
            )

            Text(
                text = "A public analytics and RAG assistant platform that brings together decades of NASA combustion-safety research conducted on the International Space Station (ISS) and other microgravity vehicles.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMediumGray,
                lineHeight = 22.sp
            )
        }

        // 1b. NASA PSI veri kaynağı (birincil kaynak)
        org.example.project.ui.psi.PsiAboutSection()

        // 2. Canlı Supabase Veritabanı İstatistikleri (GERÇEK VERİ)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE DATABASE STATISTICS",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentOrangeRed
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isConnected) RiskLowGreen else RiskMediumAmber)
                        )
                        Text(
                            text = if (uiState.isConnected) "Supabase live data" else "Connecting",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.isConnected) RiskLowGreen else RiskMediumAmber,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Metrik Kartları Grid / Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveMetricCard(
                    title = "Total NTRS records",
                    value = uiState.totalExperiments.toString(),
                    subtitle = "Verified NASA records",
                    accentColor = AccentOrangeRed,
                    modifier = Modifier.weight(1f)
                )
                LiveMetricCard(
                    title = "Full PDF text extracted",
                    value = uiState.textExtractedCount.toString(),
                    subtitle = "With Apache PDFBox",
                    accentColor = AccentTeal,
                    modifier = Modifier.weight(1f)
                )
                LiveMetricCard(
                    title = "AI summary & risk analysis",
                    value = uiState.summaryCount.toString(),
                    subtitle = "RAG & pgvector ready",
                    accentColor = RiskLowGreen,
                    modifier = Modifier.weight(1f)
                )
                LiveMetricCard(
                    title = "Needs review",
                    value = uiState.needsReviewCount.toString(),
                    subtitle = "Scanned / OCR documents",
                    accentColor = RiskMediumAmber,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3. Veri Kaynakları (NASA NTRS & NASA PSI)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "SCIENTIFIC DATA SOURCES",
                style = MaterialTheme.typography.labelSmall,
                color = TextMediumGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // NASA NTRS Kartı
                DataSourceCard(
                    title = "NASA Technical Reports Server (NTRS)",
                    badge = "${uiState.ntrsCount} records",
                    url = "https://ntrs.nasa.gov",
                    description = "NASA's official archive of technical reports, conference papers and peer-reviewed publications. The FLEX, SAFFIRE, BASS and ACME records in Flame in Freefall were pulled directly from the NTRS REST API, and the full text was extracted from the official PDF reports.",
                    onOpenUrl = { uriHandler.openUri("https://ntrs.nasa.gov") },
                    modifier = Modifier.weight(1f)
                )

                // NASA PSI Kartı
                DataSourceCard(
                    title = "Physical Sciences Informatics (PSI)",
                    badge = "PSI catalog",
                    url = "https://psi.nasa.gov",
                    description = "NASA's open data repository holding raw sensor telemetry, high-speed camera imagery and test-chamber data from combustion, fluids and materials science experiments flown on the ISS and other spaceflights.",
                    onOpenUrl = { uriHandler.openUri("https://psi.nasa.gov") },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 4. Veritabanındaki Gerçek Deney Serileri Dağılımı
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "MICROGRAVITY EXPERIMENT PROGRAMS COVERED",
                style = MaterialTheme.typography.labelSmall,
                color = TextMediumGray,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SeriesProgramCard(
                    series = "FLEX",
                    title = "Flame Extinction Experiment",
                    description = "Spherical flame extinction and burning dynamics of free droplets in the ISS Combustion Integrated Rack (CIR).",
                    count = uiState.seriesCounts["FLEX"] ?: 0,
                    modifier = Modifier.weight(1f)
                )
                SeriesProgramCard(
                    series = "SAFFIRE",
                    title = "Spacecraft Fire Safety",
                    description = "Large-scale, uncrewed material flame-spread tests aboard Cygnus resupply vehicles.",
                    count = uiState.seriesCounts["SAFFIRE"] ?: 0,
                    modifier = Modifier.weight(1f)
                )
                SeriesProgramCard(
                    series = "BASS",
                    title = "Burning and Suppression of Solids",
                    description = "Ignition, steady burning limits and fire suppression of solid materials in microgravity.",
                    count = uiState.seriesCounts["BASS"] ?: 0,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 5. Teknik Mimari ve Güvenlik İlkeleri
        FlameCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentTeal)
                    )
                    Text(
                        text = "TECHNOLOGY ARCHITECTURE & LAYERS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "• Kotlin Multiplatform + Compose Multiplatform (Web/Wasm, Android and Desktop from one shared codebase)\n" +
                            "• Material 3 adaptive layout (sidebar + assistant panel on wide screens; bottom navigation + bottom sheet on narrow screens)\n" +
                            "• Supabase data store (PostgreSQL, pgvector HNSW cosine-similarity search, Edge Functions)\n" +
                            "• Scientific RAG Q&A engine (uses only retrieved, verified NASA text and never invents an answer it cannot cite)\n" +
                            "• Zero mock data: every filter, metric and ranking in the app comes from the live Supabase database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDarkAnthracite,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

/**
 * Canlı Metrik Kartı
 */
@Composable
private fun LiveMetricCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = TextMediumGray,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = accentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextLightMuted,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Veri Kaynağı Kartı (NTRS / PSI)
 */
@Composable
private fun DataSourceCard(
    title: String,
    badge: String,
    url: String,
    description: String,
    onOpenUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AccentTealMuted,
                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentTeal,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumGray,
                lineHeight = 20.sp
            )

            HorizontalDivider(color = LabBorderSubtle)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onOpenUrl)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Open official portal: $url ↗",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Seri Program Kartı (FLEX, SAFFIRE, BASS)
 */
@Composable
private fun SeriesProgramCard(
    series: String,
    title: String,
    description: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlameSeriesBadge(series = series)
                Text(
                    text = if (count > 0) "$count records" else "In data store",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    fontSize = 11.sp
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite
            )

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMediumGray,
                lineHeight = 18.sp
            )
        }
    }
}
