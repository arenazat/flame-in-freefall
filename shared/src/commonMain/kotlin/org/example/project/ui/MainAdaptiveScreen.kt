package org.example.project.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.data.network.SupabaseProvider
import org.example.project.domain.repository.ExperimentRepository
import org.example.project.models.NavTab
import org.example.project.ui.components.AskPanel
import org.example.project.ui.components.AskPanelViewModel
import org.example.project.ui.psi.HabitatHubScreen
import org.example.project.ui.psi.HistoryEarthHubScreen
import org.example.project.ui.psi.PsiHubScreen
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import org.example.project.ui.screens.AboutScreen
import org.example.project.ui.screens.ExploreScreen
import org.example.project.ui.screens.RiskRankingScreen
import org.example.project.ui.screens.TimelineScreen
import org.example.project.ui.theme.*
import org.example.project.utils.WindowSizeClass
import org.example.project.utils.calculateWindowSizeClass
import org.koin.compose.koinInject

@Composable
fun MainAdaptiveScreen(
    modifier: Modifier = Modifier,
    supabaseProvider: SupabaseProvider = koinInject(),
    repository: ExperimentRepository = koinInject(),
    askPanelViewModel: AskPanelViewModel = koinInject()
) {
    var selectedTab by remember { mutableStateOf(NavTab.PSI_CATALOG) }
    var isAskPanelOpen by remember { mutableStateOf(false) }
    val onAskAi: (String) -> Unit = { question ->
        isAskPanelOpen = true
        askPanelViewModel.sendQuestion(question)
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(LabBackground)
    ) {
        val windowSizeClass = calculateWindowSizeClass(maxWidth)

        when (windowSizeClass) {
            WindowSizeClass.COMPACT -> {
                // Mobil / Kompakt Görünüm
                Scaffold(
                    topBar = {
                        CompactTopBar(
                            isSupabaseInit = supabaseProvider.isInitialized,
                            isRepoInit = repository.isSupabaseConnected(),
                            isAskPanelOpen = isAskPanelOpen,
                            onToggleAskPanel = { isAskPanelOpen = !isAskPanelOpen }
                        )
                    },
                    bottomBar = {
                        CompactBottomBar(
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    },
                    floatingActionButton = {
                        GlobalAskFab(
                            isOpen = isAskPanelOpen,
                            onClick = { isAskPanelOpen = !isAskPanelOpen }
                        )
                    },
                    containerColor = LabBackground
                ) { innerPadding ->
                    TabContent(
                        selectedTab = selectedTab,
                        onAskAi = onAskAi,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                // Mobil AskPanel (ModalBottomSheet)
                AskPanel(
                    isOpen = isAskPanelOpen,
                    onClose = { isAskPanelOpen = false },
                    isWideScreen = false
                )
            }

            WindowSizeClass.MEDIUM -> {
                // Tablet / Orta Boyut Görünüm
                Row(modifier = Modifier.fillMaxSize()) {
                    MediumNavigationRail(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(LabBackground)
                    ) {
                        DashboardHeader(
                            selectedTab = selectedTab,
                            isSupabaseInit = supabaseProvider.isInitialized,
                            isConfigured = supabaseProvider.isReadyForQueries,
                            isAskPanelOpen = isAskPanelOpen,
                            onToggleAskPanel = { isAskPanelOpen = !isAskPanelOpen }
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                TabContent(
                                    selectedTab = selectedTab,
                                    onAskAi = onAskAi,
                                    modifier = Modifier.fillMaxSize()
                                )
                                GlobalAskFab(
                                    isOpen = isAskPanelOpen,
                                    onClick = { isAskPanelOpen = !isAskPanelOpen },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(24.dp)
                                )
                            }
                            AskPanel(
                                isOpen = isAskPanelOpen,
                                onClose = { isAskPanelOpen = false },
                                isWideScreen = true
                            )
                        }
                    }
                }
            }

            WindowSizeClass.EXPANDED -> {
                // Masaüstü / Web Dashboard Görünümü (Birincil Halka Açık Ekran)
                Row(modifier = Modifier.fillMaxSize()) {
                    ExpandedSidebar(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        isSupabaseInit = supabaseProvider.isInitialized,
                        isConfigured = supabaseProvider.isReadyForQueries,
                        isAskPanelOpen = isAskPanelOpen,
                        onToggleAskPanel = { isAskPanelOpen = !isAskPanelOpen }
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(LabBackground)
                    ) {
                        DashboardHeader(
                            selectedTab = selectedTab,
                            isSupabaseInit = supabaseProvider.isInitialized,
                            isConfigured = supabaseProvider.isReadyForQueries,
                            isAskPanelOpen = isAskPanelOpen,
                            onToggleAskPanel = { isAskPanelOpen = !isAskPanelOpen }
                        )
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                TabContent(
                                    selectedTab = selectedTab,
                                    onAskAi = onAskAi,
                                    modifier = Modifier.fillMaxSize()
                                )
                                GlobalAskFab(
                                    isOpen = isAskPanelOpen,
                                    onClick = { isAskPanelOpen = !isAskPanelOpen },
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(24.dp)
                                )
                            }
                            AskPanel(
                                isOpen = isAskPanelOpen,
                                onClose = { isAskPanelOpen = false },
                                isWideScreen = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: NavTab,
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = selectedTab,
        modifier = modifier
    ) { tab ->
        when (tab) {
            NavTab.PSI_CATALOG -> PsiHubScreen(onAskAi = onAskAi)
            NavTab.HABITAT -> HabitatHubScreen(onAskAi = onAskAi)
            NavTab.HISTORY_EARTH -> HistoryEarthHubScreen(onAskAi = onAskAi)
            NavTab.EXPLORE -> ExploreScreen()
            NavTab.TIMELINE -> TimelineScreen()
            NavTab.RISK_RANKING -> RiskRankingScreen()
            NavTab.ABOUT -> AboutScreen()
        }
    }
}

@Composable
private fun CompactTopBar(
    isSupabaseInit: Boolean,
    isRepoInit: Boolean,
    isAskPanelOpen: Boolean = false,
    onToggleAskPanel: () -> Unit = {}
) {
    Surface(
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MicrogravityFlameIcon(size = 28)
                Column {
                    Text(
                        text = "FLAME IN FREEFALL",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite
                    )
                    Text(
                        text = "NASA Combustion Safety Dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumGray
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // AI Asistan Butonu
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAskPanelOpen) AccentOrangeRedMuted else LabBackground,
                    border = BorderStroke(
                        1.dp,
                        if (isAskPanelOpen) AccentOrangeRed else LabBorder
                    ),
                    modifier = Modifier.clickable(onClick = onToggleAskPanel)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "✦", color = AccentOrangeRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isAskPanelOpen) AccentOrangeRed else TextDarkAnthracite
                        )
                    }
                }

                InitStatusBadge(
                    label = if (isRepoInit) "Live" else "Connecting",
                    isOnline = isRepoInit
                )
            }
        }
    }
}

@Composable
private fun CompactBottomBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    // 7 sekme mobilde sığmadığı için yatay kaydırılabilir alt çubuk
    Surface(color = LabSurface, border = BorderStroke(1.dp, LabBorder)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = selectedTab == tab
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) AccentOrangeRedMuted else Color.Transparent)
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentOrangeRed else TextMediumGray)
                    )
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextDarkAnthracite else TextMediumGray
                    )
                }
            }
        }
    }
}

@Composable
private fun MediumNavigationRail(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit
) {
    NavigationRail(
        containerColor = LabSurface,
        header = {
            Spacer(modifier = Modifier.height(16.dp))
            MicrogravityFlameIcon(size = 36)
            Spacer(modifier = Modifier.height(16.dp))
        }
    ) {
        NavTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            NavigationRailItem(
                selected = isSelected,
                onClick = { onTabSelected(tab) },
                icon = {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentOrangeRed else TextMediumGray)
                    )
                },
                label = { Text(tab.title) },
                colors = NavigationRailItemDefaults.colors(
                    selectedTextColor = TextDarkAnthracite,
                    unselectedTextColor = TextMediumGray,
                    indicatorColor = AccentOrangeRedMuted
                )
            )
        }
    }
}

@Composable
private fun ExpandedSidebar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    isSupabaseInit: Boolean,
    isConfigured: Boolean,
    isAskPanelOpen: Boolean = false,
    onToggleAskPanel: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Marka & Navigasyon
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Marka Başlığı
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    MicrogravityFlameIcon(size = 42)
                    Column {
                        Text(
                            text = "FLAME IN FREEFALL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = TextDarkAnthracite,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "NASA Microgravity Lab",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                HorizontalDivider(color = LabBorder)

                // Navigasyon Öğeleri
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "DASHBOARD SECTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMediumGray,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )

                    NavTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        SidebarTabItem(
                            tab = tab,
                            isSelected = isSelected,
                            onClick = { onTabSelected(tab) }
                        )
                    }
                }

                // AI Asistan Hızlı Butonu
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAskPanelOpen) AccentOrangeRedMuted else LabBackground,
                    border = BorderStroke(
                        1.dp,
                        if (isAskPanelOpen) AccentOrangeRed.copy(alpha = 0.5f) else LabBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleAskPanel)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✦", color = AccentOrangeRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Column {
                            Text(
                                text = "NASA AI Assistant (RAG)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isAskPanelOpen) AccentOrangeRed else TextDarkAnthracite
                            )
                            Text(
                                text = if (isAskPanelOpen) "Hide panel" else "Open Q&A panel",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMediumGray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Sistem Durum Kartı (Laboratuvar Panel Stili)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "SYSTEM & DATA STORE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMediumGray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LabBackground,
                    border = BorderStroke(1.dp, LabBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusRow(
                            label = "Koin DI",
                            value = "Connected",
                            isSuccess = true
                        )
                        StatusRow(
                            label = "Supabase DB",
                            value = if (isConfigured) "Live" else "Connecting",
                            isSuccess = isConfigured
                        )
                        StatusRow(
                            label = "Platform",
                            value = "Web Wasm / Android",
                            isSuccess = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SidebarTabItem(
    tab: NavTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) AccentOrangeRedMuted
                else Color.Transparent
            )
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) AccentOrangeRed.copy(alpha = 0.3f) else Color.Transparent
                ),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) AccentOrangeRed else TextMediumGray)
            )
            Column {
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) TextDarkAnthracite else TextMediumGray
                )
                Text(
                    text = tab.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) AccentOrangeRed else TextLightMuted,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    selectedTab: NavTab,
    isSupabaseInit: Boolean,
    isConfigured: Boolean,
    isAskPanelOpen: Boolean = false,
    onToggleAskPanel: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = LabSurface,
        border = BorderStroke(1.dp, LabBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = selectedTab.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkAnthracite
                )
                Text(
                    text = selectedTab.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMediumGray
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // AI Asistan Toggle Butonu
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isAskPanelOpen) AccentOrangeRedMuted else LabSurface,
                    border = BorderStroke(
                        1.dp,
                        if (isAskPanelOpen) AccentOrangeRed else LabBorder
                    ),
                    modifier = Modifier.clickable(onClick = onToggleAskPanel)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "✦",
                            color = AccentOrangeRed,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isAskPanelOpen) "Hide AI Assistant" else "NASA AI Assistant",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isAskPanelOpen) AccentOrangeRed else TextDarkAnthracite
                        )
                    }
                }

                InitStatusBadge(
                    label = "System: Active",
                    isOnline = true
                )
                InitStatusBadge(
                    label = if (isConfigured) "Database: Live" else "Awaiting connection",
                    isOnline = isConfigured
                )
            }
        }
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isSuccess: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMediumGray
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isSuccess) RiskLowGreen else RiskHighRed)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isSuccess) RiskLowGreen else RiskHighRed
            )
        }
    }
}

@Composable
private fun InitStatusBadge(
    label: String,
    isOnline: Boolean
) {
    val color = if (isOnline) RiskLowGreen else RiskMediumAmber
    val bg = if (isOnline) RiskLowGreenBg else RiskMediumAmberBg

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
private fun MicrogravityFlameIcon(
    size: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        AccentOrangeRed.copy(alpha = 0.8f),
                        AccentTeal.copy(alpha = 0.4f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, AccentTeal.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((size * 0.45).dp)
                .clip(CircleShape)
                .background(AccentOrangeRed)
        )
    }
}

/**
 * Global Floating Action Button for RAG AI Assistant
 * Erişilebilirlik kuralı: Tüm ekranlarda (mobil ve geniş ekran) yer alır, AskPanel'i açıp kapatır.
 */
@Composable
private fun GlobalAskFab(
    isOpen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        text = {
            Text(
                text = if (isOpen) "Close Assistant" else "NASA AI Assistant",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        },
        icon = {
            Text(
                text = if (isOpen) "✕" else "✦",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        },
        onClick = onClick,
        modifier = modifier.then(
            if (isOpen) Modifier.border(1.dp, AccentOrangeRed, RoundedCornerShape(24.dp))
            else Modifier
        ),
        shape = RoundedCornerShape(24.dp),
        containerColor = if (isOpen) LabSurface else AccentOrangeRed,
        contentColor = if (isOpen) AccentOrangeRed else LabSurface,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
    )
}

