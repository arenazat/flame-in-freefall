package org.example.project.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.LabBorder
import org.example.project.ui.theme.LabSurface
import org.example.project.ui.theme.TextDarkAnthracite
import org.example.project.ui.theme.TextMediumGray

/**
 * Açık tema laboratuvar / güvenlik paneli kart bileşeni.
 * Zemin: Beyaz (#FFFFFF), Kenarlık: İnce gri (#E2E5EA).
 */
@Composable
fun FlameCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    backgroundColor: Color = LabSurface,
    borderColor: Color = LabBorder,
    borderWidth: Dp = 1.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(BorderStroke(borderWidth, borderColor), shape)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)

    Column(
        modifier = cardModifier,
        content = content
    )
}

/**
 * Başlık ve opsiyonel aksiyon alanı içeren yapılandırılmış laboratuvar kartı.
 */
@Composable
fun FlameSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    FlameCard(modifier = modifier) {
        Column(modifier = Modifier.padding(contentPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkAnthracite
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMediumGray
                        )
                    }
                }
                if (action != null) {
                    Box(modifier = Modifier.padding(start = 12.dp)) {
                        action()
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}
