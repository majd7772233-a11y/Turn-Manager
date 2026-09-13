package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.AchievementEntity
import com.example.database.HistoryEntity
import com.example.database.PreferenceEntity
import com.example.database.SessionEntity
import com.example.database.UserEntity
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.liquidGlassCardColors
import com.example.ui.theme.liquidGlassContainer
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.AudioEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

private const val BACKUP_MAGIC_HEADER = "--- AL-ADWAR SECURE BACKUP PACKAGE v2.5 [MAGIC_KEY_ADW_777_YEMEN] ---"
private const val APP_SIGNATURE = "AL_ADWAR_MAJD_ANWAR_VERIFIED"

data class BackupPreviewData(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val createdAt: String = "",
    val usersCount: Int = 0,
    val sessionsCount: Int = 0,
    val historyCount: Int = 0,
    val achievementsCount: Int = 0,
    val preferencesCount: Int = 0,
    val rawJson: JSONObject? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Backup, 1: Restore
    val timeStamp = remember { SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date()) }

    // Backup State
    var backupFileName by remember { mutableStateOf("AlAdwar_Backup_$timeStamp") }
    var selectedFormat by remember { mutableStateOf("adwb") } // "adwb" or "json"
    var includeUsers by remember { mutableStateOf(true) }
    var includeSessions by remember { mutableStateOf(true) }
    var includeHistory by remember { mutableStateOf(true) }
    var includeAchievements by remember { mutableStateOf(true) }
    var includeSettings by remember { mutableStateOf(true) }

    var isGeneratingBackup by remember { mutableStateOf(false) }
    var backupSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Restore State
    var isVerifyingFile by remember { mutableStateOf(false) }
    var restorePreview by remember { mutableStateOf<BackupPreviewData?>(null) }
    var isRestoring by remember { mutableStateOf(false) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var restoreMergeMode by remember { mutableStateOf(false) } // false: replace, true: merge
    var restoreSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Create Document Launcher (Save backup)
    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            if (selectedFormat == "adwb") "application/octet-stream" else "application/json"
        )
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isGeneratingBackup = true
                try {
                    val exportData = JSONObject()
                    exportData.put("app_signature", APP_SIGNATURE)
                    exportData.put("version", "2.5.0")
                    exportData.put("author", "مجد انور")
                    exportData.put("created_at", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))

                    // Users
                    if (includeUsers) {
                        val users = withContext(Dispatchers.IO) { viewModel.getAllUsersList() }
                        val arr = JSONArray()
                        users.forEach { u ->
                            arr.put(JSONObject().apply {
                                put("id", u.id)
                                put("name", u.name)
                                put("avatarEmoji", u.avatarEmoji)
                                put("colorHex", u.colorHex)
                                put("soundPack", u.soundPack)
                                put("turnsCount", u.turnsCount)
                                put("totalDurationSeconds", u.totalDurationSeconds)
                                put("averageDurationSeconds", u.averageDurationSeconds)
                                put("customSoundUri", u.customSoundUri ?: "")
                            })
                        }
                        exportData.put("users", arr)
                    }

                    // Sessions
                    if (includeSessions) {
                        val sessions = withContext(Dispatchers.IO) { viewModel.getAllSessionsList() }
                        val arr = JSONArray()
                        sessions.forEach { s ->
                            arr.put(JSONObject().apply {
                                put("id", s.id)
                                put("name", s.name)
                                put("type", s.type)
                                put("createdAt", s.createdAt)
                            })
                        }
                        exportData.put("sessions", arr)
                    }

                    // History
                    if (includeHistory) {
                        val history = withContext(Dispatchers.IO) { viewModel.getAllHistoryList() }
                        val arr = JSONArray()
                        history.forEach { h ->
                            arr.put(JSONObject().apply {
                                put("id", h.id)
                                put("sessionId", h.sessionId)
                                put("userId", h.userId)
                                put("userName", h.userName)
                                put("userColorHex", h.userColorHex)
                                put("actionType", h.actionType)
                                put("elapsedSeconds", h.elapsedSeconds)
                                put("timestamp", h.timestamp)
                            })
                        }
                        exportData.put("history", arr)
                    }

                    // Achievements
                    if (includeAchievements) {
                        val achievements = withContext(Dispatchers.IO) { viewModel.getAllAchievementsList() }
                        val arr = JSONArray()
                        achievements.forEach { a ->
                            arr.put(JSONObject().apply {
                                put("userId", a.userId)
                                put("id", a.id)
                                put("title", a.title)
                                put("description", a.description)
                                put("isUnlocked", a.isUnlocked)
                                put("progress", a.progress)
                                put("maxProgress", a.maxProgress)
                            })
                        }
                        exportData.put("achievements", arr)
                    }

                    // Settings / Preferences
                    if (includeSettings) {
                        val prefs = withContext(Dispatchers.IO) { viewModel.getAllPreferencesList() }
                        val arr = JSONArray()
                        prefs.forEach { p ->
                            arr.put(JSONObject().apply {
                                put("key", p.key)
                                put("value", p.value)
                            })
                        }
                        exportData.put("preferences", arr)
                    }

                    // Write to file with Magic Secure Header
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            OutputStreamWriter(os).use { writer ->
                                writer.write(BACKUP_MAGIC_HEADER + "\n")
                                writer.write(exportData.toString(2))
                            }
                        }
                    }

                    viewModel.onBackupCreated()
                    AudioEngine.playSound("TURN_FINISHED")
                    backupSuccessMessage = "تم حفظ النسخة الاحتياطية بنجاح في هاتفك! 💾✨"
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "فشل إنشاء النسخة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                } finally {
                    isGeneratingBackup = false
                }
            }
        }
    }

    // Open Document Launcher (Restore backup)
    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isVerifyingFile = true
                restorePreview = null
                try {
                    val content = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { inputStream ->
                            BufferedReader(InputStreamReader(inputStream)).readText()
                        } ?: ""
                    }

                    // Verify Proof Header
                    val jsonString = if (content.startsWith(BACKUP_MAGIC_HEADER)) {
                        content.removePrefix(BACKUP_MAGIC_HEADER).trim()
                    } else if (content.trim().startsWith("{")) {
                        content.trim()
                    } else {
                        null
                    }

                    if (jsonString == null) {
                        restorePreview = BackupPreviewData(
                            isValid = false,
                            errorMessage = "الملف غير صالح! الملف المحدد لا يحتوي على توقيع أو ترويسة حماية تطبيق الأدوار."
                        )
                    } else {
                        val json = JSONObject(jsonString)
                        val signature = json.optString("app_signature", "")
                        if (signature != APP_SIGNATURE && !json.has("users")) {
                            restorePreview = BackupPreviewData(
                                isValid = false,
                                errorMessage = "هذا الملف ليس نسخة احتياطية متوافقة مع تطبيق الأدوار."
                            )
                        } else {
                            val createdAt = json.optString("created_at", "غير معروف")
                            val usersArr = json.optJSONArray("users")
                            val sessionsArr = json.optJSONArray("sessions")
                            val historyArr = json.optJSONArray("history")
                            val achievementsArr = json.optJSONArray("achievements")
                            val prefsArr = json.optJSONArray("preferences")

                            restorePreview = BackupPreviewData(
                                isValid = true,
                                createdAt = createdAt,
                                usersCount = usersArr?.length() ?: 0,
                                sessionsCount = sessionsArr?.length() ?: 0,
                                historyCount = historyArr?.length() ?: 0,
                                achievementsCount = achievementsArr?.length() ?: 0,
                                preferencesCount = prefsArr?.length() ?: 0,
                                rawJson = json
                            )
                            AudioEngine.playSound("START")
                        }
                    }
                } catch (e: Exception) {
                    restorePreview = BackupPreviewData(
                        isValid = false,
                        errorMessage = "حدث خطأ أثناء قراءة الملف: ${e.localizedMessage}"
                    )
                } finally {
                    isVerifyingFile = false
                }
            }
        }
    }

    // Restore Action implementation
    fun performRestore(data: BackupPreviewData, isMerge: Boolean) {
        val json = data.rawJson ?: return
        scope.launch {
            isRestoring = true
            try {
                withContext(Dispatchers.IO) {
                    viewModel.restoreBackupJson(json, isMerge)
                }
                viewModel.onBackupRestored()
                AudioEngine.playSound("TURN_FINISHED")
                restoreSuccessMessage = if (isMerge) "تم دمج النسخة الاحتياطية مع بياناتك بنجاح! 🔄✨" else "تمت استعادة النسخة الاحتياطية بنجاح تام! 🔄✨"
                restorePreview = null
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "فشلت الاستعادة: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            } finally {
                isRestoring = false
                showRestoreConfirmDialog = false
            }
        }
    }

    val isLiquidGlass = LocalIsLiquidGlass.current

    Scaffold(
        containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "النسخ الاحتياطي والاستعادة 💾",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.background)
        ) {
            // Segmented Tabs Header
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        AudioEngine.playSound("TICK")
                        selectedTab = 0
                    },
                    text = {
                        Text(
                            "إنشاء نسخة 📤",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLiquidGlass) (if (selectedTab == 0) Color(0xFF64D2FF) else Color(0xFFD0E0F5)) else Color.Unspecified
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        AudioEngine.playSound("TICK")
                        selectedTab = 1
                    },
                    text = {
                        Text(
                            "استعادة نسخة 📥",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (isLiquidGlass) (if (selectedTab == 1) Color(0xFF64D2FF) else Color(0xFFD0E0F5)) else Color.Unspecified
                        )
                    }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (selectedTab == 0) {
                    // TAB 1: CREATE BACKUP
                    if (backupSuccessMessage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassContainer(RoundedCornerShape(18.dp))
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = liquidGlassCardColors(defaultContainerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تم بنجاح! 🎉",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiquidGlass) Color(0xFF64D2FF) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = backupSuccessMessage ?: "",
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { backupSuccessMessage = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("إنشاء نسخة أخرى")
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassContainer(RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = liquidGlassCardColors(defaultContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "إعدادات وتخصيص النسخة الاحتياطية",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "حدد اسم الملف والصيغة والبيانات التي تود تضمينها",
                                fontSize = 12.sp,
                                color = if (isLiquidGlass) Color(0xFFD0E0F5) else MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Backup File Name
                            OutlinedTextField(
                                value = backupFileName,
                                onValueChange = { backupFileName = it },
                                label = { Text("اسم ملف النسخة الاحتياطية") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                                }
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Format Selector
                            Text(
                                text = "صيغة التصدير والأمان:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FilterChip(
                                    selected = selectedFormat == "adwb",
                                    onClick = { selectedFormat = "adwb" },
                                    label = { Text(".adwb (حزمة مشفرة ومحمية)") },
                                    leadingIcon = {
                                        if (selectedFormat == "adwb") Icon(Icons.Default.Check, contentDescription = null)
                                        else Icon(Icons.Default.Lock, contentDescription = null)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = selectedFormat == "json",
                                    onClick = { selectedFormat = "json" },
                                    label = { Text(".json (بيانات مفتوحة)") },
                                    leadingIcon = {
                                        if (selectedFormat == "json") Icon(Icons.Default.Check, contentDescription = null)
                                        else Icon(Icons.Default.Code, contentDescription = null)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Content Checkboxes
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "محتويات النسخة الاحتياطية:",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                TextButton(
                                    onClick = {
                                        val allChecked = includeUsers && includeSessions && includeHistory && includeAchievements && includeSettings
                                        includeUsers = !allChecked
                                        includeSessions = !allChecked
                                        includeHistory = !allChecked
                                        includeAchievements = !allChecked
                                        includeSettings = !allChecked
                                    }
                                ) {
                                    Text(
                                        if (includeUsers && includeSessions && includeHistory && includeAchievements && includeSettings) "إلغاء التحديد" else "تحديد الكل",
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            BackupContentCheckbox("👥 قائمة المستخدمين واللاعبين", includeUsers) { includeUsers = it }
                            BackupContentCheckbox("🎯 الجلسات ومجموعات الوقت", includeSessions) { includeSessions = it }
                            BackupContentCheckbox("📜 سجل الأدوار الكامل والنشاطات", includeHistory) { includeHistory = it }
                            BackupContentCheckbox("🏆 الإنجازات والتقدم المحرز", includeAchievements) { includeAchievements = it }
                            BackupContentCheckbox("⚙️ إعدادات التطبيق والمظهر", includeSettings) { includeSettings = it }

                            Spacer(modifier = Modifier.height(20.dp))

                            // Save Button
                            Button(
                                onClick = {
                                    AudioEngine.playSound("TICK")
                                    val ext = if (selectedFormat == "adwb") "adwb" else "json"
                                    val finalName = if (backupFileName.endsWith(".$ext")) backupFileName else "$backupFileName.$ext"
                                    createDocLauncher.launch(finalName)
                                },
                                enabled = !isGeneratingBackup && (includeUsers || includeSessions || includeHistory || includeAchievements || includeSettings),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isGeneratingBackup) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("جارٍ إنشاء النسخة...")
                                } else {
                                    Icon(Icons.Default.SaveAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("تحديد المسار وحفظ النسخة 📁", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: RESTORE BACKUP
                    if (restoreSuccessMessage != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlassContainer(RoundedCornerShape(18.dp))
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = liquidGlassCardColors(defaultContainerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "تمت الاستعادة بنجاح! 🔄",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLiquidGlass) Color(0xFF64D2FF) else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = restoreSuccessMessage ?: "",
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    color = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { restoreSuccessMessage = null },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("إغلاق")
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlassContainer(RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        colors = liquidGlassCardColors(defaultContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Restore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "استعادة نسخة سابقة",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLiquidGlass) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "اختر ملف النسخة الاحتياطية (.adwb أو .json) للتحقق منه واستعادته مباشرة",
                                fontSize = 12.sp,
                                color = if (isLiquidGlass) Color(0xFFD0E0F5) else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = {
                                    AudioEngine.playSound("TICK")
                                    openDocLauncher.launch(arrayOf("*/*"))
                                },
                                enabled = !isVerifyingFile && !isRestoring,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                if (isVerifyingFile) {
                                    CircularProgressIndicator(
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("جارٍ قراءة والتحقق من الملف...")
                                } else {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("اختيار ملف النسخة الاحتياطية 📂", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Restore Preview & Verification Card
                    restorePreview?.let { preview ->
                        if (!preview.isValid) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "ملف غير صالح أو غير متوافق ❌",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Text(
                                            text = preview.errorMessage ?: "تأكد من اختيار ملف تم تصديره من هذا التطبيق.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تم التحقق من النسخة بنجاح ✅",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Text(
                                        text = "تاريخ الإنشاء: ${preview.createdAt}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "محتويات النسخة المكتشفة:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    RestoreStatItem("👥 المستخدمون واللاعبون", "${preview.usersCount} مستخدم")
                                    RestoreStatItem("🎯 الجلسات والتصنيفات", "${preview.sessionsCount} جلسة")
                                    RestoreStatItem("📜 سجل الأدوار والنشاطات", "${preview.historyCount} سجل")
                                    RestoreStatItem("🏆 الإنجازات والتقدم", "${preview.achievementsCount} إنجاز")
                                    RestoreStatItem("⚙️ التفضيلات والإعدادات", "${preview.preferencesCount} إعداد")

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Restore Mode Options
                                    Text(
                                        text = "طريقة الاستعادة:",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        FilterChip(
                                            selected = !restoreMergeMode,
                                            onClick = { restoreMergeMode = false },
                                            label = { Text("استبدال كامل (Clean)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilterChip(
                                            selected = restoreMergeMode,
                                            onClick = { restoreMergeMode = true },
                                            label = { Text("دمج مع الحالي (Merge)") },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    Button(
                                        onClick = { showRestoreConfirmDialog = true },
                                        enabled = !isRestoring,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(52.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isRestoring) {
                                            CircularProgressIndicator(
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("جارٍ الاستعادة...")
                                        } else {
                                            Icon(Icons.Default.RestorePage, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("تطبيق واستعادة هذه النسخة الآن ⚡", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog before performing restore
    if (showRestoreConfirmDialog && restorePreview != null) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirmDialog = false },
            title = {
                Text(
                    text = if (restoreMergeMode) "تأكيد دمج النسخة الاحتياطية" else "تأكيد استبدال البيانات بالكامل ⚠️",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (restoreMergeMode)
                        "سيتم دمج المستخدمين والسجلات والجلسات من النسخة الاحتياطية مع بياناتك الحالية دون حذف أي شيء موجود."
                    else
                        "سيتم استبدال كافة البيانات الحالية في التطبيق ببيانات النسخة الاحتياطية المحددة. هل أنت متأكد من المتابعة؟"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        performRestore(restorePreview!!, restoreMergeMode)
                    }
                ) {
                    Text("نعم، استعد الآن")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRestoreConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun BackupContentCheckbox(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun RestoreStatItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
