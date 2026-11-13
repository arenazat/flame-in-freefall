package org.example.project.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.usecase.GetSimilarExperimentsUseCase
import org.example.project.models.Experiment
import org.example.project.models.SummaryStatus
import org.example.project.ui.components.*
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 3.3 — Deney Detay Ekranı
 *
 * - Üstte başlık + meta bilgi şeridi
 * - Altında GERÇEK AI özeti bir Card içinde (yoksa "bu deney için özet henüz üretilemedi" mesajı)
 * - "Ana Bulgu" ve "Güvenlik Çıkarımı" vurgulu kutuları
 * - En altta GERÇEK kaynak linkine (source_url, platforma göre tarayıcıda açılacak) tıklanabilir bağlantı
 * - Geniş ekranda yan panelde, dar ekranda altta "Benzer Deneyler" kısa listesi (aynı seri/malzemeden GERÇEK veri).
 */
@Composable
fun ExperimentDetailScreen(
    experiment: Experiment,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onExperimentSelected: (Experiment) -> Unit = {},
    getSimilarExperimentsUseCase: GetSimilarExperimentsUseCase = koinInject()
) {
    val uriHandler = LocalUriHandler.current

    // Benzer deneyleri Supabase'den gerçek verilerle sorgula
    var similarExperiments by remember(experiment.id) { mutableStateOf<List<Experiment>>(emptyList()) }
    var isLoadingSimilar by remember(experiment.id) { mutableStateOf(true) }

    LaunchedEffect(experiment.id) {
        isLoadingSimilar = true
        getSimilarExperimentsUseCase(experiment).collect { list ->
            similarExperiments = list
            isLoadingSimilar = false
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
            .padding(16.dp)
    ) {
        val isWideScreen = maxWidth >= 960.dp

        Column(modifier = Modifier.fillMaxSize()) {
            // Üst Navigasyon Çubuğu: Geri Butonu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FlameOutlinedButton(
                    text = "← Back to list",
                    onClick = onBack
                )

                // Kaynak bağlantısını dış tarayıcıda açma butonu
                FlamePrimaryButton(
                    text = "Open NASA source ↗",
                    onClick = {
                        try {
                            uriHandler.openUri(experiment.sourceUrl)
                        } catch (e: Exception) {
                            println("[HATA] URL açılamadı: ${e.message}")
                        }
                    }
                )
            }

            if (isWideScreen) {
                // Geniş Ekran: Sol Detaylar (Scrollable) + Sağ Benzer Deneyler Yan Paneli
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Ana Detay İçeriği
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DetailHeaderCard(experiment = experiment)
                        AiSummaryCard(experiment = experiment)
                        ComparativeChartCard(experiment = experiment)
                        RawDocumentCard(experiment = experiment)
                        SourceLinkCard(
                            sourceUrl = experiment.sourceUrl,
                            onOpenUrl = { uriHandler.openUri(experiment.sourceUrl) }
                        )
                    }

                    // Sağ Yan Panel: Benzer Deneyler
                    FlameCard(
                        modifier = Modifier
                            .width(340.dp)
                            .fillMaxHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "SIMILAR RECORDS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkAnthracite,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Other NASA records in the ${experiment.series} series",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumGray
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            SimilarExperimentsList(
                                experiments = similarExperiments,
                                isLoading = isLoadingSimilar,
                                onExperimentSelected = onExperimentSelected
                            )
                        }
                    }
                }
            } else {
                // Dar Ekran (Mobil): Tüm içerik dikey kaydırma listesi
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DetailHeaderCard(experiment = experiment)
                    AiSummaryCard(experiment = experiment)
                    ComparativeChartCard(experiment = experiment)
                    RawDocumentCard(experiment = experiment)
                    SourceLinkCard(
                        sourceUrl = experiment.sourceUrl,
                        onOpenUrl = { uriHandler.openUri(experiment.sourceUrl) }
                    )

                    // Altta Benzer Deneyler Bölümü
                    FlameCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "SIMILAR RECORDS",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkAnthracite,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Other records in the ${experiment.series} series",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMediumGray
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            SimilarExperimentsList(
                                experiments = similarExperiments,
                                isLoading = isLoadingSimilar,
                                onExperimentSelected = onExperimentSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Üst Başlık ve Meta Bilgi Şeridi Kartı
 */
@Composable
private fun DetailHeaderCard(experiment: Experiment) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Meta Bilgi Rozet Şeridi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlameSeriesBadge(series = experiment.series)
                FlameMaterialBadge(material = experiment.material)

                if (experiment.oxygenConcentration != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LabSurfaceSubtle,
                        border = BorderStroke(1.dp, LabBorder)
                    ) {
                        Text(
                            text = "${experiment.oxygenConcentration}% O₂",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrangeRed,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LabSurfaceSubtle,
                    border = BorderStroke(1.dp, LabBorder)
                ) {
                    Text(
                        text = experiment.gravityLevel,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp
                    )
                }

                if (experiment.year != null) {
                    Text(
                        text = "• Year: ${experiment.year}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Gerçek Deney Başlığı
            Text(
                text = experiment.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = TextDarkAnthracite,
                lineHeight = 30.sp
            )

            // İzlenebilirlik ID Bilgisi
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Record UUID:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightMuted,
                    fontSize = 11.sp
                )
                Text(
                    text = experiment.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * AI Özeti, Ana Bulgu ve Güvenlik Çıkarımı Vurgulu Kutuları
 */
@Composable
private fun AiSummaryCard(experiment: Experiment) {
    val summary = experiment.summary

    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "✦",
                        fontSize = 16.sp,
                        color = AccentOrangeRed
                    )
                    Text(
                        text = "AI ANALYSIS & RISK ASSESSMENT",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite,
                        letterSpacing = 0.5.sp
                    )
                }

                if (summary != null && summary.status == SummaryStatus.OK) {
                    FlameRiskBadge(
                        score = summary.riskScore,
                        label = summary.riskLabel
                    )
                }
            }

            if (summary != null && summary.status == SummaryStatus.OK) {
                // 1. Genel AI Özeti
                Text(
                    text = summary.aiSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDarkAnthracite,
                    lineHeight = 22.sp
                )

                // 2. Vurgulu Kutu: "Ana Bulgu"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentTealMuted)
                        .border(
                            BorderStroke(1.dp, AccentTeal.copy(alpha = 0.3f)),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(38.dp)
                                .background(AccentTeal)
                        )
                        Column {
                            Text(
                                text = "KEY FINDING",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = summary.keyFinding,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextDarkAnthracite
                            )
                        }
                    }
                }

                // 3. Vurgulu Kutu: "Güvenlik Çıkarımı"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AccentOrangeRedMuted)
                        .border(
                            BorderStroke(1.dp, AccentOrangeRed.copy(alpha = 0.3f)),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(38.dp)
                                .background(AccentOrangeRed)
                        )
                        Column {
                            Text(
                                text = "SAFETY IMPLICATION & COMBUSTION BEHAVIOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentOrangeRed,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Risk score: ${summary.riskScore}/100 (${summary.riskLabel}). Flame spread and flammability limits of the sample under microgravity were assessed against NASA safety standards.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextDarkAnthracite
                            )
                        }
                    }
                }
            } else if (summary != null && summary.status == SummaryStatus.INSUFFICIENT_DATA) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RiskMediumAmberBg,
                    border = BorderStroke(1.dp, RiskMediumAmber.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "No AI summary could be generated because there is not enough data for this record. It has been flagged for manual review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RiskMediumAmber,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LabBackground,
                    border = BorderStroke(1.dp, LabBorder)
                ) {
                    Text(
                        text = "No summary has been generated for this record yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMediumGray,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Çıkarılan Gerçek Doküman Metni Kartı
 */
@Composable
private fun RawDocumentCard(experiment: Experiment) {
    if (experiment.rawText.isNullOrBlank()) return

    var isExpanded by remember { mutableStateOf(false) }

    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOCUMENT TEXT (NASA NTRS)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (isExpanded) "Collapse ▲" else "Expand ▼",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentTeal,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { isExpanded = !isExpanded }
                )
            }

            val displayText = if (isExpanded) {
                experiment.rawText
            } else {
                experiment.rawText.take(400) + "...\n(Expand to read the full text)"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LabBackground)
                    .border(BorderStroke(1.dp, LabBorderSubtle), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextDarkAnthracite,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Tıklanabilir Gerçek Kaynak Linki Kartı
 */
@Composable
private fun SourceLinkCard(
    sourceUrl: String,
    onOpenUrl: () -> Unit
) {
    FlameCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OFFICIAL NASA SOURCE LINK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = sourceUrl,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentTeal,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable(onClick = onOpenUrl)
                )
            }

            FlamePrimaryButton(
                text = "Open in browser ↗",
                onClick = onOpenUrl
            )
        }
    }
}

/**
 * Benzer Deneyler Listesi
 */
@Composable
private fun SimilarExperimentsList(
    experiments: List<Experiment>,
    isLoading: Boolean,
    onExperimentSelected: (Experiment) -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = AccentTeal,
                modifier = Modifier.size(24.dp)
            )
        }
        return
    }

    if (experiments.isEmpty()) {
        Text(
            text = "No similar records found.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMediumGray
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        experiments.forEach { exp ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(LabBackground)
                    .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(8.dp))
                    .clickable { onExperimentSelected(exp) }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlameMaterialBadge(material = exp.material)
                        if (exp.year != null) {
                            Text(
                                text = "${exp.year}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Text(
                        text = exp.title,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
