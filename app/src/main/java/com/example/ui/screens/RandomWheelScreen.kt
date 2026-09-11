package com.example.ui.screens

import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserEntity
import com.example.engine.TurnEngine
import com.example.ui.theme.liquidGlassContainer
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.AudioEngine
import com.example.utils.SmartNotificationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Wheel operating modes: Selecting a User vs Selecting a Random Duration for the chosen user
 */
enum class WheelMode {
    USERS,
    TIME_DURATIONS
}

data class WheelTimeSlice(
    val seconds: Long,
    val label: String,
    val emoji: String,
    val color: Color
)

val WHEEL_TIME_SLICES = listOf(
    WheelTimeSlice(1L, "1 ثانية", "⚡", Color(0xFFFF5252)),
    WheelTimeSlice(60L, "1 دقيقة", "⏱️", Color(0xFFFF7A00)),
    WheelTimeSlice(120L, "2 دقيقة", "⏳", Color(0xFFFFB300)),
    WheelTimeSlice(300L, "5 دقائق", "🔥", Color(0xFF4CAF50)),
    WheelTimeSlice(600L, "10 دقائق", "🎯", Color(0xFF00BCD4)),
    WheelTimeSlice(900L, "15 دقيقة", "🚀", Color(0xFF2196F3)),
    WheelTimeSlice(1200L, "20 دقيقة", "💎", Color(0xFF3F51B5)),
    WheelTimeSlice(1800L, "30 دقيقة", "⭐", Color(0xFF9C27B0)),
    WheelTimeSlice(2700L, "45 دقيقة", "🏆", Color(0xFFE91E63)),
    WheelTimeSlice(3600L, "60 دقيقة (ساعة)", "👑", Color(0xFF00E676)),
    WheelTimeSlice(4500L, "75 دقيقة (ساعة وربع)", "🌌", Color(0xFF7C4DFF)),
    WheelTimeSlice(5400L, "90 دقيقة (ساعة ونصف)", "🪐", Color(0xFF00E5FF))
)

/**
 * RandomWheelScreen: Fully adaptive for all screens (phone, small phone, tablet),
 * with 3-finger-swipe interactive spinning physics, dual-mode wheel (Users & Time Durations),
 * confetti, and seamless immediate start of selected random turns.
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

    var wheelMode by remember { mutableStateOf(WheelMode.USERS) }
    val rotationAngle = remember { Animatable(0f) }
    var isSpinning by remember { mutableStateOf(false) }

    // Finger Swipe Tracking
    var fingerSwipeCount by remember { mutableIntStateOf(0) }
    val maxFingerSwipes = 3

    // Winner State
    var selectedWinner by remember { mutableStateOf<UserEntity?>(null) }
    var selectedTimeSlice by remember { mutableStateOf<WheelTimeSlice?>(null) }
    var showCelebrationDialog by remember { mutableStateOf(false) }
    var timeWinnerBanner by remember { mutableStateOf<String?>(null) }

    // Particle Confetti State
    var confettiParticles by remember { mutableStateOf<List<WheelConfettiParticle>>(emptyList()) }
    val confettiAnim = remember { Animatable(0f) }

    // Handle back button when in Time Durations mode
    BackHandler {
        if (wheelMode == WheelMode.TIME_DURATIONS) {
            wheelMode = WheelMode.USERS
            fingerSwipeCount = 0
        } else {
            onBackToMain()
        }
    }

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

    fun triggerConfetti() {
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
        scope.launch {
            confettiAnim.snapTo(0f)
            confettiAnim.animateTo(1f, animationSpec = tween(2800, easing = LinearEasing))
        }
    }

    fun launchFullSpin(sliceCount: Int, onFinish: (Int) -> Unit) {
        if (isSpinning || sliceCount <= 0) return

        isSpinning = true
        showCelebrationDialog = false
        timeWinnerBanner = null
        fingerSwipeCount = 0

        scope.launch {
            val sliceAngle = 360f / sliceCount.toFloat()
            val winnerIndex = Random.nextInt(0, sliceCount)

            val randomOffsetWithinSlice = sliceAngle * (0.20f + Random.nextFloat() * 0.60f)
            val targetSliceAngle = (winnerIndex * sliceAngle) + randomOffsetWithinSlice

            val requiredFinalAngleInWheel = (270f - targetSliceAngle + 360f) % 360f
            val fullRotations = (9..14).random() * 360f
            val currentMod = (rotationAngle.value % 360f + 360f) % 360f
            val deltaAngle = ((requiredFinalAngleInWheel - currentMod) % 360f + 360f) % 360f
            val targetRotation = rotationAngle.value + fullRotations + deltaAngle

            // Ticking sound during spin
            val tickJob = launch {
                var delayMs = 45L
                while (isSpinning && delayMs < 360L) {
                    AudioEngine.playSound("TICK")
                    delay(delayMs)
                    delayMs = (delayMs * 1.13f).toLong()
                }
            }

            rotationAngle.animateTo(
                targetValue = targetRotation,
                animationSpec = tween(
                    durationMillis = 4800,
                    easing = CubicBezierEasing(0.15f, 0.85f, 0.20f, 1.0f)
                )
            )
            tickJob.cancel()

            // Calculate final index from angle
            val finalStoppedAngle = ((270f - (rotationAngle.value % 360f)) % 360f + 360f) % 360f
            val calculatedIndex = ((finalStoppedAngle / sliceAngle).toInt()).coerceIn(0, sliceCount - 1)

            isSpinning = false
            onFinish(calculatedIndex)
        }
    }

    // Spin for Users
    fun spinUserWheel() {
        if (users.isEmpty()) return
        val activeUserId = users.firstOrNull()?.id
        if (activeUserId != null) {
            viewModel.onRandomWheelSpun(activeUserId)
        }
        launchFullSpin(users.size) { calculatedWinnerIndex ->
            val winner = users[calculatedWinnerIndex]
            selectedWinner = winner
            showCelebrationDialog = true
            AudioEngine.playSound("TURN_FINISHED")
            triggerConfetti()
        }
    }

    // Spin for Time Durations
    fun spinTimeWheel() {
        val winner = selectedWinner ?: users.firstOrNull() ?: return
        launchFullSpin(WHEEL_TIME_SLICES.size) { calculatedIndex ->
            val timeSlice = WHEEL_TIME_SLICES[calculatedIndex]
            selectedTimeSlice = timeSlice
            AudioEngine.playSound("TURN_FINISHED")
            triggerConfetti()

            timeWinnerBanner = "تم اختيار وقت: ${timeSlice.label} (${timeSlice.emoji})!"

            scope.launch {
                delay(1200)
                // Start turn immediately and navigate to MainHub
                TurnEngine.selectUser(winner.id)
                TurnEngine.setTargetDuration(timeSlice.seconds, isOpenMode = false)
                TurnEngine.startTurn(timeSlice.seconds, isOpenMode = false)
                onStartTurn(winner, timeSlice.seconds, false)
                onBackToMain()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (wheelMode == WheelMode.USERS) "عجلة اختيار اللاعب 🎡" else "عجلة الوقت العشوائي ⏱️",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 19.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (wheelMode == WheelMode.TIME_DURATIONS) {
                            wheelMode = WheelMode.USERS
                            fingerSwipeCount = 0
                        } else {
                            onBackToMain()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    )
                )
        ) {
            val screenWidth = maxWidth
            val screenHeight = maxHeight

            // Adaptive sizing calculations
            val availableHeight = screenHeight - 190.dp
            val minDimension = minOf(screenWidth - 32.dp, availableHeight)
            val wheelDiameter = minDimension.coerceIn(250.dp, 440.dp)
            val isSmallScreen = screenWidth < 360.dp

            if (users.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .padding(28.dp)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(26.dp),
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
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Guidance & Finger Swipe Status
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (wheelMode == WheelMode.TIME_DURATIONS) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "🎯 تحديد وقت الدور لـ: ${selectedWinner?.name ?: "اللاعب"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Interactive Finger Swipes Tracker
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val remaining = (maxFingerSwipes - fingerSwipeCount).coerceAtLeast(0)
                                Text(
                                    text = if (isSpinning) "العجلة تدور الآن... ⚡" else "دوّر بإصبعك 3 مرات: متبقي $remaining سحبات! 👆",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSpinning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (i in 1..maxFingerSwipes) {
                                        val isDone = fingerSwipeCount >= i
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Central Interactive Wheel Canvas (Adaptive size + Pointer gesture input)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(wheelDiameter)
                                .pointerInput(isSpinning, wheelMode, fingerSwipeCount) {
                                    if (isSpinning) return@pointerInput

                                    var lastTouchAngle = 0f

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            lastTouchAngle = Math.toDegrees(
                                                atan2(
                                                    (offset.y - center.y).toDouble(),
                                                    (offset.x - center.x).toDouble()
                                                )
                                            ).toFloat()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val center = Offset(size.width / 2f, size.height / 2f)
                                            val currentTouchAngle = Math.toDegrees(
                                                atan2(
                                                    (change.position.y - center.y).toDouble(),
                                                    (change.position.x - center.x).toDouble()
                                                )
                                            ).toFloat()

                                            var angleDelta = currentTouchAngle - lastTouchAngle
                                            if (angleDelta > 180f) angleDelta -= 360f
                                            if (angleDelta < -180f) angleDelta += 360f

                                            lastTouchAngle = currentTouchAngle

                                            scope.launch {
                                                rotationAngle.snapTo(rotationAngle.value + angleDelta)
                                            }
                                        },
                                        onDragEnd = {
                                            fingerSwipeCount++
                                            AudioEngine.playSound("TICK")

                                            if (fingerSwipeCount >= maxFingerSwipes) {
                                                // Trigger the mega spin after 3 swipes!
                                                if (wheelMode == WheelMode.USERS) {
                                                    spinUserWheel()
                                                } else {
                                                    spinTimeWheel()
                                                }
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            // The Spinning Canvas
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(24.dp, CircleShape, ambientColor = Color(0xFF673AB7), spotColor = Color(0xFFFF9800))
                            ) {
                                val canvasSize = size.minDimension
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = (canvasSize / 2f) - 14.dp.toPx()

                                val sliceCount = if (wheelMode == WheelMode.USERS) users.size else WHEEL_TIME_SLICES.size
                                val sliceAngle = 360f / sliceCount.toFloat()

                                // Outer Rim background
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFF2C223E), Color(0xFF120E20)),
                                        center = center,
                                        radius = radius + 14.dp.toPx()
                                    ),
                                    radius = radius + 13.dp.toPx(),
                                    center = center
                                )

                                // Metallic Border
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
                                    radius = radius + 11.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 5.dp.toPx())
                                )

                                // LED Lights around the rim
                                val ledCount = maxOf(16, sliceCount * 2)
                                for (i in 0 until ledCount) {
                                    val ledAngle = (i.toFloat() * (360f / ledCount.toFloat())) * (Math.PI.toFloat() / 180f)
                                    val ledRadius = radius + 6.dp.toPx()
                                    val lx = center.x + ledRadius * cos(ledAngle.toDouble()).toFloat()
                                    val ly = center.y + ledRadius * sin(ledAngle.toDouble()).toFloat()
                                    val isLit = (i % 2 == 0)
                                    drawCircle(
                                        color = if (isLit) Color(0xFFFFF176) else Color(0xFFFF8A80),
                                        radius = 2.8.dp.toPx(),
                                        center = Offset(lx, ly)
                                    )
                                }

                                // Rotate wheel canvas
                                rotate(degrees = rotationAngle.value, pivot = center) {
                                    for (i in 0 until sliceCount) {
                                        val startAngle = i.toFloat() * sliceAngle

                                        val sliceColor = if (wheelMode == WheelMode.USERS) {
                                            parseColor(users[i].colorHex, i)
                                        } else {
                                            WHEEL_TIME_SLICES[i].color
                                        }

                                        // Draw Arc
                                        drawArc(
                                            color = sliceColor,
                                            startAngle = startAngle,
                                            sweepAngle = sliceAngle,
                                            useCenter = true,
                                            topLeft = Offset(center.x - radius, center.y - radius),
                                            size = Size(radius * 2f, radius * 2f)
                                        )

                                        // Slice Divider Line
                                        val rad = startAngle * (Math.PI.toFloat() / 180f)
                                        val lineEndX = center.x + radius * cos(rad.toDouble()).toFloat()
                                        val lineEndY = center.y + radius * sin(rad.toDouble()).toFloat()
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.55f),
                                            start = center,
                                            end = Offset(lineEndX, lineEndY),
                                            strokeWidth = 2.dp.toPx()
                                        )

                                        // Draw Text / Emoji on slice
                                        val midAngle = startAngle + sliceAngle / 2f
                                        rotate(degrees = midAngle, pivot = center) {
                                            drawIntoCanvas { canvas ->
                                                val nativeCanvas = canvas.nativeCanvas
                                                val dynamicTextSize = if (isSmallScreen) {
                                                    if (sliceCount > 8) 22f else 28f
                                                } else {
                                                    if (sliceCount > 8) 28f else 34f
                                                }

                                                val paint = Paint().apply {
                                                    isAntiAlias = true
                                                    textSize = dynamicTextSize
                                                    color = android.graphics.Color.WHITE
                                                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                                                    textAlign = Paint.Align.RIGHT
                                                    setShadowLayer(4f, 1f, 1f, android.graphics.Color.BLACK)
                                                }

                                                val text = if (wheelMode == WheelMode.USERS) {
                                                    val u = users[i]
                                                    val displayName = if (u.name.length > 6) u.name.take(6) else u.name
                                                    "${u.avatarEmoji} $displayName"
                                                } else {
                                                    val slice = WHEEL_TIME_SLICES[i]
                                                    "${slice.emoji} ${slice.label}"
                                                }

                                                val textX = center.x + radius - 16.dp.toPx()
                                                val textY = center.y + (paint.textSize / 3f)
                                                nativeCanvas.drawText(text, textX, textY, paint)
                                            }
                                        }
                                    }
                                }

                                // Center Hub
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFFB300), Color(0xFFF57F17)),
                                        center = center,
                                        radius = 26.dp.toPx()
                                    ),
                                    radius = 25.dp.toPx(),
                                    center = center
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 25.dp.toPx(),
                                    center = center,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )
                            }

                            // Golden Top Pointer Indicator (Directly attached to the rim)
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

                    // Time Winner Banner Notice
                    AnimatedVisibility(
                        visible = timeWinnerBanner != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = timeWinnerBanner ?: "",
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    // Bottom Action Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Main Action Button
                        Button(
                            onClick = {
                                if (wheelMode == WheelMode.USERS) {
                                    spinUserWheel()
                                } else {
                                    spinTimeWheel()
                                }
                            },
                            enabled = !isSpinning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (wheelMode == WheelMode.USERS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(
                                imageVector = if (wheelMode == WheelMode.USERS) Icons.Default.Casino else Icons.Default.Timer,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isSpinning) "العجلة تدور الآن... ⚡" else if (wheelMode == WheelMode.USERS) "تدوير العجلة بالزر 🎯" else "تدوير عجلة الوقت ⏱️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        // Switch Wheel Mode Button if in Time Mode
                        if (wheelMode == WheelMode.TIME_DURATIONS) {
                            OutlinedButton(
                                onClick = {
                                    wheelMode = WheelMode.USERS
                                    fingerSwipeCount = 0
                                },
                                enabled = !isSpinning,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("العودة لعجلة اللاعبين 👥")
                            }
                        }
                    }
                }
            }

            // Confetti Canvas Overlay
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

            // Winner Celebration Dialog (For User selection)
            if (showCelebrationDialog && selectedWinner != null && wheelMode == WheelMode.USERS) {
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
                                "حان دورك الآن! اختر كيف ترغب في بدء الدور:",
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
                            // KEY REQUEST: Random Time Duration Wheel Button!
                            Button(
                                onClick = {
                                    showCelebrationDialog = false
                                    wheelMode = WheelMode.TIME_DURATIONS
                                    fingerSwipeCount = 0
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF9800)
                                )
                            ) {
                                Icon(Icons.Default.Timer, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("اختيار وقت عشوائي بالعجلة ⏱️🎡", fontWeight = FontWeight.Bold)
                            }

                            // Button 2: Direct Quick Random Turn
                            Button(
                                onClick = {
                                    showCelebrationDialog = false
                                    val randomSecs = SmartNotificationManager.generateRandomDurationSeconds()
                                    TurnEngine.selectUser(winner.id)
                                    TurnEngine.setTargetDuration(randomSecs, isOpenMode = false)
                                    TurnEngine.startTurn(randomSecs, isOpenMode = false)
                                    onStartTurn(winner, randomSecs, false)
                                    onBackToMain()
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
                                Text("بدء دور عشوائي سريع 🎲", fontWeight = FontWeight.Bold)
                            }

                            // Button 3: Spin Again
                            OutlinedButton(
                                onClick = {
                                    showCelebrationDialog = false
                                    spinUserWheel()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تدوير العجلة مجدداً 🔄", fontWeight = FontWeight.SemiBold)
                            }

                            // Button 4: Back to Main
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
