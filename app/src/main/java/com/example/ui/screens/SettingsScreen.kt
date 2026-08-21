package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlowingTimerCircle
import com.example.engine.TurnState
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current

    val currentTheme by viewModel.currentTheme.collectAsState()
    val circleStyle by viewModel.circleStyle.collectAsState()
    val soundPack by viewModel.soundPack.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()

    // History Preferences
    val durationFormat by viewModel.historyDurationFormat.collectAsState()
    val timeFormat by viewModel.historyTimeFormat.collectAsState()
    val dateFormat by viewModel.historyDateFormat.collectAsState()
    val sortOrder by viewModel.historySortOrder.collectAsState()
    val showExactTimestamps by viewModel.historyShowExactTimestamps.collectAsState()
    val showTimelineDots by viewModel.historyShowTimelineDots.collectAsState()

    var showCirclesGallery by remember { mutableStateOf(false) }

    // Intercept back button if gallery is open
    if (showCirclesGallery) {
        BackHandler {
            showCirclesGallery = false
        }
        TimerCircleGalleryScreen(
            viewModel = viewModel,
            onBack = { showCirclesGallery = false }
        )
        return
    }

    // Activity launcher for JSON backup restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val success = viewModel.restoreFromJson(context, uri)
            if (success) {
                Toast.makeText(context, "تمت استعادة البيانات بنجاح! 🎉", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "فشلت استعادة البيانات. يرجى التحقق من الملف.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "الإعدادات والتخصيص ⚙️",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
        )

        // 1. Visual Themes Customization Card
        SettingsSectionCard(title = "مظهر وثيمات التطبيق 🎨") {
            val themes = listOf(
                "Dark" to "الوضع الليلي 🌌",
                "Light" to "الوضع المضيء ☀️",
                "AMOLED" to "شاشة سوبر أموليد 🕶️",
                "Neon" to "نيون مستقبلي 🔮",
                "Retro" to "أندرويد كلاسيكي 👾"
            )

            themes.forEach { (themeKey, themeLabel) ->
                val selected = currentTheme == themeKey
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateTheme(themeKey) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = themeLabel,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateTheme(themeKey) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Timer Circles Dedicated Entry Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showCirclesGallery = true },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mini live animated circle icon with clean scaled text
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            GlowingTimerCircle(
                                remainingSeconds = 120L,
                                elapsedSeconds = 60L,
                                totalDurationSeconds = 180L,
                                state = TurnState.RUNNING,
                                circleStyle = circleStyle,
                                modifier = Modifier.size(52.dp),
                                showCenterText = true,
                                showStatusText = false,
                                customTimeText = "02:00",
                                textSizeFactor = 0.3f
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "معرض دوائر المؤقت الأسطورية ✨",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "النمط المعتمد: $circleStyle",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "12 نمطاً سينمائياً • 3D وهولوغرام وسوائل ونيون",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Strip
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
                            "معاينة حية وتفاعلية مع مؤثرات 3D 🔮",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "فتح المعرض",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. History Settings Section (السجل)
        SettingsSectionCard(title = "إعدادات السجل وعرض التوقيت 📜") {
            // A. Duration Display Format
            Text(
                "طريقة عرض مدة الدور في السجل:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val durationFormats = listOf(
                "DIGITAL_MM_SS" to "05:01 (دقيقة وثانية رقمي قياسي)",
                "DIGITAL_M_SS" to "5:01 (دقيقة وثانية بدون أصفار بادئة)",
                "DIGITAL_HH_MM_SS" to "00:05:01 (ساعة:دقيقة:ثانية كامل)",
                "TEXT_ARABIC" to "5 دقائق و 1 ثانية (نص عربي كامل)",
                "TEXT_COMPACT" to "5د 1ث (نص عربي مختصر)",
                "BADGE_PLUS" to "+ 5د:1ث (شارة علامة زائد)",
                "TOTAL_SECONDS" to "301 ثانية (بالثواني الإجمالية)"
            )

            durationFormats.forEach { (key, label) ->
                val selected = durationFormat == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateHistoryDurationFormat(key) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateHistoryDurationFormat(key) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // B. Clock System (12h vs 24h)
            Text(
                "نظام توقيت الساعة:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val timeFormats = listOf(
                "12H" to "نظام 12 ساعة (مثال: 02:30 م)",
                "24H" to "نظام 24 ساعة (مثال: 14:30)"
            )
            timeFormats.forEach { (key, label) ->
                val selected = timeFormat == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateHistoryTimeFormat(key) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateHistoryTimeFormat(key) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // C. History Date Grouping Format
            Text(
                "تنسيق ترويسة التاريخ والأيام:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val dateFormats = listOf(
                "FULL_ARABIC" to "تاريخ عربي كامل (الأربعاء، 20 أغسطس 2026)",
                "RELATIVE" to "تاريخ نسبي ذكي (اليوم 📍 / أمس ⏳ / التاريخ)",
                "SLASH_YMD" to "2026/08/20 (سنة / شهر / يوم)",
                "SLASH_DMY" to "20/08/2026 (يوم / شهر / سنة)"
            )
            dateFormats.forEach { (key, label) ->
                val selected = dateFormat == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateHistoryDateFormat(key) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateHistoryDateFormat(key) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // D. Sort Order
            Text(
                "ترتيب عناصر السجل:",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            val sortOrders = listOf(
                "NEWEST" to "الأحدث أولاً ⬇️ (الترتيب الافتراضي)",
                "OLDEST" to "الأقدم أولاً ⬆️",
                "LONGEST" to "الأدوار الأطول مدة ⏱️",
                "SHORTEST" to "الأدوار الأقصر مدة ⚡"
            )
            sortOrders.forEach { (key, label) ->
                val selected = sortOrder == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.updateHistorySortOrder(key) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { viewModel.updateHistorySortOrder(key) }
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // E. Switches
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("إظهار التوقيت الزمني الدقيق:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("عرض وقت البدء والانتهاء (من... إلى...) تحت اسم المستخدم", fontSize = 10.sp, color = Color.Gray)
                }
                Switch(
                    checked = showExactTimestamps,
                    onCheckedChange = { viewModel.updateHistoryShowExactTimestamps(it) }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("إظهار خط المسار الزمني والنقاط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("عرض مسار النقاط الملونة بجانب بطاقات الأدوار", fontSize = 10.sp, color = Color.Gray)
                }
                Switch(
                    checked = showTimelineDots,
                    onCheckedChange = { viewModel.updateHistoryShowTimelineDots(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Audio & Haptics Toggles Card
        SettingsSectionCard(title = "الأصوات والاهتزاز 🔔") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("باقة التنبيهات الصوتية:", fontWeight = FontWeight.Medium)
                Text(soundPack, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("اهتزاز التنبيهات (Haptic Feedback):", fontWeight = FontWeight.Medium)
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { viewModel.updateVibration(it) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Exports & Secure Backup Card
        SettingsSectionCard(title = "النسخ الاحتياطي وتصدير البيانات 💾") {
            SettingsActionRow(
                title = "تصدير نسخة احتياطية (JSON)",
                description = "مشاركة ملف الإعدادات واللاعبين لاستعادتها لاحقاً",
                icon = Icons.Default.Share
            ) {
                val json = viewModel.exportToJson(context)
                if (json.isNotEmpty()) {
                    shareTextContent(context, json, "backup_turns.json")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsActionRow(
                title = "استعادة نسخة احتياطية (JSON)",
                description = "اختر ملف احتياطي لاستعادة جميع البيانات المفقودة",
                icon = Icons.Default.Backup
            ) {
                filePickerLauncher.launch("application/json")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsActionRow(
                title = "تصدير جدول البيانات (CSV)",
                description = "حفظ إحصائيات المستخدمين في جدول Excel متكامل",
                icon = Icons.Default.TableChart
            ) {
                val csv = viewModel.exportToCsv()
                shareTextContent(context, csv, "turns_statistics.csv")
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsActionRow(
                title = "تصدير تقرير الجلسة (TXT)",
                description = "مشاركة تقرير نصي شامل ومبسط لأدوار الجلسة",
                icon = Icons.Default.Description
            ) {
                val txt = viewModel.exportToTxt()
                shareTextContent(context, txt, "session_report.txt")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun SettingsActionRow(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}

private fun shareTextContent(context: Context, text: String, filename: String) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, filename)
        }
        context.startActivity(Intent.createChooser(intent, "تصدير ومشاركة الملف"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
