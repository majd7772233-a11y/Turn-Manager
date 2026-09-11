package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF5C6BC0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8EAF6),
    onPrimaryContainer = Color(0xFF1A237E),
    secondary = Color(0xFF26A69A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F2F1),
    onSecondaryContainer = Color(0xFF004D40),
    tertiary = Color(0xFFFF9800),
    onTertiary = Color.White,
    background = Color(0xFFF5F5F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFFECEFF1),
    onSurfaceVariant = Color(0xFF455A64),
    outline = Color(0xFFB0BEC5),
    outlineVariant = Color(0xFFCFD8DC)
)

// Dark (Midnight Blue/Slate) Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7986CB),
    onPrimary = Color(0xFF0D1236),
    primaryContainer = Color(0xFF283593),
    onPrimaryContainer = Color(0xFFE8EAF6),
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF004D40),
    onSecondaryContainer = Color(0xFFE0F2F1),
    tertiary = Color(0xFFFFB74D),
    onTertiary = Color(0xFF4E2600),
    background = Color(0xFF12121A),
    onBackground = Color(0xFFE5E5EA),
    surface = Color(0xFF1A1A24),
    onSurface = Color(0xFFE5E5EA),
    surfaceVariant = Color(0xFF242432),
    onSurfaceVariant = Color(0xFFA5A5B5),
    surfaceContainer = Color(0xFF1F1F2C),
    surfaceContainerHigh = Color(0xFF272738),
    surfaceContainerHighest = Color(0xFF313144),
    outline = Color(0xFF3E3E52),
    outlineVariant = Color(0xFF2A2A3A)
)

// AMOLED (Pure Black) Theme
private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1A1F38),
    onPrimaryContainer = Color(0xFFC5CAE9),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF0E2E2B),
    onSecondaryContainer = Color(0xFFB2DFDB),
    tertiary = Color(0xFFFFCC80),
    onTertiary = Color.Black,
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFEEEEEE),
    surfaceVariant = Color(0xFF141414),
    onSurfaceVariant = Color(0xFFB0B0B0),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF181818),
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color(0xFF2C2C2C),
    outlineVariant = Color(0xFF1A1A1A)
)

// Neon Theme (Vibrant glowing cyberpunk)
private val NeonColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003B45),
    onPrimaryContainer = Color(0xFF80F2FF),
    secondary = Color(0xFFFF0055),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4A0017),
    onSecondaryContainer = Color(0xFFFF80AA),
    tertiary = Color(0xFFE040FB),
    onTertiary = Color.Black,
    background = Color(0xFF0A0915),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF121026),
    onSurface = Color(0xFFF0F0FF),
    surfaceVariant = Color(0xFF1C1938),
    onSurfaceVariant = Color(0xFF00E5FF),
    surfaceContainer = Color(0xFF16142E),
    surfaceContainerHigh = Color(0xFF1E1B3E),
    surfaceContainerHighest = Color(0xFF292552),
    outline = Color(0xFF00E5FF).copy(alpha = 0.4f),
    outlineVariant = Color(0xFFFF0055).copy(alpha = 0.3f)
)

// Retro Android Theme (Classic Android Holo & Material Dark Green/Cyan)
private val RetroColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    onPrimary = Color(0xFF003810),
    primaryContainer = Color(0xFF004D1A),
    onPrimaryContainer = Color(0xFF76FF9B),
    secondary = Color(0xFF33B5E5),
    onSecondary = Color(0xFF00354A),
    secondaryContainer = Color(0xFF004D6A),
    onSecondaryContainer = Color(0xFFBBE9FF),
    tertiary = Color(0xFFFF8800),
    onTertiary = Color(0xFF4A2000),
    tertiaryContainer = Color(0xFF6B3000),
    onTertiaryContainer = Color(0xFFFFDBC9),
    background = Color(0xFF070B08),
    onBackground = Color(0xFFE2EBE2),
    surface = Color(0xFF0E1610),
    onSurface = Color(0xFFE2EBE2),
    surfaceVariant = Color(0xFF16231A),
    onSurfaceVariant = Color(0xFF86A58E),
    surfaceContainer = Color(0xFF121D15),
    surfaceContainerHigh = Color(0xFF1A291F),
    surfaceContainerHighest = Color(0xFF223528),
    surfaceContainerLow = Color(0xFF0B130D),
    surfaceContainerLowest = Color(0xFF050906),
    outline = Color(0xFF2D4734),
    outlineVariant = Color(0xFF1C2E21)
)

// Liquid Glass (iOS 27 Super Glassmorphism & Refractive Frosted Crystals)
private val LiquidGlassColorScheme = darkColorScheme(
    primary = Color(0xFF64D2FF), // Vibrant crystal ice cyan
    onPrimary = Color(0xFF002738),
    primaryContainer = Color(0x66004D73),
    onPrimaryContainer = Color(0xFFD0F4FF),
    secondary = Color(0xFFBF5AF2), // Electric orchid
    onSecondary = Color(0xFF2C004D),
    secondaryContainer = Color(0x664A126D),
    onSecondaryContainer = Color(0xFFF3D8FF),
    tertiary = Color(0xFFFF375F), // Radiant rose
    onTertiary = Color(0xFF4A0014),
    tertiaryContainer = Color(0x666D0E29),
    onTertiaryContainer = Color(0xFFFFD1DC),
    background = Color(0xFF070B18), // Deep oceanic midnight glass canvas
    onBackground = Color(0xFFF5F7FA),
    surface = Color(0x2EFFFFFF), // 18% translucent frosted glass surface
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0x3DFFFFFF), // 24% translucent frosted glass variant
    onSurfaceVariant = Color(0xFFDCE6FA),
    surfaceContainer = Color(0x35192440),
    surfaceContainerHigh = Color(0x4D24355A),
    surfaceContainerHighest = Color(0x662E4473),
    surfaceContainerLow = Color(0x2210192D),
    surfaceContainerLowest = Color(0x18090E1C),
    outline = Color(0x66FFFFFF), // High-specular frosted white border rim
    outlineVariant = Color(0x4464D2FF)
)

val LocalIsLiquidGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun TurnManagerTheme(
    themeName: String = "Dark",
    content: @Composable () -> Unit
) {
    val normalizedTheme = themeName.trim().uppercase().replace(" ", "_")
    val isLiquidGlass = normalizedTheme == "LIQUID_GLASS"

    val colorScheme = when (normalizedTheme) {
        "LIGHT" -> LightColorScheme
        "DARK" -> DarkColorScheme
        "AMOLED" -> AmoledColorScheme
        "NEON" -> NeonColorScheme
        "RETRO" -> RetroColorScheme
        "LIQUID_GLASS" -> LiquidGlassColorScheme
        else -> DarkColorScheme // Fallback to Dark
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalIsLiquidGlass provides isLiquidGlass
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
