package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserEntity
import com.example.engine.TurnEngine
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.AudioEngine
import com.example.utils.SmartNotificationManager
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * RandomWheelScreen: A rich, interactive Canvas-based spinning wheel with realistic physics,
 * luminous lighting, confetti particle burst, and user selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomWheelScreen(
    viewModel: MainViewModel,
    onBackToMain: () -> Unit,
    onStartTurn: (UserEntity, Long, Boolean) -> Unit
) {
    val users by viewModel.users.collectAsState()
    val scope = rememberCoroutineScope()

    val rotationAngle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }
    var selectedWinner by remember { mutableStateOf<UserEntity?>(null) }
    var showCelebrationDialog by remember { mutableStateOf(false) }

    // Particle Confetti State
    var confettiParticles by remember { mutableStateOf<List<WheelConfettiParticle>>(emptyList()) }
    val confettiAnim = remember { Animatable(0f) }

    val defaultPalette = remember {
        listOf(
            Color(0xFFE91E63),
            Color(0xFF9C27B0),
            Color(0xFF673AB7),
            Color(0xFF3F51B5),
            Color(0xFF2196F3),
            Color(0xFF00BCD4),
            Color(0xFF009688),
            Color(0xFF4CAF50),
            Color(0xFFFF9800),
            Color(0xFFFF5722)
        )
    }

    fun parseColor(hex: String, fallbackIndex: Int): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            defaultPalette[fallbackIndex % defaultPalette.size]
        }
    }

    fun spinWheel() {
        if (isSpinning || users.isEmpty()) return

        isSpinning = true
        showCelebrationDialog = false
        selectedWinner = null
        confettiParticles = emptyList()

        val activeUserId = users.firstOrNull()?.id
        if (activeUserId != null) {
            viewModel.onRandomWheelSpun(activeUserId)
        }

        scope.launch {
            val userCount = users.size
            val sliceAngle = 360f / userCount.toFloat()
            // Pick a winning index
            val winnerIndex = Random.nextInt(0, userCount)

            // Random offset within slice (between 15% and 85% of slice width - never stuck dead center)
            val randomOffsetWithinSlice = sliceAngle * (0.15f + Random.nextFloat() * 0.70f)
            val targetSliceAngle = (winnerIndex * sliceAngle) + randomOffsetWithinSlice

            // Pointer is at the top (270 degrees in polar coordinates)
            val requiredFinalAngleInWheel = (270f - targetSliceAngle + 360f) % 360f
            val fullRotations = (8..13).random() * 360f
            val currentMod = (rotationAngle.value % 360f + 360f) % 360f
            val deltaAngle = ((requiredFinalAngleInWheel - currentMod) % 360f + 360f) % 360f
            val targetRotation = rotationAngle.value + fullRotations + deltaAngle

            // Ticking sound during deceleration
            val tickJob = launch {
                var delayMs = 50L
                while (isSpinning && delayMs < 360L) {
                    AudioEngine.playSound("TICK")
                    kotlinx.coroutines.delay(delayMs)
                    delayMs = (delayMs * 1.14f).toLong()
                }
            }

            rotationAngle.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(
                    durationMillis = 4800,
                    easing = CubicBezierEasing(0.15f, 0.85f, 0.20f, 1.0f) // Realistic heavy wheel inertia & friction
                )
            )
            tickJob.cancel()

            // Calculate winner from actual final stopped angle
            val finalStoppedAngle = ((270f - (rotationAngle.value % 360f)) % 360f + 360f) % 360f
            val calculatedWinnerIndex = ((finalStoppedAngle / sliceAngle).toInt()).coerceIn(0, userCount - 1)
            val winner = users[calculatedWinnerIndex]

            selectedWinner = winner
            isSpinning = false
            showCelebrationDialog = true

            // Trigger celebration audio & particles
            AudioEngine.playSound("TURN_FINISHED")

            // Create confetti
            confettiParticles = List(80) {
                WheelConfettiParticle(
                    x = Random.nextFloat(),
                    y = Random.nextFloat() * 0.25f,
                    vx = (Random.nextFloat() - 0.5f) * 700f,
                    vy = (Random.nextFloat() - 0.7f) * 900f,
                    color = defaultPalette.random(),
                    size = Random.nextFloat() * 14f + 6f
                )
            }
            confettiAnim.snapTo(0f)
            confettiAnim.animateTo(1f, animationSpec = tween(2800, easing = LinearEasing))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "عجلة الاختيار العشوائي 🎡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMain) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (users.isEmpty()) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "لا يوجد مستخدمين مسجلين",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "يرجى إضافة لاعبين في الشاشة الرئيسية أولاً للتمكن من تدوير العجلة!",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = onBackToMain) {
                            Text("العودة للرئيسية")
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Subtitle
                    Text(
                        text = if (isSpinning) "العجلة تدور لاختيار صاحب الدور القادم... ⚡" else "اضغط على زر التدوير لاختيار لاعب بشكل عشوائي وممتع! 🎯",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    // Wheel Area with Pointer (Tight wrapper so pointer sits directly on the outer rim)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier.size(320.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // The Spinning Wheel Canvas
                            Canvas(
                                modifier = Modifier
                                    .size(310.dp)
                                    .shadow(24.dp, shape = CircleShape, ambientColor = Color(0xFF9C27B0), spotColor = Color(0xFFFF9800))
                            ) {
                                val canvasSize = size.minDimension
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = (canvasSize / 2f) - 16.dp.toPx()
                                val userCount = users.size
                                val sliceAngle = 360f / userCount.toFloat()

                                // Draw Outer Rim with gradient
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF2C223E), Color(0xFF140F22)),
                                        center = center,
                                        radius = radius + 16.dp.toPx()
                                    ),
                                    radius = radius + 14.dp.toPx(),
                                    center = center
                                )

                                // Outer Metallic Border
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFB300),
                                            Color(0xFFFFF9C4),
                                            Color(0xFFFFB300),
                                            Color(0xFFFFD700)
                                        )
                                    ),
                                    radius = radius + 12.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 6.dp.toPx())
                                )

                                // Decorative LED lights along the outer ring
                                val ledCount = kotlin.math.max(16, userCount * 3)
                                for (i in 0 until ledCount) {
                                    val ledAngle = (i.toFloat() * (360f / ledCount.toFloat())) * (Math.PI.toFloat() / 180f)
                                    val ledRadius = radius + 6.dp.toPx()
                                    val lx = center.x + ledRadius * cos(ledAngle.toDouble()).toFloat()
                                    val ly = center.y + ledRadius * sin(ledAngle.toDouble()).toFloat()
                                    val isLit = (i % 2 == 0)
                                    drawCircle(
                                        color = if (isLit) Color(0xFFFFF176) else Color(0xFFFF8A80),
                                        radius = 3.dp.toPx(),
                                        center = Offset(lx, ly)
                                    )
                                }

                                // Rotate Canvas according to current rotationAngle
                                rotate(degrees = rotationAngle.value, pivot = center) {
                                    for (i in 0 until userCount) {
                                        val startAngle = i.toFloat() * sliceAngle
                                        val user = users[i]
                                        val sliceColor = parseColor(user.colorHex, i)

                                        // Draw Slice Arc
                                        drawArc(
                                            color = sliceColor,
                                            startAngle = startAngle,
                                            sweepAngle = sliceAngle,
                                            useCenter = true,
                                            topLeft = Offset(center.x - radius, center.y - radius),
                                            size = Size(radius * 2f, radius * 2f)
                                        )

                                        // Draw Slice Divider Line
                                        val rad = startAngle * (Math.PI.toFloat() / 180f)
                                        val lineEndX = center.x + radius * cos(rad.toDouble()).toFloat()
                                        val lineEndY = center.y + radius * sin(rad.toDouble()).toFloat()
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.5f),
                                            start = center,
                                            end = Offset(lineEndX, lineEndY),
                                            strokeWidth = 2.dp.toPx()
                                        )

                                        // Draw User Emoji and Name on Slice
                                        val midAngle = startAngle + sliceAngle / 2f
                                        rotate(degrees = midAngle, pivot = center) {
                                            drawIntoCanvas { canvas ->
                                                val nativeCanvas = canvas.nativeCanvas
                                                val paint = Paint().apply {
                                                    isAntiAlias = true
                                                    textSize = if (userCount > 8) 28f else 34f
                                                    color = android.graphics.Color.WHITE
                                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                                    textAlign = Paint.Align.RIGHT
                                                    setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                                                }

                                                val displayName = if (user.name.length > 7) user.name.take(7) else user.name
                                                val text = "${user.avatarEmoji} $displayName"
                                                val textX = center.x + radius - 20.dp.toPx()
                                                val textY = center.y + (paint.textSize / 3f)
                                                nativeCanvas.drawText(text, textX, textY, paint)
                                            }
                                        }
                                    }
                                }

                                // Central Hub Button / Badge
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFB300), Color(0xFFF57F17)),
                                        center = center,
                                        radius = 28.dp.toPx()
                                    ),
                                    radius = 26.dp.toPx(),
                                    center = center
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 26.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }

                            // Top Golden Pointer Arrow (Directly attached to the top rim)
                            Canvas(
                                modifier = Modifier
                                    .size(34.dp)
                                    .align(Alignment.TopCenter)
                                    .offset(y = (-4).dp)
                            ) {
                                val path = Path().apply {
                                    moveTo(size.width / 2f, size.height)
                                    lineTo(size.width * 0.15f, 0f)
                                    lineTo(size.width * 0.85f, 0f)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFFD54F), Color(0xFFFF6F00))
                                    )
                                )
                                drawPath(
                                    path = path,
                                    color = Color.White,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                            }
                        }
                    }

                    // Bottom Action Area
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = { spinWheel() },
                            enabled = !isSpinning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSpinning) "العجلة تدور..." else "تدوير العجلة 🎯",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                        }
                    }
                }
            }

            // Confetti Overlay
            if (confettiParticles.isNotEmpty() && confettiAnim.value > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val progress = confettiAnim.value
                    confettiParticles.forEach { p ->
                        val currentX = size.width * p.x + p.vx * progress * 0.8f
                        val currentY = size.height * p.y + p.vy * progress + (0.5f * 980f * progress * progress)
                        val alpha = (1f - progress).coerceIn(0f, 1f)
                        drawCircle(
                            color = p.color.copy(alpha = alpha),
                            radius = p.size * (1f - progress * 0.3f),
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }

            // Winner Celebration Dialog / Card
            if (showCelebrationDialog && selectedWinner != null) {
                val winner = selectedWinner!!
                AlertDialog(
                    onDismissRequest = { showCelebrationDialog = false },
                    title = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "🎉 مبروك يا ${winner.name}! 👑",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 21.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(parseColor(winner.colorHex, 0).copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(winner.avatarEmoji, fontSize = 40.sp)
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                "حان دورك الآن! اختر كيف تريد البدء:",
                                textAlign = TextAlign.Center,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    confirmButton = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Button 1: Start Random Turn (1s to 2h)
                            Button(
                                onClick = {
                                    showCelebrationDialog = false
                                    // Generate random duration between 1s and 7200s
                                    val randomSecs = SmartNotificationManager.generateRandomDurationSeconds()
                                    TurnEngine.selectUser(winner.id)
                                    TurnEngine.setTargetDuration(randomSecs, isOpenMode = false)
                                    TurnEngine.startTurn(randomSecs, isOpenMode = false)
                                    onStartTurn(winner, randomSecs, false)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("بدء دور عشوائي 🎲", fontWeight = FontWeight.Bold)
                            }

                            // Button 2: Spin Again
                            OutlinedButton(
                                onClick = {
                                    showCelebrationDialog = false
                                    spinWheel()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("التدوير مجدداً 🔄", fontWeight = FontWeight.SemiBold)
                            }

                            // Button 3: Back to Main
                            TextButton(
                                onClick = {
                                    showCelebrationDialog = false
                                    TurnEngine.selectUser(winner.id)
                                    onBackToMain()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("العودة للشاشة الرئيسية 🏠")
                            }
                        }
                    },
                    dismissButton = null,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            }
        }
    }
}

private data class WheelConfettiParticle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val size: Float
)
