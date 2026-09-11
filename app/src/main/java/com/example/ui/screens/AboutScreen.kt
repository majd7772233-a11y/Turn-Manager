package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.liquidGlassContainer
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.AudioEngine
import kotlin.math.cos
import kotlin.math.sin

/**
 * AboutScreen: Legendary, immersive UI with animated neon plasma canvas, glowing aurora orbs,
 * particle stars, neon-rimmed glass cards, and fluid interactive animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.onAboutAppOpened()
    }

    val phoneNumber = "+967 735 465 673"
    val cleanPhone = "+967735465673"
    val emailAddress = "majd7772233@gmail.com"

    fun callDeveloper() {
        AudioEngine.playSound("TICK")
        try {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$cleanPhone")
            }
            context.startActivity(dialIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyEmail() {
        AudioEngine.playSound("TICK")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Developer Email", emailAddress)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "تم نسخ البريد الإلكتروني بنجاح: $emailAddress 📋", Toast.LENGTH_LONG).show()
    }

    fun sendEmail() {
        AudioEngine.playSound("TICK")
        try {
            val mailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$emailAddress")
                putExtra(Intent.EXTRA_SUBJECT, "استفسار حول تطبيق الأدوار لإدارة الوقت")
            }
            context.startActivity(Intent.createChooser(mailIntent, "إرسال بريد إلكتروني"))
        } catch (e: Exception) {
            copyEmail()
        }
    }

    // Master Infinite Transition for Neon Effects
    val infiniteTransition = rememberInfiniteTransition(label = "AboutScreenNeonTransition")

    val orbitAngle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle1"
    )

    val orbitAngle2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitAngle2"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val neonGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neonGlowAlpha"
    )

    val backgroundPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backgroundPhase"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "عن التطبيق ℹ️",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Text(
                                "PRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ==============================================================
            // 🌟 Animated Neon Cosmos / Aurora Waves Canvas Background 🌟
            // ==============================================================
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val width = size.width
                val height = size.height

                // Deep ambient nebula base
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF080C1A),
                            Color(0xFF0F1528),
                            Color(0xFF080C1A)
                        )
                    )
                )

                // Neon Plasma Orb 1: Electric Cyan
                val p1x = width * (0.25f + 0.5f * sin(backgroundPhase * Math.PI.toFloat()))
                val p1y = height * (0.2f + 0.3f * cos(backgroundPhase * Math.PI.toFloat()))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.28f * neonGlowAlpha),
                            Color(0xFF00B0FF).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(p1x, p1y),
                        radius = width * 0.7f
                    ),
                    radius = width * 0.7f,
                    center = Offset(p1x, p1y)
                )

                // Neon Plasma Orb 2: Electric Magenta / Fuchsia
                val p2x = width * (0.75f - 0.5f * cos(backgroundPhase * Math.PI.toFloat()))
                val p2y = height * (0.55f + 0.35f * sin(backgroundPhase * Math.PI.toFloat()))
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFF007F).copy(alpha = 0.24f * neonGlowAlpha),
                            Color(0xFF7C4DFF).copy(alpha = 0.10f),
                            Color.Transparent
                        ),
                        center = Offset(p2x, p2y),
                        radius = width * 0.75f
                    ),
                    radius = width * 0.75f,
                    center = Offset(p2x, p2y)
                )

                // Neon Plasma Orb 3: Radiant Gold
                val p3x = width * 0.5f
                val p3y = height * (0.85f - 0.2f * backgroundPhase)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFB300).copy(alpha = 0.18f * neonGlowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(p3x, p3y),
                        radius = width * 0.6f
                    ),
                    radius = width * 0.6f,
                    center = Offset(p3x, p3y)
                )

                // Twinkling Starlight particles
                val starCount = 35
                for (i in 0 until starCount) {
                    val sx = (i * 97 % 100) / 100f * width
                    val sy = (i * 131 % 100) / 100f * height
                    val starPhase = ((backgroundPhase * 3f + i * 0.2f) % 1f)
                    val starAlpha = (sin(starPhase * Math.PI.toFloat())).coerceIn(0.1f, 0.9f)
                    val starRadius = (i % 3 + 1.5f).dp.toPx()

                    drawCircle(
                        color = if (i % 2 == 0) Color(0xFF80D8FF).copy(alpha = starAlpha) else Color(0xFFFF80AB).copy(alpha = starAlpha),
                        radius = starRadius,
                        center = Offset(sx, sy)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ==============================================================
                // 🚀 Hero Section with Rotating Neon Rings & Glowing Badge
                // ==============================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Neon Ring & Icon Core
                        Box(
                            modifier = Modifier.size(140.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer Orbiting Neon Canvas
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                val r1 = size.width * 0.44f
                                val r2 = size.width * 0.36f

                                // Orbit 1 ring (Cyan/Indigo)
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00E5FF),
                                            Color(0xFF7C4DFF),
                                            Color(0xFF00E5FF).copy(alpha = 0.2f),
                                            Color(0xFF00E5FF)
                                        )
                                    ),
                                    radius = r1,
                                    center = c,
                                    style = Stroke(width = 2.5.dp.toPx())
                                )

                                // Orbit 1 Photon Node
                                val rad1 = (orbitAngle1 * Math.PI.toFloat() / 180f)
                                val node1X = c.x + r1 * cos(rad1.toDouble()).toFloat()
                                val node1Y = c.y + r1 * sin(rad1.toDouble()).toFloat()
                                drawCircle(
                                    color = Color(0xFF00E5FF),
                                    radius = 5.dp.toPx(),
                                    center = Offset(node1X, node1Y)
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(node1X, node1Y)
                                )

                                // Orbit 2 ring (Pink/Gold)
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFF007F),
                                            Color(0xFFFFB300),
                                            Color(0xFFFF007F).copy(alpha = 0.2f),
                                            Color(0xFFFF007F)
                                        )
                                    ),
                                    radius = r2,
                                    center = c,
                                    style = Stroke(width = 1.8.dp.toPx())
                                )

                                // Orbit 2 Photon Node
                                val rad2 = (orbitAngle2 * Math.PI.toFloat() / 180f)
                                val node2X = c.x + r2 * cos(rad2.toDouble()).toFloat()
                                val node2Y = c.y + r2 * sin(rad2.toDouble()).toFloat()
                                drawCircle(
                                    color = Color(0xFFFF007F),
                                    radius = 4.dp.toPx(),
                                    center = Offset(node2X, node2Y)
                                )
                            }

                            // Glowing Central Core
                            Box(
                                modifier = Modifier
                                    .size(86.dp)
                                    .scale(pulseScale)
                                    .shadow(
                                        elevation = 28.dp,
                                        shape = CircleShape,
                                        ambientColor = Color(0xFF00E5FF),
                                        spotColor = Color(0xFFFF007F)
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF2979FF),
                                                Color(0xFF651FFF),
                                                Color(0xFFD500F9)
                                            )
                                        )
                                    )
                                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⏱️",
                                    fontSize = 46.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // App Title with Neon Gradient
                        Text(
                            text = "الأدوار لإدارة الوقت",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Shimmering Neon Pro Badge
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00E5FF),
                                        Color(0xFFBF5AF2),
                                        Color(0xFFFF375F)
                                    )
                                )
                            ),
                            shadowElevation = 8.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الإصدار 2.5.0 • Pro Ultra Edition 💎",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "تطبيق فائق الدقة والذكاء لتنظيم الأدوار وتوزيع الوقت بعدالة مطلقة مع تنبيهات ذكية، عجلة قرعة واقعية، ومعرض دائري أسطوري.",
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ==============================================================
                // 🌟 Key Highlights Showcase (Neon Glassmorphic Card)
                // ==============================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = Color(0xFF00E5FF).copy(alpha = 0.2f))
                        .liquidGlassContainer(RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.82f)
                    ),
                    border = BorderStroke(
                        1.2.dp,
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF00E5FF).copy(alpha = 0.6f),
                                Color(0xFF7C4DFF).copy(alpha = 0.3f),
                                Color(0xFFFF007F).copy(alpha = 0.5f)
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "أبرز مميزات التطبيق الأسطورية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        NeonFeatureItem(
                            icon = Icons.Default.Timer,
                            glowColor = Color(0xFF00E5FF),
                            title = "نظام مؤقتات متعدد الأدوار و20 نمط دائري",
                            desc = "إدارة الأدوار بالوقت التنازلي والمفتوح مع أشكال دائرية ساحرة وتنبيهات صوتية واهتزازات غامرة."
                        )
                        NeonFeatureItem(
                            icon = Icons.Default.Casino,
                            glowColor = Color(0xFFFF5252),
                            title = "عجلة القرعة العشوائية والتدوير بالأصبع",
                            desc = "تدوير واقعي بالأصبع 3 مرات أو بزر التشغيل لاختيار اللاعب والوقت العشوائي بعدالة تامة."
                        )
                        NeonFeatureItem(
                            icon = Icons.Default.EmojiEvents,
                            glowColor = Color(0xFFFFB300),
                            title = "نظام إنجازات وتحديات ضخم (85+ إنجاز)",
                            desc = "تتبع مسيرتك وإنجازاتك اليومية والسلاسل المتتالية بشارات فخمة ومحفزة."
                        )
                        NeonFeatureItem(
                            icon = Icons.Default.NotificationsActive,
                            glowColor = Color(0xFF00E676),
                            title = "إشعارات تفاعلية وذكاء تنبيهي مخصص",
                            desc = "تحكم مباشر بالدور من شريط الإشعارات مع تنبيهات فكاهية ذكية ومزامنة الشاشة."
                        )
                        NeonFeatureItem(
                            icon = Icons.Default.Backup,
                            glowColor = Color(0xFF9C27B0),
                            title = "نسخ احتياطي مشفر وتصدير CSV و TXT",
                            desc = "حفظ كامل للسجلات والإنجازات والإعدادات مع تصدير تقارير Excel متوافقة بالكامل."
                        )
                        NeonFeatureItem(
                            icon = Icons.Default.AutoAwesome,
                            glowColor = Color(0xFF64D2FF),
                            title = "ثيم Liquid Glass (iOS 27 Glassmorphism)",
                            desc = "تجربة بصرية فائقة الزجاجية والانعكاسات الشفافة والمؤثرات الضوئية الخاطفة للأنظار."
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ==============================================================
                // 👨‍💻 Developer Section (مجد انور) with Sweeping Neon Rim
                // ==============================================================
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(20.dp, RoundedCornerShape(26.dp), spotColor = Color(0xFF651FFF))
                        .liquidGlassContainer(RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFF00E5FF),
                                Color(0xFFFF007F),
                                Color(0xFFFFB300),
                                Color(0xFF7C4DFF),
                                Color(0xFF00E5FF)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.08f),
                                        Color(0xFF651FFF).copy(alpha = 0.12f),
                                        Color(0xFFFF007F).copy(alpha = 0.06f)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Developer Avatar Badge with Neon Halo
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF00E5FF).copy(alpha = 0.4f),
                                                Color(0xFF651FFF),
                                                Color(0xFF1A103C)
                                            )
                                        )
                                    )
                                    .border(2.5.dp, Color(0xFF00E5FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "👨‍💻",
                                    fontSize = 40.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "برمجة وتطوير : مجد انور",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "Software & Mobile Applications Developer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(18.dp))

                            // Phone Action Card
                            NeonContactActionCard(
                                icon = Icons.Default.Phone,
                                iconBgColor = Color(0xFF00E676),
                                label = "رقم الهاتف والواتساب الرسمي",
                                value = phoneNumber,
                                actionLabel = "اتصال / نسخ 📞",
                                onClick = { callDeveloper() },
                                onLongClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Developer Phone", cleanPhone)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ رقم الهاتف 📞", Toast.LENGTH_SHORT).show()
                                }
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Email Action Card
                            NeonContactActionCard(
                                icon = Icons.Default.Email,
                                iconBgColor = Color(0xFF00B0FF),
                                label = "البريد الإلكتروني الرسمي",
                                value = emailAddress,
                                actionLabel = "نسخ للحافظة 📋",
                                onClick = { copyEmail() },
                                onLongClick = { sendEmail() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Text(
                    text = "صنع بكل حب وإتقان لخدمتكم 💖\nجميع الحقوق محفوظة © ${java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun NeonFeatureItem(
    icon: ImageVector,
    glowColor: Color,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = glowColor.copy(alpha = 0.16f),
            border = BorderStroke(1.dp, glowColor.copy(alpha = 0.45f)),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NeonContactActionCard(
    icon: ImageVector,
    iconBgColor: Color,
    label: String,
    value: String,
    actionLabel: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.75f),
        border = BorderStroke(1.dp, iconBgColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(iconBgColor.copy(alpha = 0.18f))
                        .border(1.dp, iconBgColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconBgColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                color = iconBgColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, iconBgColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = actionLabel,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = iconBgColor,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
