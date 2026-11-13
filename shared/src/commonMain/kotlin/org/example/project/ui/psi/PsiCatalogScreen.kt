package org.example.project.ui.psi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.models.PsiInvestigation
import org.example.project.ui.components.FlameCard
import org.example.project.ui.components.FlameOutlinedButton
import org.example.project.ui.components.FlamePrimaryButton
import org.example.project.ui.components.FlameSearchField
import org.example.project.ui.components.FlameSeriesBadge
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

private sealed interface PsiRoute {
    data object Catalog : PsiRoute
    data class Detail(val psiId: String) : PsiRoute
    data class Compare(val ids: List<String>) : PsiRoute
}

/**
 * NASA PSI bölümü: Katalog -> Detay -> Karşılaştırma (iç gezinme).
 * onAskAi: AI asistanına hazır soru gönderir ve paneli açar.
 */
@Composable
fun PsiHubScreen(
    onAskAi: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PsiCatalogViewModel = koinInject()
) {
    var stack by remember { mutableStateOf(listOf<PsiRoute>(PsiRoute.Catalog)) }
    val push: (PsiRoute) -> Unit = { stack = stack + it }
    val pop: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }

    when (val route = stack.last()) {
        PsiRoute.Catalog -> PsiCatalogScreen(
            viewModel = viewModel,
            onOpen = { push(PsiRoute.Detail(it)) },
            onCompare = { push(PsiRoute.Compare(it)) },
            modifier = modifier
        )
        is PsiRoute.Detail -> PsiDetailScreen(
            psiId = route.psiId,
            onBack = pop,
            onOpenRelated = { push(PsiRoute.Detail(it)) },
            onAskAi = onAskAi,
            modifier = modifier
        )
        is PsiRoute.Compare -> PsiCompareScreen(
            psiIds = route.ids,
            onBack = pop,
            onOpen = { push(PsiRoute.Detail(it)) },
            onAskAi = onAskAi,
            modifier = modifier
        )
    }
}

@Composable
fun PsiCatalogScreen(
    viewModel: PsiCatalogViewModel,
    onOpen: (String) -> Unit,
    onCompare: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showMobileFilters by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(LabBackground)) {
        val wide = maxWidth >= 860.dp
        Row(Modifier.fillMaxSize()) {
            if (wide) {
                Box(
                    Modifier.width(300.dp).fillMaxHeight().background(LabSurface)
                        .border(BorderStroke(1.dp, LabBorder))
                ) {
                    PsiFilterPanel(state, viewModel, Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp))
                }
            }
            Column(Modifier.weight(1f).fillMaxHeight().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FlameSearchField(
                        value = state.filter.query ?: "",
                        onValueChange = viewModel::onQuery,
                        placeholder = "Search PSI experiments, acronyms (FLEX, BASS…) or fuels...",
                        modifier = Modifier.weight(1f)
                    )
                    if (!wide) {
                        FlameOutlinedButton(
                            text = if (state.activeFilterCount > 0) "Filters (${state.activeFilterCount})" else "Filters",
                            onClick = { showMobileFilters = !showMobileFilters }
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip("${state.items.size} / ${state.totalCount} NASA PSI experiments", emphasized = true)
                    if (state.selectedIds.isEmpty()) {
                        Text("Select up to 3 experiments to compare", fontSize = 12.sp, color = TextMediumGray)
                    }
                }
                AnimatedVisibility(!wide && showMobileFilters) {
                    FlameCard(Modifier.fillMaxWidth()) {
                        PsiFilterPanel(state, viewModel, Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(14.dp))
                    }
                }
                Box(Modifier.weight(1f)) {
                    when {
                        state.isLoading && state.items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                            CircularProgressIndicator(color = AccentOrangeRed)
                        }
                        state.items.isEmpty() -> NoticeBox(
                            "No results",
                            if (state.totalCount == 0) "No PSI data found in Supabase. Make sure the psi_investigations migration and seed SQL have been run."
                            else "No PSI experiments match the selected filters.",
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 340.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 96.dp)
                        ) {
                            items(state.items, key = { it.psiId }) { inv ->
                                PsiCard(
                                    inv = inv,
                                    selected = inv.psiId in state.selectedIds,
                                    selectionFull = state.selectedIds.size >= 3,
                                    onClick = { onOpen(inv.psiId) },
                                    onToggleSelect = { viewModel.toggleSelection(inv.psiId) }
                                )
                            }
                        }
                    }
                    if (state.selectedIds.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = LabSurface,
                            border = BorderStroke(1.dp, AccentOrangeRed.copy(alpha = 0.4f)),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("${state.selectedIds.size} selected", fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
                                FlameOutlinedButton("Clear", onClick = viewModel::clearSelection)
                                FlamePrimaryButton(
                                    "Compare",
                                    onClick = { onCompare(state.selectedIds) },
                                    enabled = state.selectedIds.size >= 2
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsiFilterPanel(state: PsiCatalogUiState, vm: PsiCatalogViewModel, modifier: Modifier) {
    val f = state.filter
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Filters", fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
            if (state.activeFilterCount > 0) LinkText("Clear", vm::clearFilters)
        }
        FilterSection("Fire-safety category") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.options.categories.forEach { c ->
                    SelectableChip(c.label, c in f.categories, { vm.toggleCategory(c) }, c.color())
                }
            }
        }
        FilterSection("Fuel state") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.options.fuelStates.forEach { s -> SelectableChip(s.label, s in f.fuelStates, { vm.toggleFuel(s) }) }
            }
        }
        FilterSection("Platform") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SelectableChip("All", f.flightOnly == null, { vm.setFlightOnly(null) })
                SelectableChip("Flight (ISS / Cygnus)", f.flightOnly == true, { vm.setFlightOnly(true) })
                SelectableChip("Ground study", f.flightOnly == false, { vm.setFlightOnly(false) })
            }
        }
        FilterSection("Hardware group") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.options.hardwareGroups.forEach { h -> SelectableChip(h, h in f.hardwareGroups, { vm.toggleHardware(h) }) }
            }
        }
        state.options.o2Bounds?.let { b ->
            RangeFilter(
                title = "Test O₂ range (%)",
                bounds = b,
                current = f.o2Range,
                decimals = 0,
                onChange = vm::setO2Range
            )
        }
        state.options.pressureBoundsAtm?.let { b ->
            RangeFilter(
                title = "Test pressure range (atm)",
                bounds = 0.0..minOf(b.endInclusive, 5.0),
                current = f.pressureRangeAtm,
                decimals = 2,
                onChange = vm::setPressureRange
            )
        }
        Text(
            "Range filter: an experiment is listed if its test range overlaps the selected range. Studies with unknown ranges (modeling) are not hidden.",
            fontSize = 11.sp, color = TextLightMuted
        )
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMediumGray, letterSpacing = 0.5.sp)
        content()
    }
}

@Composable
private fun RangeFilter(
    title: String,
    bounds: ClosedFloatingPointRange<Double>,
    current: ClosedFloatingPointRange<Double>?,
    decimals: Int,
    onChange: (ClosedFloatingPointRange<Double>?) -> Unit
) {
    val lo = bounds.start.toFloat()
    val hi = bounds.endInclusive.toFloat().coerceAtLeast(lo + 0.01f)
    val value = current?.let { it.start.toFloat()..it.endInclusive.toFloat() } ?: (lo..hi)
    FilterSection(title) {
        Text("${value.start.toDouble().fmt(decimals)} – ${value.endInclusive.toDouble().fmt(decimals)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDarkAnthracite)
        RangeSlider(
            value = value,
            onValueChange = { r ->
                val full = r.start <= lo + 0.0001f && r.endInclusive >= hi - 0.0001f
                onChange(if (full) null else r.start.toDouble()..r.endInclusive.toDouble())
            },
            valueRange = lo..hi,
            colors = SliderDefaults.colors(thumbColor = AccentOrangeRed, activeTrackColor = AccentOrangeRed, inactiveTrackColor = LabBorder)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PsiCard(
    inv: PsiInvestigation,
    selected: Boolean,
    selectionFull: Boolean,
    onClick: () -> Unit,
    onToggleSelect: () -> Unit
) {
    FlameCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (selected) AccentOrangeRed else LabBorder,
        backgroundColor = if (selected) AccentOrangeRedMuted.copy(alpha = 0.4f) else LabSurface,
        onClick = onClick
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FlameSeriesBadge(inv.displayName)
                InfoChip(inv.psiId)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(5.dp))
                        .background(if (selected) AccentOrangeRed else LabSurface)
                        .border(BorderStroke(1.5.dp, if (selected) AccentOrangeRed else LabBorder), RoundedCornerShape(5.dp))
                        .clickable(enabled = selected || !selectionFull, onClick = onToggleSelect),
                    contentAlignment = Alignment.Center
                ) { if (selected) Text("✓", color = LabSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            }
            Text(
                inv.title, fontWeight = FontWeight.Bold, color = TextDarkAnthracite,
                style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CategoryBadge(inv.safetyCategory)
                inv.fuelState?.let { InfoChip(it.label) }
                InfoChip(if (inv.isGroundStudy) "Ground study" else (inv.platform ?: "Flight"))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                inv.o2Text()?.let { InfoChip(it, emphasized = true) }
                inv.pressureText()?.let { InfoChip(it, emphasized = true) }
                inv.flowText()?.let { InfoChip(it, emphasized = true) }
            }
            if (inv.fuels.isNotEmpty()) {
                Text("Fuels: " + inv.fuels.joinToString(", "), fontSize = 12.sp, color = TextMediumGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${inv.testCount} test rows", fontSize = 11.sp, color = TextMediumGray)
                Text("${inv.publicationCount} publications", fontSize = 11.sp, color = TextMediumGray)
                inv.startYear?.let { Text("$it", fontSize = 11.sp, color = TextMediumGray) }
                inv.doi?.let { Text("DOI ✓", fontSize = 11.sp, color = AccentTeal, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
