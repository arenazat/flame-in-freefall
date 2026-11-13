package org.example.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.example.project.ui.theme.*

/**
 * Açık tema laboratuvar arama alanı (SearchField).
 * Zemin: Beyaz (#FFFFFF), Kenarlık: İnce gri (#E2E5EA), Odak kenarlığı: Koyu camgöbeği (#1E88C7)
 * Birincil metin: Koyu antrasit (#1B1F27), Placeholder: Orta gri (#6B7280)
 */
@Composable
fun FlameSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search experiments, materials or keywords...",
    onSearch: (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }

    val currentBorderColor = if (isFocused) AccentTeal else LabBorder

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LabSurface)
            .border(BorderStroke(1.dp, currentBorderColor), RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Arama ikonu
            Text(
                text = "🔍",
                fontSize = 14.sp
            )

            // Giriş alanı
            Box(modifier = Modifier.weight(1f)) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMediumGray
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = TextDarkAnthracite,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(AccentTeal)
                )
            }

            // Temizleme butonu
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LabSurfaceSubtle)
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 11.sp,
                        color = TextMediumGray
                    )
                }
            }
        }
    }
}
