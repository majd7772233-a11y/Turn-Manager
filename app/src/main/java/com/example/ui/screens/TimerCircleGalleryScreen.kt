package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.TurnState
import com.example.ui.components.GlowingTimerCircle
import com.example.ui.viewmodel.MainViewModel

data class CircleStyleInfo(
    val key: String,
    val title: String,
    val englishName: String,
    val category: String, // "3D", "LIQUID", "NEON", "CLASSIC"
    val categoryLabel: String,
    val description: String,
    val emoji: String,
    val accentColors: List<Color>
)

val CIRCLE_STYLES_CATALOG = listOf(
    CircleStyleInfo(
        key = "Glow Circle",
        title = "نيون متوهج أسطوري",
        englishName = "Neon Glow Circle",
        category = "NEON",
        categoryLabel = "نيون مشع",
        description = "توهج نيون ليزري ساحر بإشعاع ناعم يدور حول المحيط مع نبضات ضوئية.",
        emoji = "✨",
        accentColors = listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
    ),
    CircleStyleInfo(
        key = "Liquid Wave",
        title = "أمواج سوائل حية",
        englishName = "Fluid Liquid Wave",
        category = "LIQUID",
        categoryLabel = "سوائل وطبيعة",
        description = "حركة تموجات مائية حية ديناميكية ترتفع وتنخفض بسلاسة مع انقضاء الوقت.",
        emoji = "🌊",
        accentColors = listOf(Color(0xFF00B0FF), Color(0xFF00E676))
    ),
    CircleStyleInfo(
        key = "3D Holographic Sphere",
        title = "كرة هولوغرام ثلاثية الأبعاد",
        englishName = "3D Hologram Sphere",
        category = "3D",
        categoryLabel = "ثلاثي الأبعاد 3D",
        description = "تأثير عمق ثلاثي الأبعاد مع طبقات هولوغرافية وظلال متدرجة وتوهج مداري.",
        emoji = "🔮",
        accentColors = listOf(Color(0xFF9C27B0), Color(0xFFE040FB))
    ),
    CircleStyleInfo(
        key = "Cosmic Galaxy",
        title = "دوامة المجرة الكونية",
        englishName = "Cosmic Galaxy Vortex",
        category = "3D",
        categoryLabel = "فلك ومجرات",
        description = "دوامة فلكية كونية ساحرة تدور ببطء وتتلألأ بالنجوم والغبار الكوني الساطع.",
        emoji = "🌌",
        accentColors = listOf(Color(0xFF651FFF), Color(0xFFFF4081))
    ),
    CircleStyleInfo(
        key = "Cyberpunk Matrix",
        title = "سايبر بانك وماتريكس HUD",
        englishName = "Cyberpunk Matrix HUD",
        category = "NEON",
        categoryLabel = "مستقبلي ورقمي",
        description = "واجهة مستقبلية رقمية مستوحاة من الخيال العلمي بشبكة ألياف وأقواس بيانات.",
        emoji = "⚡",
        accentColors = listOf(Color(0xFF00E676), Color(0xFF00E5FF))
    ),
    CircleStyleInfo(
        key = "Lava Magma",
        title = "حمم بركانية مشتعلة",
        englishName = "Lava Magma Core",
        category = "LIQUID",
        categoryLabel = "طاقة ونار",
        description = "توهج بركاني ناري متدفق بألوان اللهب البرتقالية والحمراء المشتعلة بحرارة.",
        emoji = "🔥",
        accentColors = listOf(Color(0xFFFF3D00), Color(0xFFFFD600))
    ),
    CircleStyleInfo(
        key = "Crystal Prism",
        title = "منشور كريستالي مشع",
        englishName = "Crystal Prism Light",
        category = "3D",
        categoryLabel = "كريستال وماس",
        description = "انكسار ضوئي بلوري متعدد الزوايا يبعث لمعاناً ماسياً كريستالياً براقاً.",
        emoji = "💎",
        accentColors = listOf(Color(0xFF00E5FF), Color(0xFFFFFFFF))
    ),
    CircleStyleInfo(
        key = "Aurora Borealis",
        title = "شفق قطبي ساحر",
        englishName = "Aurora Borealis",
        category = "LIQUID",
        categoryLabel = "شفق قطبي",
        description = "أضواء الشفق القطبي الشمالي بألوان خضراء وبنفسجية حالمة وهادئة ومريحة.",
        emoji = "🌈",
        accentColors = listOf(Color(0xFF00E676), Color(0xFF7C4DFF))
    ),
    CircleStyleInfo(
        key = "Golden Luxury",
        title = "ذهب ملكي فاخر",
        englishName = "Golden Luxury Crown",
        category = "CLASSIC",
        categoryLabel = "فخامة وأناقة",
        description = "تصميم ملكي فاخر مطعم ببريق الذهب الخالص والأناقة الرفيعة للمناسبات.",
        emoji = "👑",
        accentColors = listOf(Color(0xFFFFD700), Color(0xFFFFAB00))
    ),
    CircleStyleInfo(
        key = "Arcade Retro",
        title = "ريترو أركيد كلاسيكي",
        englishName = "Arcade Retro Pixel",
        category = "CLASSIC",
        categoryLabel = "ألعاب كلاسيكية",
        description = "أسلوب الألعاب القديمة الثمانينات بتدرجات بكسلية ومؤثرات كلاسيكية مميزة.",
        emoji = "👾",
        accentColors = listOf(Color(0xFFFF0055), Color(0xFF00E5FF))
    ),
    CircleStyleInfo(
        key = "Dotted Circle",
        title = "رادار مصفوفة نقطية",
        englishName = "Dot Matrix Radar",
        category = "NEON",
        categoryLabel = "رادار إلكتروني",
        description = "مصفوفة نقطية إلكترونية تدور كقرص رادار دقيق يعرض الوقت باحترافية وسلاسة.",
        emoji = "🌐",
        accentColors = listOf(Color(0xFF2979FF), Color(0xFF00E5FF))
    ),
    CircleStyleInfo(
        key = "Solid Ring",
        title = "حلقة كلاسيكية حادة",
        englishName = "Solid Precision Ring",
        category = "CLASSIC",
        categoryLabel = "كلاسيكي أنيق",
        description = "تصميم بسيط وعصري وواضح للغاية بألوان متباينة وحواف نظيفة ومريحة للعين.",
        emoji = "⭕",
        accentColors = listOf(Color(0xFF3D5AFE), Color(0xFF651FFF))
    )
)

enum class TestStateMode(val label: String, val remaining: Long, val total: Long, val state: TurnState) {
    RUNNING("تشغيل 🚀", 145L, 300L, TurnState.RUNNING),
    WARNING("إنذار ⚠️", 8L, 300L, TurnState.RUNNING),
    OPEN("مفتوح ♾️", 85L, 0L, TurnState.OPEN_MODE),
    PAUSED("مؤقت ⏸️", 180L, 300L, TurnState.PAUSED)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerCircleGalleryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeCircleStyle by viewModel.circleStyle.collectAsState()
    var selectedPreviewKey by remember(activeCircleStyle) { mutableStateOf(activeCircleStyle) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var testMode by remember { mutableStateOf(TestStateMode.RUNNING) }

    val currentPreviewInfo = remember(selectedPreviewKey) {
        CIRCLE_STYLES_CATALOG.find { it.key.equals(selectedPreviewKey, ignoreCase = true) }
            ?: CIRCLE_STYLES_CATALOG.first()
    }

    val filteredList = remember(selectedCategory) {
        if (selectedCategory == "ALL") {
            CIRCLE_STYLES_CATALOG
        } else {
            CIRCLE_STYLES_CATALOG.filter { it.category == selectedCategory }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "معرض دوائر المؤقت الأسطورية ✨",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "اختر النمط المناسب لمؤقت الأدوار (${CIRCLE_STYLES_CATALOG.size} أنماط متوفرة)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. HERO PREVIEW STAGE (منصة العرض الرئيسية الكبيرة)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        Brush.linearGradient(currentPreviewInfo.accentColors)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header badges in stage
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "منصة المعاينة التفاعلية 🔍",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            val isApplied = activeCircleStyle.equals(selectedPreviewKey, ignoreCase = true)
                            if (isApplied) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF00C853).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF00C853),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "النمط المعتمد حالياً",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00C853)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Large, properly proportioned Glowing Timer Circle (190dp)
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            currentPreviewInfo.accentColors.first().copy(alpha = 0.12f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GlowingTimerCircle(
                                remainingSeconds = testMode.remaining,
                                elapsedSeconds = 75L,
                                totalDurationSeconds = testMode.total,
                                state = testMode.state,
                                circleStyle = selectedPreviewKey,
                                modifier = Modifier.size(185.dp),
                                showCenterText = true,
                                showStatusText = true,
                                textSizeFactor = 0.82f
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Test State Mode Chips (Running, Warning, Open Mode, Paused)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TestStateMode.values().forEach { mode ->
                                val selected = testMode == mode
                                FilterChip(
                                    selected = selected,
                                    onClick = { testMode = mode },
                                    label = { Text(mode.label, fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Title and Description
                        Text(
                            text = "${currentPreviewInfo.emoji} ${currentPreviewInfo.title}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${currentPreviewInfo.englishName} • ${currentPreviewInfo.categoryLabel}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentPreviewInfo.description,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Apply Button
                        val isApplied = activeCircleStyle.equals(selectedPreviewKey, ignoreCase = true)
                        Button(
                            onClick = {
                                viewModel.updateCircleStyle(selectedPreviewKey)
                                Toast.makeText(
                                    context,
                                    "تم تفعيل نمط (${currentPreviewInfo.title}) بنجاح! 🔮",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isApplied) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            if (isApplied) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "هذا هو النمط المعتمد للمؤقت ✅",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تفعيل هذا النمط الآن ✨", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            // 2. CATEGORIES FILTER BAR
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        "تصفح حسب التصنيف 📂",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(
                            "ALL" to "الكل 🔮 (${CIRCLE_STYLES_CATALOG.size})",
                            "3D" to "ثلاثي الأبعاد 3D 🌐",
                            "LIQUID" to "سوائل وطبيعة 🌊",
                            "NEON" to "نيون وماتريكس ⚡",
                            "CLASSIC" to "ملكي وكلاسيكي 👑"
                        )
                        items(categories) { (catKey, catLabel) ->
                            FilterChip(
                                selected = selectedCategory == catKey,
                                onClick = { selectedCategory = catKey },
                                label = { Text(catLabel, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // 3. ELEGANT LIST OF STYLES (Organized, clean, legible preview cards)
            items(filteredList, key = { it.key }) { item ->
                val isSelectedForPreview = selectedPreviewKey.equals(item.key, ignoreCase = true)
                val isActiveInApp = activeCircleStyle.equals(item.key, ignoreCase = true)

                val borderColor by animateColorAsState(
                    targetValue = when {
                        isActiveInApp -> MaterialTheme.colorScheme.primary
                        isSelectedForPreview -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    },
                    animationSpec = tween(300),
                    label = "border_color"
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp)
                        .clickable {
                            selectedPreviewKey = item.key
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelectedForPreview) {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    border = BorderStroke(if (isSelectedForPreview || isActiveInApp) 2.dp else 1.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isSelectedForPreview) 3.dp else 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Information and Actions
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${item.englishName} • ${item.categoryLabel}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Quick Action Buttons
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isActiveInApp) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF00C853).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            "النمط النشط ✅",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF00C853),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            selectedPreviewKey = item.key
                                            viewModel.updateCircleStyle(item.key)
                                            Toast.makeText(
                                                context,
                                                "تم تطبيق (${item.title})! 🔮",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("تطبيق مباشرة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!isSelectedForPreview) {
                                    TextButton(
                                        onClick = { selectedPreviewKey = item.key },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("معاينة بالأعلى ⬆️", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // Right: Crisp, large, clean Circle Thumbnail (105dp) with perfectly readable time
                        Box(
                            modifier = Modifier
                                .size(105.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            item.accentColors.first().copy(alpha = 0.15f),
                                            Color.Transparent
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            GlowingTimerCircle(
                                remainingSeconds = 165L,
                                elapsedSeconds = 75L,
                                totalDurationSeconds = 240L,
                                state = TurnState.RUNNING,
                                circleStyle = item.key,
                                modifier = Modifier.size(98.dp),
                                showCenterText = true,
                                showStatusText = false, // Clean without clutter
                                customTimeText = "02:45",
                                textSizeFactor = 0.45f
                            )
                        }
                    }
                }
            }
        }
    }
}
