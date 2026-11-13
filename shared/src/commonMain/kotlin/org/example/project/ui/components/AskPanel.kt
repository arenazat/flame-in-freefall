package org.example.project.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.RagSourceReference
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/**
 * Faz 3.5 — Soru-Cevap Paneli (RAG AskPanel)
 *
 * - Geniş ekranda sağda sabit/açılır yan panel, dar ekranda alttan açılan bottom sheet.
 * - Mesaj geçmişi (kullanıcı/AI farklı hizalama ve renk).
 * - Her AI cevabının altında GERÇEK kaynak deneylere tıklanabilir "Kaynaklar:" linkleri.
 * - RagRepository'ye bağlı: "yeterli veri yok" dönerse arayüzde bunu net gösterir.
 * - Cevap beklenirken "düşünüyor..." göstergesi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskPanel(
    isOpen: Boolean,
    onClose: () -> Unit,
    isWideScreen: Boolean,
    modifier: Modifier = Modifier,
    viewModel: AskPanelViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (isWideScreen) {
        // Geniş Ekran (Web / Masaüstü): Sağ Yan Panel (Animated Slide-in)
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = modifier
        ) {
            Surface(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight(),
                color = LabSurface,
                border = BorderStroke(1.dp, LabBorder)
            ) {
                AskPanelContent(
                    uiState = uiState,
                    onClose = onClose,
                    onInputChange = { viewModel.onInputChange(it) },
                    onSend = { viewModel.sendQuestion() },
                    onQuickQuestion = { viewModel.sendQuestion(it) }
                )
            }
        }
    } else {
        // Dar Ekran (Mobil): Alttan Açılan Bottom Sheet
        if (isOpen) {
            ModalBottomSheet(
                onDismissRequest = onClose,
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = LabSurface,
                contentColor = TextDarkAnthracite,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(LabBorder)
                    )
                }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.85f)
                ) {
                    AskPanelContent(
                        uiState = uiState,
                        onClose = onClose,
                        onInputChange = { viewModel.onInputChange(it) },
                        onSend = { viewModel.sendQuestion() },
                        onQuickQuestion = { viewModel.sendQuestion(it) }
                    )
                }
            }
        }
    }
}

/**
 * AskPanel İçerik Bileşeni (Header + Mesaj Listesi + Input)
 */
@Composable
private fun AskPanelContent(
    uiState: AskPanelUiState,
    onClose: () -> Unit,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickQuestion: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val uriHandler = LocalUriHandler.current

    // Yeni mesaj geldiğinde en alta kaydır
    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Panel Başlığı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LabSurface)
                .border(BorderStroke(1.dp, LabBorder))
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        .background(AccentOrangeRed)
                )
                Column {
                    Text(
                        text = "NASA SCIENCE ASSISTANT (RAG)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Answers grounded in verified NASA microgravity data",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumGray,
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LabSurfaceSubtle)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✕",
                    fontSize = 12.sp,
                    color = TextMediumGray,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Mesaj Geçmişi (LazyColumn)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(LabBackground)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(uiState.messages, key = { it.id }) { msg ->
                ChatBubble(
                    message = msg,
                    onOpenUrl = { url -> uriHandler.openUri(url) }
                )
            }

            // Düşünüyor... Göstergesi
            if (uiState.isThinking) {
                item {
                    ThinkingIndicatorBubble()
                }
            }
        }

        // 3. Alt Giriş ve Hızlı Soru Önerileri
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(LabSurface)
                .border(BorderStroke(1.dp, LabBorder))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Hızlı Soru Önerileri
            if (uiState.messages.size <= 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickChip(
                        text = "PMMA flame spread",
                        onClick = { onQuickQuestion("How does flame spread over PMMA behave in microgravity?") }
                    )
                    QuickChip(
                        text = "FLEX droplet burning",
                        onClick = { onQuickQuestion("How were droplet burning and extinction limits measured in the FLEX experiment?") }
                    )
                }
            }

            // Metin Giriş Alanı + Gönder Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LabBackground)
                        .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (uiState.currentInput.isEmpty()) {
                        Text(
                            text = "Ask about NASA experiments...",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumGray
                        )
                    }

                    BasicTextField(
                        value = uiState.currentInput,
                        onValueChange = onInputChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(
                            color = TextDarkAnthracite,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(AccentTeal)
                    )
                }

                FlamePrimaryButton(
                    text = "Ask ➤",
                    onClick = onSend,
                    enabled = uiState.currentInput.isNotBlank() && !uiState.isThinking
                )
            }
        }
    }
}

/**
 * Mesaj Balonu (Kullanıcı / AI)
 */
@Composable
private fun ChatBubble(
    message: ChatMessage,
    onOpenUrl: (String) -> Unit
) {
    val isUser = message.isUser

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Gönderici Etiketi
        Text(
            text = if (isUser) "You" else "✦ NASA AI Assistant",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isUser) AccentTeal else AccentOrangeRed,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 3.dp, start = if (isUser) 0.dp else 4.dp, end = if (isUser) 4.dp else 0.dp)
        )

        // Mesaj Gövdesi
        if (message.isInsufficientData) {
            // Kural: "yeterli veri yok" dönerse arayüzde bunu net göster, asla genel/uydurma cevap gibi gösterme
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RiskMediumAmberBg,
                border = BorderStroke(1.dp, RiskMediumAmber.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = "⚠️", fontSize = 14.sp)
                        Text(
                            text = "INSUFFICIENT DATA",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = RiskMediumAmber,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Text(
                        text = "The NASA dataset does not contain enough verifiable information on this topic. By design, no invented or estimated answer is generated.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDarkAnthracite,
                        lineHeight = 18.sp
                    )
                }
            }
        } else if (message.isError) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = RiskHighRedBg,
                border = BorderStroke(1.dp, RiskHighRed.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = RiskHighRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = if (isUser) 12.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 12.dp
                ),
                color = if (isUser) AccentTealMuted else LabSurface,
                border = BorderStroke(1.dp, if (isUser) AccentTeal.copy(alpha = 0.3f) else LabBorder),
                modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.95f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextDarkAnthracite,
                        lineHeight = 20.sp
                    )

                    // AI Cevabının altındaki GERÇEK Kaynaklar (source_url tıklanabilir)
                    if (!isUser && message.sources.isNotEmpty()) {
                        HorizontalDivider(color = LabBorderSubtle)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "SOURCES (NASA NTRS):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            )

                            message.sources.take(3).forEach { src ->
                                SourceReferenceItem(
                                    source = src,
                                    onOpenUrl = onOpenUrl
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI Yanıtı Altındaki Tıklanabilir Kaynak Çipi
 */
@Composable
private fun SourceReferenceItem(
    source: RagSourceReference,
    onOpenUrl: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LabBackground,
        border = BorderStroke(1.dp, LabBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (source.sourceUrl.isNotBlank()) {
                    onOpenUrl(source.sourceUrl)
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlameSeriesBadge(series = source.series)
                    Text(
                        text = source.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkAnthracite,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                text = "NTRS ↗",
                style = MaterialTheme.typography.labelSmall,
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

/**
 * "Düşünüyor..." Göstergesi
 */
@Composable
private fun ThinkingIndicatorBubble() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "✦ NASA AI Assistant",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AccentOrangeRed,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = LabSurface,
            border = BorderStroke(1.dp, LabBorder),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentOrangeRed,
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Thinking... searching NASA experiments semantically",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun QuickChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(LabBackground)
            .border(BorderStroke(1.dp, LabBorder), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = TextDarkAnthracite,
            fontSize = 11.sp
        )
    }
}
