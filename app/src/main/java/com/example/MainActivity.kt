package com.example

import android.Manifest
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.core.content.ContextCompat
import com.example.engine.TurnEngine
import com.example.ui.screens.*
import com.example.ui.theme.TurnManagerTheme
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.UserOnboardingData
import com.example.ui.components.MeowBottomNavigationCompose

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val currentTheme by viewModel.currentTheme.collectAsState()
            val circleStyle by viewModel.circleStyle.collectAsState()
            val users by viewModel.users.collectAsState()
            val isLoaded by viewModel.isLoaded.collectAsState()

            TurnManagerTheme(themeName = currentTheme) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
    viewModel: MainViewModel,
    circleStyle: String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDeletedHistory by remember { mutableStateOf(false) }

    val tabs = listOf(
        TabItem("الرئيسية", Icons.Default.PlayArrow),
        TabItem("السجل", Icons.Default.History),
        TabItem("الإحصائيات", Icons.Default.BarChart),
        TabItem("الإنجازات", Icons.Default.EmojiEvents),
        TabItem("التخصيص", Icons.Default.Settings)
    )

    if (showDeletedHistory) {
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
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                when (selectedTab) {
                    0 -> DashboardScreen(viewModel, circleStyle)
                    1 -> HistoryScreen(viewModel)
                    2 -> StatisticsScreen(viewModel)
                    3 -> AchievementsScreen(viewModel)
                    4 -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

data class TabItem(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
