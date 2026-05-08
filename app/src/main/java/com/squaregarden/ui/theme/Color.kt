package com.squaregarden.ui.theme

import androidx.compose.ui.graphics.Color

// Botanical garden palette (used as defaults / light theme)
val Sage = Color(0xFF8FBC8F)
val DarkSage = Color(0xFF5F8A5F)
val Cream = Color(0xFFFFF8F0)
val WarmBrown = Color(0xFF8B7355)
val SoftWhite = Color(0xFFFAF8F5)
val DeepForest = Color(0xFF2E4A2E)
val LightSage = Color(0xFFB5D5B5)

// Tile colors (vivid and saturated)
val TileRed = Color(0xFFE53935)
val TileBlue = Color(0xFF1E88E5)
val TileYellow = Color(0xFFFFB300)
val TileGreen = Color(0xFF43A047)
val TileOrange = Color(0xFFF57C00)

// Emboss highlight (bright edge for bevel)
val TileRedLight = Color(0xFFFF6F60)
val TileBlueLight = Color(0xFF64B5F6)
val TileYellowLight = Color(0xFFFFD54F)
val TileGreenLight = Color(0xFF76D275)
val TileOrangeLight = Color(0xFFFFAD42)

// Emboss shadow (deep edge for bevel)
val TileRedDark = Color(0xFFAB000D)
val TileBlueDark = Color(0xFF0D47A1)
val TileYellowDark = Color(0xFFC68400)
val TileGreenDark = Color(0xFF1B5E20)
val TileOrangeDark = Color(0xFFBB4D00)

// ── Theme color sets ──────────────────────────────────────────

data class ThemeColors(
    val id: String,
    val label: String,
    val background: Color,
    val surface: Color,
    val primary: Color,
    val primaryContainer: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val isDark: Boolean = false
)

val ThemeLight = ThemeColors(
    id = "light",
    label = "Light",
    background = Color(0xFFFFF8F0),
    surface = Color(0xFFFAF8F5),
    primary = Color(0xFF8FBC8F),
    primaryContainer = Color(0xFFB5D5B5),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF2E4A2E),
    secondary = Color(0xFF8B7355),
    onSecondary = Color.White,
    onBackground = Color(0xFF2E4A2E),
    onSurface = Color(0xFF2E4A2E),
    surfaceVariant = Color(0xFFE8E0D4),
    onSurfaceVariant = Color(0xFF8B7355)
)

val ThemeDark = ThemeColors(
    id = "dark",
    label = "Dark",
    isDark = true,
    background = Color(0xFF1A1A2E),
    surface = Color(0xFF252540),
    primary = Color(0xFF7EBEA0),
    primaryContainer = Color(0xFF3D6B55),
    onPrimary = Color(0xFF1A1A2E),
    onPrimaryContainer = Color(0xFFD0E8DC),
    secondary = Color(0xFFC9A87C),
    onSecondary = Color(0xFF1A1A2E),
    onBackground = Color(0xFFE8E8E8),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF32324A),
    onSurfaceVariant = Color(0xFFB0B0C0)
)

val ThemeSummer = ThemeColors(
    id = "summer",
    label = "Summer",
    background = Color(0xFFFFF9E6),
    surface = Color(0xFFFFFBF0),
    primary = Color(0xFFFF9E5E),
    primaryContainer = Color(0xFFFFD0A8),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF5D4037),
    secondary = Color(0xFFFF7043),
    onSecondary = Color.White,
    onBackground = Color(0xFF5D4037),
    onSurface = Color(0xFF5D4037),
    surfaceVariant = Color(0xFFFFF0D6),
    onSurfaceVariant = Color(0xFF8D6E63)
)

val ThemeWinter = ThemeColors(
    id = "winter",
    label = "Winter",
    background = Color(0xFFEDF2F7),
    surface = Color(0xFFF7FAFC),
    primary = Color(0xFF5C8DB5),
    primaryContainer = Color(0xFFB3D4F0),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF2D3748),
    secondary = Color(0xFF90CDF4),
    onSecondary = Color(0xFF2D3748),
    onBackground = Color(0xFF2D3748),
    onSurface = Color(0xFF2D3748),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF718096)
)

val ThemeFall = ThemeColors(
    id = "fall",
    label = "Fall",
    background = Color(0xFFFDF2E9),
    surface = Color(0xFFFFF5EB),
    primary = Color(0xFFC67C4E),
    primaryContainer = Color(0xFFE8B896),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF3E2723),
    secondary = Color(0xFFD4A574),
    onSecondary = Color(0xFF3E2723),
    onBackground = Color(0xFF3E2723),
    onSurface = Color(0xFF3E2723),
    surfaceVariant = Color(0xFFF0E0D0),
    onSurfaceVariant = Color(0xFF8D6E63)
)

val ThemeSpring = ThemeColors(
    id = "spring",
    label = "Spring",
    background = Color(0xFFF0FFF0),
    surface = Color(0xFFF5FFF5),
    primary = Color(0xFF66BB6A),
    primaryContainer = Color(0xFFA5D6A7),
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF1B5E20),
    secondary = Color(0xFFAED581),
    onSecondary = Color(0xFF1B5E20),
    onBackground = Color(0xFF1B5E20),
    onSurface = Color(0xFF1B5E20),
    surfaceVariant = Color(0xFFE0F0E0),
    onSurfaceVariant = Color(0xFF558B2F)
)

val allThemes = listOf(ThemeLight, ThemeDark, ThemeSummer, ThemeWinter, ThemeFall, ThemeSpring)

fun themeById(id: String): ThemeColors = allThemes.firstOrNull { it.id == id } ?: ThemeLight
