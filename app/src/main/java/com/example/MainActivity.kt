package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.example.engine.TurnEngine
import com.example.service.TurnService
import com.example.ui.screens.*
import com.example.ui.theme.TurnManagerTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UserOnboardingData
import com.example.ui.components.MeowBottomNavigationCompose
import com.example.utils.SmartNotificationManager

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)

        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val circleStyle by viewModel.circleStyle.collectAsState()
            val users by viewModel.users.collectAsState()
            val isLoaded by viewModel.isLoaded.collectAsState()

            TurnManagerTheme(themeName = currentTheme) {
                com.example.ui.theme.LiquidGlassBackground {
                    // Runtime Notification Permission check for Android 13+
                    val context = LocalContext.current
                    var hasNotificationPermission by remember {
                        mutableStateOf(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                            } else {
                                true
                            }
                        )
                    }

                    val permissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { isGranted ->
                        hasNotificationPermission = isGranted
                    }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        // Restore notification visibility and notify service that app is in foreground
                        try {
                            val foregroundIntent = Intent(context, TurnService::class.java).apply {
                                action = TurnService.ACTION_APP_FOREGROUND
                            }
                            context.startService(foregroundIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    if (!isLoaded) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                        }
                    } else {
                        // Decide whether to show Onboarding wizard or the Main Hub
                        if (users.isEmpty()) {
                            OnboardingWizard(
                                onComplete = { userList, sessionName, sessionType ->
                                    viewModel.setupOnboarding(userList, sessionName, sessionType)
                                }
                            )
                        } else {
                            MainHubScreen(viewModel, circleStyle)
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            val foregroundIntent = Intent(this, TurnService::class.java).apply {
                action = TurnService.ACTION_APP_FOREGROUND
            }
            startService(foregroundIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStop() {
        super.onStop()
        try {
            val backgroundIntent = Intent(this, TurnService::class.java).apply {
                action = TurnService.ACTION_APP_BACKGROUND
            }
            startService(backgroundIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val isRandomTurn = intent.getBooleanExtra(SmartNotificationManager.EXTRA_START_RANDOM_TURN, false)
        val targetUserId = intent.getIntExtra(SmartNotificationManager.EXTRA_TARGET_USER_ID, -1)
        val randomDuration = intent.getLongExtra(SmartNotificationManager.EXTRA_RANDOM_DURATION_SEC, 0L)

        if (isRandomTurn && randomDuration > 0L) {
            if (targetUserId != -1) {
                TurnEngine.selectUser(targetUserId)
            }
            TurnEngine.setTargetDuration(randomDuration, isOpenMode = false)
            TurnEngine.startTurn(randomDuration, isOpenMode = false)
            Toast.makeText(this, "تم بدء دور عشوائي مدته ${SmartNotificationManager.formatSecondsToDisplay(randomDuration)} 🎲", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: MainViewModel,
    circleStyle: String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeletedHistory by remember { mutableStateOf(false) }
    var showRandomWheel by remember { mutableStateOf(false) }
    var showAboutScreen by remember { mutableStateOf(false) }
    var showBackupRestoreScreen by remember { mutableStateOf(false) }
    var showMenuDropdown by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabItem("الرئيسية", Icons.Default.PlayArrow),
        TabItem("السجل", Icons.Default.History),
        TabItem("الإحصائيات", Icons.Default.BarChart),
        TabItem("الإنجازات", Icons.Default.EmojiEvents),
        TabItem("التخصيص", Icons.Default.Settings)
    )

    if (showAboutScreen) {
        AboutScreen(
            viewModel = viewModel,
            onBack = { showAboutScreen = false }
        )
    } else if (showBackupRestoreScreen) {
        BackupRestoreScreen(
            viewModel = viewModel,
            onBack = { showBackupRestoreScreen = false }
        )
    } else if (showRandomWheel) {
        RandomWheelScreen(
            viewModel = viewModel,
            onBackToMain = { showRandomWheel = false },
            onStartTurn = { _, _, _ ->
                showRandomWheel = false
                selectedTab = 0
            }
        )
    } else if (showDeletedHistory) {
        DeletedHistoryScreen(
            viewModel = viewModel,
            onBack = { showDeletedHistory = false }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("الأدوار لإدارة الوقت ⏱️", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { showMenuDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "خيارات إضافية"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenuDropdown,
                            onDismissRequest = { showMenuDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("عجلة الاختيار العشوائي") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Casino,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showRandomWheel = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("عن التطبيق والمطور") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showMenuDropdown = false
                                    showAboutScreen = true
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                MeowBottomNavigationCompose(
                    tabs = tabs,
                    selectedIndex = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onTabLongPress = { index ->
                        if (index == 1) { // 5-second long press on "السجل"
                            showDeletedHistory = true
                            viewModel.onSecretArchiveOpened()
                            Toast.makeText(context, "تم فتح الأرشيف السري للأدوار المحذوفة 🗝️", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Keep visited tabs in composition so scroll positions and inputs are preserved 100%
                val visitedTab0 = rememberSaveable { mutableStateOf(true) }
                val visitedTab1 = rememberSaveable { mutableStateOf(false) }
                val visitedTab2 = rememberSaveable { mutableStateOf(false) }
                val visitedTab3 = rememberSaveable { mutableStateOf(false) }
                val visitedTab4 = rememberSaveable { mutableStateOf(false) }

                when (selectedTab) {
                    0 -> visitedTab0.value = true
                    1 -> visitedTab1.value = true
                    2 -> visitedTab2.value = true
                    3 -> visitedTab3.value = true
                    4 -> visitedTab4.value = true
                }

                // Tab 0: Dashboard (الرئيسية)
                if (visitedTab0.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (selectedTab == 0) 1f else 0f
                                translationX = if (selectedTab == 0) 0f else 9999f
                            }
                    ) {
                        DashboardScreen(viewModel, circleStyle)
                    }
                }

                // Tab 1: History (السجل)
                if (visitedTab1.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (selectedTab == 1) 1f else 0f
                                translationX = if (selectedTab == 1) 0f else 9999f
                            }
                    ) {
                        HistoryScreen(viewModel)
                    }
                }

                // Tab 2: Statistics (الإحصائيات)
                if (visitedTab2.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (selectedTab == 2) 1f else 0f
                                translationX = if (selectedTab == 2) 0f else 9999f
                            }
                    ) {
                        StatisticsScreen(viewModel)
                    }
                }

                // Tab 3: Achievements (الإنجازات)
                if (visitedTab3.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (selectedTab == 3) 1f else 0f
                                translationX = if (selectedTab == 3) 0f else 9999f
                            }
                    ) {
                        AchievementsScreen(viewModel)
                    }
                }

                // Tab 4: Settings (الإعدادات)
                if (visitedTab4.value) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = if (selectedTab == 4) 1f else 0f
                                translationX = if (selectedTab == 4) 0f else 9999f
                            }
                    ) {
                        SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

