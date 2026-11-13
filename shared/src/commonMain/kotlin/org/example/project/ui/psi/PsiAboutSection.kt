package org.example.project.ui.psi

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.domain.usecase.GetPsiInvestigationsUseCase
import org.example.project.models.PsiInvestigation
import org.example.project.ui.components.FlameCard
import org.example.project.ui.components.FlameSectionCard
import org.example.project.ui.theme.*
import org.koin.compose.koinInject

/** Hakkında ekranı için NASA PSI veri kaynağı istatistikleri ve kapsam notları (Supabase'den canlı). */
@Composable
fun PsiAboutSection(getInvestigations: GetPsiInvestigationsUseCase = koinInject()) {
    var items by remember { mutableStateOf<List<PsiInvestigation>?>(null) }
    LaunchedEffect(Unit) { items = getInvestigations() }
    val uri = LocalUriHandler.current
    val list = items.orEmpty()

    FlameSectionCard(
        title = "Primary Data Source: NASA PSI",
        subtitle = "Physical Sciences Informatics — Combustion Science",
        action = { LinkText("psi.nasa.gov") { uri.openUri("https://psi.nasa.gov/physci/repo/search?q=combustion") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("Combustion studies", if (items == null) "…" else list.size.toString(), Modifier.weight(1f))
                StatTile("Flight experiments", if (items == null) "…" else list.count { !it.isGroundStudy }.toString(), Modifier.weight(1f))
                StatTile("Test rows", if (items == null) "…" else list.sumOf { it.testCount }.toString(), Modifier.weight(1f))
                StatTile("Publications", if (items == null) "…" else list.sumOf { it.publicationCount }.toString(), Modifier.weight(1f))
            }
            Text(
                "Citation: publications, presentations and reports using PSI data must cite the experiment's DOI (NASA SMD Open Source Science).",
                fontSize = 12.sp, color = TextMediumGray
            )
            NoticeBox("Partial gravity", PARTIAL_GRAVITY_NOTE)
            NoticeBox(
                "Out of scope",
                "Rocket and propellant fires are outside the scope of PSI data; the high velocities in rocket engines make combustion practically independent of gravity.",
                warning = false
            )
            Text(HABITAT_SCORE_NOTE, fontSize = 11.sp, color = TextLightMuted)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier) {
    FlameCard(modifier) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = AccentOrangeRed)
            Text(label, fontSize = 12.sp, color = TextMediumGray)
        }
    }
}
