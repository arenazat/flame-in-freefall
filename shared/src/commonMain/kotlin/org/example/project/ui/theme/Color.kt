package org.example.project.ui.theme

import androidx.compose.ui.graphics.Color

// ==============================================================================
// Flame in Freefall — Açık Tema (Light Theme) Tasarım Sistemi Renk Paleti
// Temiz, laboratuvar/güvenlik paneli estetiği
// ==============================================================================

// 1. Zemin ve Yüzey Renkleri
val LabBackground = Color(0xFFF7F8FA)        // Arka plan kırık beyaz (#F7F8FA)
val LabSurface = Color(0xFFFFFFFF)           // Kart / yüzey beyaz (#FFFFFF)
val LabBorder = Color(0xFFE2E5EA)            // İnce gri kenarlık (#E2E5EA)
val LabBorderSubtle = Color(0xFFECEFF3)      // Çok ince ayrım çizgisi
val LabSurfaceSubtle = Color(0xFFF1F3F6)     // Vurgulu yüzey / hafif gri panel

// 2. Vurgu Renkleri
val AccentOrangeRed = Color(0xFFE8572E)      // Birincil vurgu turuncu-kırmızı (sadece vurgu/ikonlarda #E8572E)
val AccentTeal = Color(0xFF1E88C7)           // İkincil vurgu koyu camgöbeği (#1E88C7)

val AccentOrangeRedMuted = Color(0xFFFDEEE9) // Açık turuncu-kırmızı zemin (badge / vurgulu buton)
val AccentTealMuted = Color(0xFFE8F4FA)      // Açık camgöbeği zemin (badge / aktif durum)

// 3. Risk Seviyeleri (Açık zeminler üzerinde)
val RiskLowGreen = Color(0xFF2E9E6C)         // Düşük risk yeşil (#2E9E6C)
val RiskLowGreenBg = Color(0xFFEAF6F0)       // Açık yeşil badge zemin
val RiskMediumAmber = Color(0xFFC98A1A)      // Orta risk amber (#C98A1A)
val RiskMediumAmberBg = Color(0xFFFAF3E6)    // Açık amber badge zemin
val RiskHighRed = Color(0xFFD64545)          // Yüksek risk kırmızı (#D64545)
val RiskHighRedBg = Color(0xFFFBECEC)        // Açık kırmızı badge zemin

// 4. Tipografi ve Metin Renkleri
val TextDarkAnthracite = Color(0xFF1B1F27)   // Birincil metin koyu antrasit (#1B1F27)
val TextMediumGray = Color(0xFF6B7280)       // İkincil metin orta gri (#6B7280)
val TextLightMuted = Color(0xFF9CA3AF)       // Üçüncül / pasif metin

// Geriye dönük uyumluluk takma adları (Açık tema eşdeğerleri)
val PlasmaCyan = AccentTeal
val PlasmaCyanVariant = AccentTeal
val PlasmaCyanMuted = AccentTealMuted
val SolarAmber = AccentOrangeRed
val SolarAmberGlow = AccentOrangeRed
val SolarAmberMuted = AccentOrangeRedMuted
val ThermalCrimson = RiskHighRed
val ThermalYellow = RiskMediumAmber
val ThermalGreenSafe = RiskLowGreen
val SpaceVoid = LabBackground
val SpaceDarkSurface = LabSurface
val SpaceCardElevated = LabSurface
val SpaceCardHighlight = LabSurfaceSubtle
val SpaceBorder = LabBorder
val TextStarWhite = TextDarkAnthracite
val TextNebulaMuted = TextMediumGray
val TextNebulaSubtle = TextLightMuted
