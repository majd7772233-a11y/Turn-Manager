package com.example.ui.screens

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val persistentNotificationEnabled by viewModel.persistentNotificationEnabled.collectAsState()
    val smartNotificationsEnabled by viewModel.smartNotificationsEnabled.collectAsState()

    var showCirclesGallery by remember { mutableStateOf(false) }
    var showBackupRestoreScreen by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }

    // Intercept back button if sub-screens are open
    if (showCirclesGallery) {
        BackHandler { showCirclesGallery = false }
        TimerCircleGalleryScreen(
            viewModel = viewModel,
            onBack = { showCirclesGallery = false }
        )
        return
    }

    if (showBackupRestoreScreen) {
        BackHandler { showBackupRestoreScreen = false }
        BackupRestoreScreen(
            viewModel = viewModel,
            onBack = { showBackupRestoreScreen = false }
        )
        return
    }

    if (showAboutScreen) {
        BackHandler { showAboutScreen = false }
        AboutScreen(
            viewModel = viewModel,
            onBack = { showAboutScreen = false }
        )
        return
    }

    val currentStyleInfo = remember(circleStyle) {
        CIRCLE_STYLES_CATALOG.find { it.key.equals(circleStyle, ignoreCase = true) }
    }
    val displayStyleTitle = currentStyleInfo?.title ?: circleStyle
    val displayStyleEmoji = currentStyleInfo?.emoji ?: "✨"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Page Title & Subtitle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                "الإعدادات والتخصيص ⚙️",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "لوحة تحكم منظمة للتحكم في كافة خصائص التطبيق والمظهر والإشعارات",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ==========================================
        // 1. قسم المظهر وثيمات التطبيق (Visuals & Themes)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.Palette,
            title = "قسم المظهر والثيمات",
            subtitle = "تخصيص شكل دائرة المؤقت، ألوان التطبيق، والسمات البصرية"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Circle Timer Gallery Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showCirclesGallery = true },
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
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
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(displayStyleEmoji, fontSize = 22.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "شكل دائرة المؤقت والتفاعل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    displayStyleTitle,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "معرض الأشكال",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(14.dp))

                // Theme Mode Selector
                Text(
                    text = "ثيم وألوان التطبيق:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

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
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.updateTheme(themeKey) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = themeLabel,
                            fontSize = 13.sp,
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
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 2. قسم الأصوات والاهتزاز (Sounds & Haptics)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.VolumeUp,
            title = "قسم الأصوات والاهتزاز",
            subtitle = "التحكم بالنغمات، باقات المؤثرات، واهتزازات التنبيه"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("باقة التنبيهات الصوتية الحالية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("نغمات ومؤثرات نهاية الدور وتنبيهات السرعة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            soundPack,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("اهتزاز التنبيهات (Haptic Feedback):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("تفعيل الاهتزاز عند ضغط الأزرار وانتهاء الوقت", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = { viewModel.updateVibration(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 3. قسم الإشعارات والذكاء التنبيهي (Notifications)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.NotificationsActive,
            title = "قسم الإشعارات والذكاء التنبيهي",
            subtitle = "إدارة شريط الإشعارات العلوي والتنبيهات التفاعلية"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الإشعار التفاعلي بشريط الإشعارات:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("إظهار المؤقت والتحكم السريع خارج التطبيق (يختفي تلقائياً داخل التطبيق)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = persistentNotificationEnabled,
                        onCheckedChange = { viewModel.updatePersistentNotification(it) }
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("نظام الإشعارات الذكية والتحفيزية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("تنبيهات مشجعة وتنافسية تذكرك بالدور وتضمن سير الجلسة بدقة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = smartNotificationsEnabled,
                        onCheckedChange = { viewModel.updateSmartNotifications(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 4. قسم السجل وعرض التوقيت (History & Display)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.History,
            title = "قسم السجل وعرض التوقيت",
            subtitle = "تنسيق مدة الدور، التوقيت الزمني، التواريخ وترتيب السجلات"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.padding(vertical = 12.dp),
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
                            .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.padding(vertical = 12.dp),
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
                            .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.padding(vertical = 12.dp),
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
                            .clip(RoundedCornerShape(8.dp))
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
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                // E. Switches
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إظهار التوقيت الزمني الدقيق:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("عرض وقت البدء والانتهاء (من... إلى...) تحت اسم المستخدم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showExactTimestamps,
                        onCheckedChange = { viewModel.updateHistoryShowExactTimestamps(it) }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("إظهار خط المسار الزمني والنقاط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("عرض مسار النقاط الملونة بجانب بطاقات الأدوار", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = showTimelineDots,
                        onCheckedChange = { viewModel.updateHistoryShowTimelineDots(it) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 5. قسم النسخ الاحتياطي وتصدير البيانات (Backup & Export)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.Save,
            title = "قسم النسخ الاحتياطي وتصدير البيانات",
            subtitle = "حفظ وإعادة البيانات بالكامل أو تصدير التشارير بملفات مختلفة"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsActionRow(
                    title = "مركز النسخ الاحتياطي والاستعادة المتقدم",
                    description = "حفظ السجلات، الإنجازات، والإعدادات بصيغة مشفرة وموقعة أو استرجاعها",
                    icon = Icons.Default.Backup,
                    accentColor = MaterialTheme.colorScheme.primary
                ) {
                    showBackupRestoreScreen = true
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingsActionRow(
                    title = "تصدير جدول البيانات (CSV)",
                    description = "حفظ إحصائيات ونشاط اللاعبين في جدول Excel متكامل",
                    icon = Icons.Default.TableChart,
                    accentColor = Color(0xFF4CAF50)
                ) {
                    val csv = viewModel.exportToCsv()
                    shareTextContent(context, csv, "turns_statistics.csv")
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )

                SettingsActionRow(
                    title = "تصدير تقرير الجلسة (TXT)",
                    description = "مشاركة تقرير نصي شامل ومبسط لأدوار الجلسة الحالية",
                    icon = Icons.Default.Description,
                    accentColor = Color(0xFF2196F3)
                ) {
                    val txt = viewModel.exportToTxt()
                    shareTextContent(context, txt, "session_report.txt")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ==========================================
        // 6. قسم عن التطبيق والمطور (About App & Developer)
        // ==========================================
        CategoryHeader(
            icon = Icons.Default.Info,
            title = "قسم عن التطبيق والمطور",
            subtitle = "معلومات الإصدار، المميزات الحصرية، وبيانات التواصل مع المطور"
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                SettingsActionRow(
                    title = "عن التطبيق والبرمجة (مجد انور)",
                    description = "تطبيق الأدوار لإدارة الوقت - الإصدار 2.5.0 Pro Edition",
                    icon = Icons.Default.Code,
                    accentColor = MaterialTheme.colorScheme.tertiary
                ) {
                    showAboutScreen = true
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun CategoryHeader(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
