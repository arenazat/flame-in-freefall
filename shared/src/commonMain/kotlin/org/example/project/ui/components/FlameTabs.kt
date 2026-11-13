package org.example.project.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.*

data class FlameTabItem<T>(
    val value: T,
    val title: String,
    val icon: String? = null,
    val count: Int? = null
)

/**
 * Açık tema laboratuvar sekme bileşeni (FlameTabs).
 * Zemin: Beyaz (#FFFFFF), Alt sınır çizgisi: İnce gri (#E2E5EA)
 * Aktif gösterge: Turuncu-kırmızı (#E8572E)
 * Aktif metin: Koyu antrasit (#1B1F27), Pasif metin: Orta gri (#6B7280)
 */
@Composable
fun <T> FlameTabs(
    tabs: List<FlameTabItem<T>>,
    selectedTab: T,
    onTabSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentOrangeRed
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LabSurface)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.value == selectedTab

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) TextDarkAnthracite else TextMediumGray
                )

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .clickable { onTabSelected(tab.value) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tab.icon != null) {
                            Text(
                                text = tab.icon,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )

                        if (tab.count != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AccentTealMuted else LabSurfaceSubtle)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${tab.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AccentTeal else TextMediumGray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Aktif alt gösterge çizgisi
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp)
                            .background(if (isSelected) activeColor else Color.Transparent)
                    )
                }
            }
        }

        // Sekmeler altındaki ince gri taban çizgisi
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(LabBorder)
        )
    }
}
