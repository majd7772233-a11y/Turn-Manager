package com.example.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Liquid Glass dynamic refractive background renderer (iOS 27 style)
 * Renders moving ethereal caustic gradients beneath translucent frosted glass elements.
 */
@Composable
fun LiquidGlassBackground(
    content: @Composable () -> Unit
) {
    val isLiquidGlass = LocalIsLiquidGlass.current

    if (!isLiquidGlass) {
        content()
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "LiquidGlassCaustics")

    val phaseX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phaseX"
    )

    val phaseY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phaseY"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Multi-layered refractive caustics canvas
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF040711))
        ) {
            val width = size.width
            val height = size.height

            // Deep sapphire ambient base
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0F1A30), Color(0xFF040711)),
                    center = Offset(width * 0.5f, height * 0.5f),
                    radius = maxOf(width, height) * 0.95f
                )
            )

            // Caustic Orb 1: Cyan Crystal Light
            val cx1 = width * (0.2f + 0.6f * phaseX)
            val cy1 = height * (0.15f + 0.4f * phaseY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x6064D2FF), Color(0x2000557A), Color.Transparent),
                    center = Offset(cx1, cy1),
                    radius = width * 0.8f
                ),
                radius = width * 0.8f,
                center = Offset(cx1, cy1)
            )

            // Caustic Orb 2: Electric Orchid / Magenta Light
            val cx2 = width * (0.8f - 0.5f * phaseY)
            val cy2 = height * (0.7f - 0.4f * phaseX)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x50BF5AF2), Color(0x183A0066), Color.Transparent),
                    center = Offset(cx2, cy2),
                    radius = width * 0.85f
                ),
                radius = width * 0.85f,
                center = Offset(cx2, cy2)
            )

            // Caustic Orb 3: Radiant Rose Accent
            val cx3 = width * (0.5f + 0.3f * (1f - phaseX))
            val cy3 = height * (0.85f - 0.3f * phaseY)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0x40FF375F), Color.Transparent),
                    center = Offset(cx3, cy3),
                    radius = width * 0.65f
                ),
                radius = width * 0.65f,
                center = Offset(cx3, cy3)
            )
        }

        content()
    }
}

/**
 * Modifier extension to apply real translucent frosted glass background, specular reflection, and crystal border.
 * Guarantees true translucent glass that allows background caustics to shine through without grey haze.
 */
fun Modifier.liquidGlassContainer(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.2.dp
): Modifier = composed {
    val isLiquidGlass = LocalIsLiquidGlass.current
    if (isLiquidGlass) {
        val glassGradient = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f), // Delicate specular top reflection
                Color(0x1264D2FF),               // Subtle crystal cyan refraction
                Color.White.copy(alpha = 0.04f), // Pure translucent crystal body
                Color(0x02040711)                // Deep transparency
            )
        )
        val crystalRim = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.65f),      // Specular highlight catch on top-left edge
                Color(0xFF64D2FF).copy(alpha = 0.45f),// Crystal cyan refraction
                Color.White.copy(alpha = 0.18f),      // Middle transparency
                Color(0x35BF5AF2)                     // Subtle orchid dispersion at bottom-right
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 800f)
        )

        this
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color(0x3564D2FF),
                spotColor = Color(0x25BF5AF2)
            )
            .clip(shape)
            .background(brush = glassGradient, shape = shape)
            .border(
                width = borderWidth,
                brush = crystalRim,
                shape = shape
            )
    } else {
        this
    }
}

/**
 * Returns CardColors configured specifically for liquid glass compatibility.
 * In Liquid Glass mode, the container color is set to transparent so that the
 * frosted glass gradient from liquidGlassContainer shines through without any solid or grey overlay.
 */
@Composable
fun liquidGlassCardColors(
    defaultContainerColor: Color = MaterialTheme.colorScheme.surface,
    defaultContentColor: Color = MaterialTheme.colorScheme.onSurface
): CardColors {
    val isLiquidGlass = LocalIsLiquidGlass.current
    return CardDefaults.cardColors(
        containerColor = if (isLiquidGlass) Color.Transparent else defaultContainerColor,
        contentColor = defaultContentColor
    )
}
