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
    secondary = Color(0xFF26A69A),
    background = Color(0xFFF5F5F7),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E)
)

// Dark (Midnight Blue/Slate) Theme
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7986CB),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF12121A),
    surface = Color(0xFF1A1A24),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E5EA),
    onSurface = Color(0xFFE5E5EA)
)

// AMOLED (Pure Black) Theme
private val AmoledColorScheme = darkColorScheme(
    primary = Color(0xFF9FA8DA),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFEEEEEE)
)

// Neon Theme (Vibrant glowing cyberpunk)
private val NeonColorScheme = darkColorScheme(
    primary = Color(0xFF00E5FF),
    secondary = Color(0xFFFF0055),
    background = Color(0xFF0A0915),
    surface = Color(0xFF121026),
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFF00E5FF)
)

// Retro Android Theme (Classic Holo Green / Terminal style)
private val RetroColorScheme = darkColorScheme(
    primary = Color(0xFF00FF66),
    secondary = Color(0xFF33B5E5),
    background = Color(0xFF050805),
    surface = Color(0xFF0D140D),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFF00FF66),
    onSurface = Color(0xFF33B5E5)
)

@Composable
fun TurnManagerTheme(
    themeName: String = "Dark",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName.uppercase()) {
        "LIGHT" -> LightColorScheme
        "DARK" -> DarkColorScheme
        "AMOLED" -> AmoledColorScheme
        "NEON" -> NeonColorScheme
        "RETRO" -> RetroColorScheme
        else -> DarkColorScheme // Fallback to Dark
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
