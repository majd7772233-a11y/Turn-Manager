package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TurnState
import kotlin.math.*

@Composable
fun GlowingTimerCircle(
    remainingSeconds: Long,
    elapsedSeconds: Long,
    totalDurationSeconds: Long,
    state: TurnState,
    circleStyle: String,
    modifier: Modifier = Modifier,
    showCenterText: Boolean = true,
    showStatusText: Boolean = true,
    customTimeText: String? = null,
    customStatusText: String? = null,
    textSizeFactor: Float = 1f
) {
    val isOpenMode = totalDurationSeconds == 0L
    
    // Progress calculation
    val progress = if (isOpenMode) {
        1f
    } else {
        if (totalDurationSeconds > 0) {
            (remainingSeconds.toFloat() / totalDurationSeconds.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    // Glowing/Pulsing scale animation when under 10 seconds or finished
    val isNearEnd = !isOpenMode && state == TurnState.RUNNING && remainingSeconds <= 10
    
    val infiniteTransition = rememberInfiniteTransition(label = "timer_anims")
    val pulseScale by if (isNearEnd) {
        infiniteTransition.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_scale"
        )
    } else {
        rememberUpdatedState(1f)
    }

    // Continuous rotation angle for effects
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_rotate"
    )

    // Wave phase animation for liquid effects
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    // Shimmer / Glow phase
    val shimmerPhase by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer(
                scaleX = pulseScale,
                scaleY = pulseScale
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (size.width / 2f) - 12.dp.toPx()

            val style = circleStyle.lowercase().trim()
            when {
                // 1. Neon Glow Circle
                style.contains("glow") || style.contains("نيون") || style.contains("neon") -> {
                    drawGlowCircle(center, radius, progress, isOpenMode, rotationAngle, primaryColor, secondaryColor, backgroundColor)
                }

                // 2. Liquid Wave
                style.contains("liquid") || style.contains("wave") || style.contains("أمواج") || style.contains("سوائل") -> {
                    drawLiquidWave(center, radius, progress, wavePhase, primaryColor, secondaryColor, tertiaryColor, backgroundColor)
                }

                // 3. 3D Holographic Sphere
                style.contains("hologram") || style.contains("sphere") || style.contains("هولوغرام") || style.contains("3d") -> {
                    draw3DHologramSphere(center, radius, progress, rotationAngle, primaryColor, secondaryColor, shimmerPhase, backgroundColor)
                }

                // 4. Cosmic Galaxy Vortex
                style.contains("galaxy") || style.contains("cosmic") || style.contains("مجرة") || style.contains("فلك") -> {
                    drawCosmicGalaxy(center, radius, progress, rotationAngle, primaryColor, secondaryColor, backgroundColor)
                }

                // 5. Cyberpunk Matrix HUD
                style.contains("cyberpunk") || style.contains("matrix") || style.contains("hud") || style.contains("سايبر") -> {
                    drawCyberpunkHUD(center, radius, progress, rotationAngle, primaryColor, secondaryColor, backgroundColor)
                }

                // 6. Lava Magma Fire
                style.contains("lava") || style.contains("magma") || style.contains("حمم") || style.contains("بركان") || style.contains("fire") -> {
                    drawLavaMagma(center, radius, progress, wavePhase, rotationAngle, backgroundColor)
                }

                // 7. Crystal Prism
                style.contains("crystal") || style.contains("prism") || style.contains("كريستال") || style.contains("منشور") || style.contains("ماس") -> {
                    drawCrystalPrism(center, radius, progress, rotationAngle, shimmerPhase, primaryColor, secondaryColor, backgroundColor)
                }

                // 8. Aurora Borealis
                style.contains("aurora") || style.contains("borealis") || style.contains("شفق") || style.contains("أورورا") -> {
                    drawAuroraBorealis(center, radius, progress, wavePhase, backgroundColor)
                }

                // 9. Golden Luxury
                style.contains("gold") || style.contains("luxury") || style.contains("ذهب") || style.contains("ملكي") || style.contains("تاج") -> {
                    drawGoldenLuxury(center, radius, progress, rotationAngle, shimmerPhase, backgroundColor)
                }

                // 10. Arcade Retro 8-Bit
                style.contains("arcade") || style.contains("retro") || style.contains("أركيد") || style.contains("ريترو") || style.contains("بكسل") -> {
                    drawArcadeRetro(center, radius, progress, primaryColor, secondaryColor, backgroundColor)
                }

                // 11. Dotted Circle / Radar Matrix
                style.contains("dotted") || style.contains("radar") || style.contains("رادار") || style.contains("مصفوفة") || style.contains("نقط") -> {
                    drawDottedRadar(center, radius, progress, isOpenMode, rotationAngle, primaryColor, secondaryColor, backgroundColor)
                }

                // 13. Quantum Reactor
                style.contains("quantum") || style.contains("reactor") || style.contains("كوانتوم") || style.contains("مفاعل") || style.contains("ذري") -> {
                    drawQuantumReactor(center, radius, progress, rotationAngle, shimmerPhase, backgroundColor)
                }

                // 14. Emerald Dragon
                style.contains("emerald") || style.contains("dragon") || style.contains("زمرد") || style.contains("تنين") || style.contains("أخضر") -> {
                    drawEmeraldDragon(center, radius, progress, rotationAngle, wavePhase, backgroundColor)
                }

                // 15. Hyperdrive Warp
                style.contains("hyperdrive") || style.contains("warp") || style.contains("سرعة") || style.contains("خارقة") || style.contains("انطلاق") -> {
                    drawHyperdriveWarp(center, radius, progress, rotationAngle, shimmerPhase, backgroundColor)
                }

                // 16. Sunset Horizon Synthwave
                style.contains("sunset") || style.contains("horizon") || style.contains("غروب") || style.contains("شمس") || style.contains("ريترو ويف") -> {
                    drawSunsetHorizon(center, radius, progress, wavePhase, backgroundColor)
                }

                // 17. Electric Plasma Storm
                style.contains("plasma") || style.contains("electric") || style.contains("صاعقة") || style.contains("برق") || style.contains("بلازما") -> {
                    drawElectricPlasma(center, radius, progress, rotationAngle, backgroundColor)
                }

                // 18. Cherry Blossom Sakura
                style.contains("sakura") || style.contains("blossom") || style.contains("ساكورا") || style.contains("كرز") || style.contains("زهور") -> {
                    drawCherryBlossom(center, radius, progress, rotationAngle, backgroundColor)
                }

                // 19. Diamond Black Hole
                style.contains("black hole") || style.contains("blackhole") || style.contains("ثقب") || style.contains("أسود") || style.contains("ماسي") -> {
                    drawDiamondBlackHole(center, radius, progress, rotationAngle, shimmerPhase, backgroundColor)
                }

                // 20. Steampunk Clockwork
                style.contains("steampunk") || style.contains("clockwork") || style.contains("ستيم") || style.contains("تروس") || style.contains("ساعة") -> {
                    drawSteampunkClockwork(center, radius, progress, rotationAngle, backgroundColor)
                }

                // 12. Solid Ring / Minimalist
                else -> {
                    drawSolidRing(center, radius, progress, primaryColor, backgroundColor)
                }
            }
        }

        // Center Time Text Overlay (if enabled)
        if (showCenterText) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                val displayTime = customTimeText ?: if (isOpenMode) {
                    formatTime(elapsedSeconds)
                } else {
                    formatTime(remainingSeconds)
                }

                val mainFontSize = (38 * textSizeFactor).coerceAtLeast(10f).sp
                val subFontSize = (12 * textSizeFactor).coerceAtLeast(8f).sp

                Text(
                    text = displayTime,
                    fontSize = mainFontSize,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )

                if (showStatusText && textSizeFactor >= 0.7f) {
                    Spacer(modifier = Modifier.height((2 * textSizeFactor).dp))

                    val statusMsg = customStatusText ?: when (state) {
                        TurnState.IDLE -> "جاهز للبدء ⏱️"
                        TurnState.RUNNING -> "الدور جاري 🚀"
                        TurnState.OPEN_MODE -> "وقت مفتوح ♾️"
                        TurnState.PAUSED -> "مؤقت ⏸️"
                        TurnState.FINISHED -> "انتهى الدور 🎉"
                    }

                    Text(
                        text = statusMsg,
                        fontSize = subFontSize,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// 1. NEON GLOW CIRCLE
private fun DrawScope.drawGlowCircle(
    center: Offset,
    radius: Float,
    progress: Float,
    isOpenMode: Boolean,
    rotationAngle: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    // Background track
    drawCircle(color = background, radius = radius, center = center, style = Stroke(width = 10.dp.toPx()))

    val sweep = if (isOpenMode) 280f else progress * 360f
    val start = -90f + if (isOpenMode) rotationAngle else 0f

    // Outer bloom glow
    drawArc(
        brush = Brush.sweepGradient(listOf(primary.copy(alpha = 0.4f), secondary.copy(alpha = 0.4f), primary.copy(alpha = 0.4f))),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
    )

    // Inner bright neon line
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFFFF007A), Color(0xFFFFD700), Color(0xFF00F0FF))),
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 2. LIQUID WAVE
private fun DrawScope.drawLiquidWave(
    center: Offset,
    radius: Float,
    progress: Float,
    wavePhase: Float,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color
) {
    // Glass container circle
    drawCircle(
        color = primary.copy(alpha = 0.08f),
        radius = radius,
        center = center,
        style = Fill
    )
    drawCircle(
        color = primary.copy(alpha = 0.4f),
        radius = radius,
        center = center,
        style = Stroke(width = 4.dp.toPx())
    )

    // Clip to circle and draw animated sine wave
    val circlePath = Path().apply {
        addOval(androidx.compose.ui.geometry.Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))
    }

    clipPath(circlePath) {
        val waterLevelY = center.y + radius - (progress * radius * 2f)
        val waveAmplitude = 12.dp.toPx() * (1f - abs(progress - 0.5f))

        // Secondary deep water layer
        val wavePath2 = Path().apply {
            moveTo(center.x - radius, center.y + radius)
            lineTo(center.x - radius, waterLevelY)
            var x = center.x - radius
            while (x <= center.x + radius) {
                val y = waterLevelY + waveAmplitude * sin(((x / radius) * 2 * PI + wavePhase + 1.2).toFloat())
                lineTo(x, y)
                x += 8f
            }
            lineTo(center.x + radius, center.y + radius)
            close()
        }
        drawPath(
            path = wavePath2,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0088FF).copy(alpha = 0.4f), Color(0xFF0044AA).copy(alpha = 0.7f)),
                startY = waterLevelY,
                endY = center.y + radius
            )
        )

        // Front primary wave layer
        val wavePath1 = Path().apply {
            moveTo(center.x - radius, center.y + radius)
            lineTo(center.x - radius, waterLevelY)
            var x = center.x - radius
            while (x <= center.x + radius) {
                val y = waterLevelY + waveAmplitude * sin(((x / radius) * 2 * PI + wavePhase).toFloat())
                lineTo(x, y)
                x += 8f
            }
            lineTo(center.x + radius, center.y + radius)
            close()
        }
        drawPath(
            path = wavePath1,
            brush = Brush.verticalGradient(
                listOf(Color(0xFF00E5FF).copy(alpha = 0.6f), Color(0xFF0055FF).copy(alpha = 0.85f)),
                startY = waterLevelY,
                endY = center.y + radius
            )
        )

        // Bubbles in water
        for (i in 0..4) {
            val bX = center.x + (sin(wavePhase + i.toFloat()) * (radius * 0.6f))
            val bY = center.y + radius - ((wavePhase * 40f + (i * 50f)) % (radius * 1.5f))
            if (bY > waterLevelY) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = (3 + (i % 3)).dp.toPx(),
                    center = Offset(bX, bY)
                )
            }
        }
    }

    // Progress rim overlay
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFF00E5FF), Color(0xFF0055FF), Color(0xFF00E5FF))),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 3. 3D HOLOGRAPHIC SPHERE
private fun DrawScope.draw3DHologramSphere(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    primary: Color,
    secondary: Color,
    shimmer: Float,
    background: Color
) {
    // 3D Sphere Shading
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF2C3E50).copy(alpha = 0.3f),
                Color(0xFF0F2027).copy(alpha = 0.85f),
                Color(0xFF000000).copy(alpha = 0.95f)
            ),
            center = center.copy(x = center.x - radius * 0.3f, y = center.y - radius * 0.3f),
            radius = radius * 1.2f
        ),
        radius = radius,
        center = center
    )

    // Dual Orbiting Gyroscopic Rings in 3D
    val gyro1Scale = cos((rotationAngle * PI / 180f)).toFloat()
    drawOval(
        brush = Brush.sweepGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF00F2FE))),
        topLeft = Offset(center.x - radius, center.y - radius * abs(gyro1Scale)),
        size = Size(radius * 2, radius * 2 * abs(gyro1Scale)),
        style = Stroke(width = 3.dp.toPx())
    )

    val gyro2Scale = sin((rotationAngle * PI / 180f)).toFloat()
    drawOval(
        brush = Brush.sweepGradient(listOf(Color(0xFFFF0844), Color(0xFFFFB199), Color(0xFFFF0844))),
        topLeft = Offset(center.x - radius * abs(gyro2Scale), center.y - radius),
        size = Size(radius * 2 * abs(gyro2Scale), radius * 2),
        style = Stroke(width = 3.dp.toPx())
    )

    // Outer Holo Arc
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF00F2FE))),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )

    // Top-left Specular Glass Sheen
    drawCircle(
        color = Color.White.copy(alpha = 0.15f * shimmer),
        radius = radius * 0.45f,
        center = Offset(center.x - radius * 0.35f, center.y - radius * 0.35f)
    )
}

// 4. COSMIC GALAXY VORTEX
private fun DrawScope.drawCosmicGalaxy(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    // Deep Space backdrop
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF4A00E0).copy(alpha = 0.4f), Color(0xFF8E2DE2).copy(alpha = 0.2f), Color(0xFF050515).copy(alpha = 0.9f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Orbiting Star Dust particles
    for (i in 0..24) {
        val angle = (i * 15f + rotationAngle * (1f + (i % 3) * 0.3f)) * (PI / 180f)
        val dist = radius * (0.3f + 0.65f * ((i % 5) / 5f))
        val sX = center.x + dist * cos(angle).toFloat()
        val sY = center.y + dist * sin(angle).toFloat()
        val starAlpha = (0.3f + 0.7f * abs(sin((rotationAngle + i * 20).toDouble()))).toFloat()
        val starColor = if (i % 2 == 0) Color(0xFF00FFFF) else Color(0xFFFF77FF)
        drawCircle(
            color = starColor.copy(alpha = starAlpha),
            radius = (1.5f + (i % 3)).dp.toPx(),
            center = Offset(sX, sY)
        )
    }

    // Outer Nebula Arc
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0), Color(0xFFFF007F), Color(0xFF8E2DE2))),
        startAngle = -90f + rotationAngle,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 5. CYBERPUNK MATRIX HUD
private fun DrawScope.drawCyberpunkHUD(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    val totalTicks = 48
    val activeTicks = (progress * totalTicks).toInt()

    for (i in 0 until totalTicks) {
        val angle = (i * (360f / totalTicks) - 90f) * (PI / 180f)
        val isMajor = i % 4 == 0
        val tickLength = if (isMajor) 14.dp.toPx() else 8.dp.toPx()
        val innerR = radius - tickLength
        val outerR = radius

        val p1 = Offset(center.x + innerR * cos(angle).toFloat(), center.y + innerR * sin(angle).toFloat())
        val p2 = Offset(center.x + outerR * cos(angle).toFloat(), center.y + outerR * sin(angle).toFloat())

        val isLit = i < activeTicks
        val tickColor = if (isLit) {
            if (isMajor) Color(0xFF00FF66) else Color(0xFF00F0FF)
        } else {
            Color(0xFF334455).copy(alpha = 0.4f)
        }

        drawLine(
            color = tickColor,
            start = p1,
            end = p2,
            strokeWidth = if (isMajor) 3.5.dp.toPx() else 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Corner HUD Brackets
    val bracketRadius = radius + 6.dp.toPx()
    val brackets = listOf(45f, 135f, 225f, 315f)
    for (b in brackets) {
        drawArc(
            color = Color(0xFF00FF66).copy(alpha = 0.8f),
            startAngle = b - 15f,
            sweepAngle = 30f,
            useCenter = false,
            topLeft = Offset(center.x - bracketRadius, center.y - bracketRadius),
            size = Size(bracketRadius * 2, bracketRadius * 2),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Square)
        )
    }
}

// 6. LAVA MAGMA FIRE
private fun DrawScope.drawLavaMagma(
    center: Offset,
    radius: Float,
    progress: Float,
    wavePhase: Float,
    rotationAngle: Float,
    background: Color
) {
    // Glowing Ember Background
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFFFF3300).copy(alpha = 0.35f), Color(0xFF660000).copy(alpha = 0.7f), Color(0xFF1A0000).copy(alpha = 0.95f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Fiery Arc
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFFFFCC00), Color(0xFFFF3300), Color(0xFFFF0055), Color(0xFFFFCC00))),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )

    // Floating Sparks
    for (i in 0..12) {
        val sAngle = (i * 30f + wavePhase * 40f) * (PI / 180f)
        val sDist = radius * (0.4f + 0.5f * sin((wavePhase + i).toDouble()).toFloat())
        val pX = center.x + sDist * cos(sAngle).toFloat()
        val pY = center.y + sDist * sin(sAngle).toFloat()
        drawCircle(
            color = Color(0xFFFFDD44).copy(alpha = 0.8f),
            radius = 2.5.dp.toPx(),
            center = Offset(pX, pY)
        )
    }
}

// 7. CRYSTAL PRISM
private fun DrawScope.drawCrystalPrism(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    shimmer: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    // Faceted Octagonal Polygon Outline
    val sides = 8
    val path = Path()
    for (i in 0 until sides) {
        val angle = (i * (360f / sides) - 90f) * (PI / 180f)
        val pX = center.x + radius * cos(angle).toFloat()
        val pY = center.y + radius * sin(angle).toFloat()
        if (i == 0) path.moveTo(pX, pY) else path.lineTo(pX, pY)
    }
    path.close()

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            listOf(Color(0xFFE0F7FA).copy(alpha = 0.25f), Color(0xFF006064).copy(alpha = 0.6f)),
            center = center,
            radius = radius
        )
    )
    drawPath(
        path = path,
        color = Color(0xFF80DEEA).copy(alpha = 0.5f),
        style = Stroke(width = 3.dp.toPx())
    )

    // Rainbow Chromatic Arc
    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFFFF0055), Color(0xFFFFCC00), Color(0xFF00FFCC), Color(0xFF0088FF), Color(0xFFFF00AA))),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 8. AURORA BOREALIS
private fun DrawScope.drawAuroraBorealis(
    center: Offset,
    radius: Float,
    progress: Float,
    wavePhase: Float,
    background: Color
) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color(0xFF00FFA3).copy(alpha = 0.25f), Color(0xFF00B8FF).copy(alpha = 0.35f), Color(0xFF0B0D17).copy(alpha = 0.95f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFF00FFA3), Color(0xFF00D1FF), Color(0xFF9D00FF), Color(0xFF00FFA3))),
        startAngle = -90f + (sin(wavePhase.toDouble()) * 30f).toFloat(),
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 9. GOLDEN LUXURY
private fun DrawScope.drawGoldenLuxury(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    shimmer: Float,
    background: Color
) {
    drawCircle(
        color = Color(0xFF1E1705),
        radius = radius,
        center = center,
        style = Fill
    )
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = 0.3f),
        radius = radius,
        center = center,
        style = Stroke(width = 2.dp.toPx())
    )

    drawArc(
        brush = Brush.sweepGradient(listOf(Color(0xFFFFD700), Color(0xFFFFF8DC), Color(0xFFDAA520), Color(0xFFFFD700))),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
    )

    // Gem studs at 4 cardinal corners
    val gems = listOf(0f, 90f, 180f, 270f)
    for (g in gems) {
        val angle = (g - 90f) * (PI / 180f)
        val gX = center.x + radius * cos(angle).toFloat()
        val gY = center.y + radius * sin(angle).toFloat()
        drawCircle(color = Color(0xFFFFE066), radius = 5.dp.toPx(), center = Offset(gX, gY))
        drawCircle(color = Color(0xFFD4AF37), radius = 2.5.dp.toPx(), center = Offset(gX, gY))
    }
}

// 10. ARCADE RETRO 8-BIT
private fun DrawScope.drawArcadeRetro(
    center: Offset,
    radius: Float,
    progress: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    val blockCount = 32
    val activeBlocks = (progress * blockCount).toInt()

    for (i in 0 until blockCount) {
        val angle = (i * (360f / blockCount) - 90f) * (PI / 180f)
        val pX = center.x + radius * cos(angle).toFloat()
        val pY = center.y + radius * sin(angle).toFloat()

        val isLit = i < activeBlocks
        val bColor = if (isLit) {
            when (i % 4) {
                0 -> Color(0xFFFF0055)
                1 -> Color(0xFF00FF66)
                2 -> Color(0xFF00E5FF)
                else -> Color(0xFFFFD700)
            }
        } else {
            Color(0xFF222233)
        }

        drawRect(
            color = bColor,
            topLeft = Offset(pX - 5.dp.toPx(), pY - 5.dp.toPx()),
            size = Size(10.dp.toPx(), 10.dp.toPx())
        )
    }
}

// 11. DOTTED RADAR MATRIX
private fun DrawScope.drawDottedRadar(
    center: Offset,
    radius: Float,
    progress: Float,
    isOpenMode: Boolean,
    rotationAngle: Float,
    primary: Color,
    secondary: Color,
    background: Color
) {
    val dotCount = 36
    val activeDots = if (isOpenMode) dotCount else (progress * dotCount).toInt()

    for (i in 0 until dotCount) {
        val angle = (i * (360f / dotCount) - 90f + if (isOpenMode) rotationAngle else 0f) * (PI / 180f)
        val x = center.x + radius * cos(angle).toFloat()
        val y = center.y + radius * sin(angle).toFloat()

        val dotColor = if (i < activeDots) primary else background
        val dotRadius = if (i < activeDots) 6.dp.toPx() else 3.5.dp.toPx()

        drawCircle(
            color = dotColor,
            radius = dotRadius,
            center = Offset(x, y)
        )
    }
}

// 12. SOLID RING
private fun DrawScope.drawSolidRing(
    center: Offset,
    radius: Float,
    progress: Float,
    primary: Color,
    background: Color
) {
    drawCircle(
        color = background,
        radius = radius,
        center = center,
        style = Stroke(width = 10.dp.toPx())
    )
    drawArc(
        color = primary,
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Square)
    )
}

// 13. QUANTUM REACTOR
private fun DrawScope.drawQuantumReactor(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    shimmerPhase: Float,
    background: Color
) {
    val cyan = Color(0xFF00E5FF)
    val magenta = Color(0xFFFF0055)
    val coreGlow = Color(0xFF7C4DFF)

    // Outer faint field ring
    drawCircle(
        color = background,
        radius = radius,
        center = center,
        style = Stroke(width = 6.dp.toPx())
    )

    // 3 orbital electron rings
    for (i in 0 until 3) {
        val angleOffset = i * 60f + rotationAngle * (if (i % 2 == 0) 1f else -1f)
        val arcRadius = radius - (i * 8.dp.toPx())
        val arcColor = if (i == 0) cyan else if (i == 1) magenta else coreGlow

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(arcColor, arcColor.copy(alpha = 0.2f), arcColor),
                center = center
            ),
            startAngle = angleOffset,
            sweepAngle = progress * 240f,
            useCenter = false,
            topLeft = Offset(center.x - arcRadius, center.y - arcRadius),
            size = Size(arcRadius * 2, arcRadius * 2),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        // Pulsing quantum particle on the orbit
        val particleAngle = (angleOffset + progress * 240f) * (PI / 180f)
        val px = center.x + arcRadius * cos(particleAngle).toFloat()
        val py = center.y + arcRadius * sin(particleAngle).toFloat()
        drawCircle(
            color = Color.White,
            radius = (3.5f * shimmerPhase).dp.toPx(),
            center = Offset(px, py)
        )
    }

    // Core plasma glow in the center
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(cyan.copy(alpha = 0.25f * shimmerPhase), Color.Transparent),
            center = center,
            radius = radius * 0.7f
        ),
        radius = radius * 0.7f,
        center = center
    )
}

// 14. EMERALD DRAGON
private fun DrawScope.drawEmeraldDragon(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    wavePhase: Float,
    background: Color
) {
    val emeraldDark = Color(0xFF004D40)
    val emeraldBright = Color(0xFF00E676)
    val dragonGold = Color(0xFFFFD700)

    // Dragon scale background track
    drawCircle(
        color = background,
        radius = radius,
        center = center,
        style = Stroke(width = 12.dp.toPx())
    )

    // Serpentine scales around perimeter
    val scaleCount = 28
    val activeScales = (progress * scaleCount).toInt()
    for (i in 0 until scaleCount) {
        val angle = (i * (360f / scaleCount) - 90f + rotationAngle * 0.2f) * (PI / 180f)
        val rOffset = sin(wavePhase + i * 0.5f) * 4.dp.toPx()
        val currR = radius + rOffset
        val sx = center.x + currR * cos(angle).toFloat()
        val sy = center.y + currR * sin(angle).toFloat()

        val isLit = i < activeScales
        val scaleColor = if (isLit) {
            if (i % 3 == 0) dragonGold else emeraldBright
        } else {
            emeraldDark.copy(alpha = 0.3f)
        }

        drawCircle(
            color = scaleColor,
            radius = if (isLit) 6.dp.toPx() else 3.dp.toPx(),
            center = Offset(sx, sy)
        )
    }

    // Main Emerald Jade Arc
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(emeraldBright, dragonGold, emeraldBright),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 15. HYPERDRIVE WARP
private fun DrawScope.drawHyperdriveWarp(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    shimmerPhase: Float,
    background: Color
) {
    val hyperBlue = Color(0xFF00B0FF)
    val warpWhite = Color(0xFFFFFFFF)
    val deepSpace = Color(0xFF0D47A1)

    // Base warp tunnel
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(deepSpace.copy(alpha = 0.2f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )

    // Star streaks shooting radially outwards
    val streakCount = 20
    for (i in 0 until streakCount) {
        val baseAngle = (i * (360f / streakCount) + rotationAngle * 0.5f) * (PI / 180f)
        val innerDist = radius * 0.65f
        val outerDist = radius + (sin(i + shimmerPhase * 5f) * 6.dp.toPx())

        val x1 = center.x + innerDist * cos(baseAngle).toFloat()
        val y1 = center.y + innerDist * sin(baseAngle).toFloat()
        val x2 = center.x + outerDist * cos(baseAngle).toFloat()
        val y2 = center.y + outerDist * sin(baseAngle).toFloat()

        drawLine(
            color = if (i % 2 == 0) hyperBlue.copy(alpha = 0.6f * shimmerPhase) else warpWhite.copy(alpha = 0.8f * shimmerPhase),
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Progress Warp Ring
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(hyperBlue, warpWhite, hyperBlue),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 16. SUNSET HORIZON SYNTHWAVE
private fun DrawScope.drawSunsetHorizon(
    center: Offset,
    radius: Float,
    progress: Float,
    wavePhase: Float,
    background: Color
) {
    val sunYellow = Color(0xFFFFD600)
    val sunOrange = Color(0xFFFF6D00)
    val synthMagenta = Color(0xFFFF007F)
    val synthPurple = Color(0xFF651FFF)

    // Horizontal synthwave sun stripes
    val stripeCount = 10
    for (i in 0 until stripeCount) {
        val yFraction = (i.toFloat() / stripeCount)
        val yPos = center.y - radius + (radius * 2f * yFraction)
        val halfW = sqrt(max(0f, radius * radius - (yPos - center.y) * (yPos - center.y)))

        if (halfW > 0) {
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(synthPurple.copy(alpha = 0.2f), synthMagenta, sunOrange, synthMagenta, synthPurple.copy(alpha = 0.2f)),
                    startX = center.x - halfW,
                    endX = center.x + halfW
                ),
                start = Offset(center.x - halfW, yPos),
                end = Offset(center.x + halfW, yPos),
                strokeWidth = (2.5f + (i * 0.4f)).dp.toPx()
            )
        }
    }

    // Outer Progress Rim
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(sunYellow, sunOrange, synthMagenta, synthPurple, sunYellow),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 17. ELECTRIC PLASMA STORM
private fun DrawScope.drawElectricPlasma(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    background: Color
) {
    val lightningBlue = Color(0xFF00E5FF)
    val deepViolet = Color(0xFF7C4DFF)
    val shockWhite = Color(0xFFFFFFFF)

    // Outer track
    drawCircle(
        color = background,
        radius = radius,
        center = center,
        style = Stroke(width = 8.dp.toPx())
    )

    // Jagged electric arcs
    val segmentCount = 32
    val activeSegments = (progress * segmentCount).toInt()

    for (i in 0 until activeSegments) {
        val a1 = (i * (360f / segmentCount) - 90f) * (PI / 180f)
        val a2 = ((i + 1) * (360f / segmentCount) - 90f) * (PI / 180f)

        // Random jitter for lightning effect
        val jitter = sin(rotationAngle * 2f + i * 7f) * 5.dp.toPx()
        val r1 = radius + (if (i % 2 == 0) jitter else -jitter)
        val r2 = radius + (if (i % 2 == 1) jitter else -jitter)

        val x1 = center.x + r1 * cos(a1).toFloat()
        val y1 = center.y + r1 * sin(a1).toFloat()
        val x2 = center.x + r2 * cos(a2).toFloat()
        val y2 = center.y + r2 * sin(a2).toFloat()

        drawLine(
            color = if (i % 4 == 0) shockWhite else lightningBlue,
            start = Offset(x1, y1),
            end = Offset(x2, y2),
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Inner glowing aura
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(deepViolet.copy(alpha = 0.25f), Color.Transparent),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}

// 18. CHERRY BLOSSOM SAKURA
private fun DrawScope.drawCherryBlossom(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    background: Color
) {
    val sakuraPink = Color(0xFFFF80AB)
    val softRose = Color(0xFFFF4081)
    val petalWhite = Color(0xFFFFF0F5)

    // Soft zen background ring
    drawCircle(
        color = sakuraPink.copy(alpha = 0.15f),
        radius = radius,
        center = center,
        style = Stroke(width = 8.dp.toPx())
    )

    // Floating petals around perimeter
    val petalCount = 16
    for (i in 0 until petalCount) {
        val angle = (i * (360f / petalCount) + rotationAngle * 0.3f) * (PI / 180f)
        val px = center.x + (radius + sin(rotationAngle + i) * 6.dp.toPx()) * cos(angle).toFloat()
        val py = center.y + (radius + sin(rotationAngle + i) * 6.dp.toPx()) * sin(angle).toFloat()

        drawCircle(
            color = if (i % 2 == 0) sakuraPink else petalWhite,
            radius = 4.dp.toPx(),
            center = Offset(px, py)
        )
    }

    // Smooth watercolor progress brush
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(petalWhite, sakuraPink, softRose, sakuraPink),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 19. DIAMOND BLACK HOLE
private fun DrawScope.drawDiamondBlackHole(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    shimmerPhase: Float,
    background: Color
) {
    val pureBlack = Color(0xFF121212)
    val chromeSilver = Color(0xFFCFD8DC)
    val diamondCyan = Color(0xFF80D8FF)

    // Gravitational center void
    drawCircle(
        color = pureBlack,
        radius = radius * 0.85f,
        center = center
    )

    // Gravitational photon lensing ring
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(Color.Transparent, diamondCyan, chromeSilver, Color.White, diamondCyan, Color.Transparent),
            center = center
        ),
        startAngle = rotationAngle,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.9f, center.y - radius * 0.9f),
        size = Size(radius * 1.8f, radius * 1.8f),
        style = Stroke(width = 3.dp.toPx())
    )

    // Outer progress accretion disk
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(chromeSilver, diamondCyan, Color.White, diamondCyan),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
    )
}

// 20. STEAMPUNK CLOCKWORK
private fun DrawScope.drawSteampunkClockwork(
    center: Offset,
    radius: Float,
    progress: Float,
    rotationAngle: Float,
    background: Color
) {
    val brassGold = Color(0xFFD4AF37)
    val copperBronze = Color(0xFFCD7F32)
    val darkIron = Color(0xFF3E2723)

    // Clockwork gear teeth
    val cogCount = 24
    for (i in 0 until cogCount) {
        val angle = (i * (360f / cogCount) + rotationAngle * 0.4f) * (PI / 180f)
        val cx1 = center.x + (radius - 4.dp.toPx()) * cos(angle).toFloat()
        val cy1 = center.y + (radius - 4.dp.toPx()) * sin(angle).toFloat()
        val cx2 = center.x + (radius + 6.dp.toPx()) * cos(angle).toFloat()
        val cy2 = center.y + (radius + 6.dp.toPx()) * sin(angle).toFloat()

        drawLine(
            color = copperBronze,
            start = Offset(cx1, cy1),
            end = Offset(cx2, cy2),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Square
        )
    }

    // Inner brass dial track
    drawCircle(
        color = darkIron.copy(alpha = 0.4f),
        radius = radius - 8.dp.toPx(),
        center = center,
        style = Stroke(width = 4.dp.toPx())
    )

    // Progress Gear Arc
    drawArc(
        brush = Brush.sweepGradient(
            colors = listOf(copperBronze, brassGold, copperBronze),
            center = center
        ),
        startAngle = -90f,
        sweepAngle = progress * 360f,
        useCenter = false,
        topLeft = Offset(center.x - radius, center.y - radius),
        size = Size(radius * 2, radius * 2),
        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
    )
}

private fun formatTime(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}
