package org.example.project.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ==============================================================================
// Flame in Freefall — Açık Tema (Light Theme) Tasarım Sistemi
// Kesin kural: Temiz, laboratuvar/güvenlik paneli havası. Koyu tema eklenmez.
// ==============================================================================

val FlameLabLightColorScheme = lightColorScheme(
    primary = AccentOrangeRed,           // #E8572E (sadece vurgu ve ikonlarda)
    onPrimary = LabSurface,              // #FFFFFF
    primaryContainer = AccentOrangeRedMuted,
    onPrimaryContainer = AccentOrangeRed,

    secondary = AccentTeal,              // #1E88C7 (koyu camgöbeği)
    onSecondary = LabSurface,
    secondaryContainer = AccentTealMuted,
    onSecondaryContainer = AccentTeal,

    tertiary = RiskMediumAmber,          // #C98A1A (orta risk amber)
    onTertiary = LabSurface,
    tertiaryContainer = RiskMediumAmberBg,
    onTertiaryContainer = RiskMediumAmber,

    error = RiskHighRed,                 // #D64545 (yüksek risk kırmızı)
    onError = LabSurface,
    errorContainer = RiskHighRedBg,
    onErrorContainer = RiskHighRed,

    background = LabBackground,          // #F7F8FA (kırık beyaz)
    onBackground = TextDarkAnthracite,   // #1B1F27 (koyu antrasit)

    surface = LabSurface,                // #FFFFFF (beyaz)
    onSurface = TextDarkAnthracite,      // #1B1F27 (koyu antrasit)
    surfaceVariant = LabSurfaceSubtle,   // #F1F3F6 (ince gri zemin)
    onSurfaceVariant = TextMediumGray,   // #6B7280 (orta gri)

    outline = LabBorder,                 // #E2E5EA (ince gri kenarlık)
    outlineVariant = LabBorderSubtle     // #ECEFF3
)

@Composable
fun FlameFallTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = FlameLabLightColorScheme,
        content = content
    )
}
