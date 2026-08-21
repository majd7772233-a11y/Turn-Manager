package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.SessionEntity
import com.example.database.UserEntity
import com.example.engine.TurnEngine
import com.example.engine.TurnState
import com.example.ui.components.GlowingTimerCircle
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.verticalScrollbar
import com.example.ui.components.horizontalScrollbar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    circleStyle: String
) {
    val context = LocalContext.current
    val users by viewModel.users.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    val engineState by TurnEngine.state.collectAsState()
    val currentUser by TurnEngine.currentUser.collectAsState()
    val remainingSeconds by TurnEngine.remainingSeconds.collectAsState()
    val elapsedSeconds by TurnEngine.elapsedSeconds.collectAsState()
    val totalDurationSeconds by TurnEngine.totalDurationSeconds.collectAsState()

    var showSessionMenu by remember { mutableStateOf(false) }
    var showCreateSessionDialog by remember { mutableStateOf(false) }
    var newSessionName by remember { mutableStateOf("") }
    var newSessionType by remember { mutableStateOf("PLAY") }

    var showEditSessionDialog by remember { mutableStateOf(false) }
    var selectedSessionToEdit by remember { mutableStateOf<SessionEntity?>(null) }
    var editSessionName by remember { mutableStateOf("") }
    var editSessionType by remember { mutableStateOf("PLAY") }

    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var customHours by remember { mutableStateOf("0") }
    var customMinutes by remember { mutableStateOf("5") }
    var customSeconds by remember { mutableStateOf("0") }

    val targetDurationSeconds by TurnEngine.targetDurationSeconds.collectAsState()
    val isTargetOpenMode by TurnEngine.isTargetOpenMode.collectAsState()
    var showDurationPickerDialog by remember { mutableStateOf(false) }

    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserEmoji by remember { mutableStateOf("😎") }
    var newUserColorHex by remember { mutableStateOf("#FF0055") }

    var showEditUserDialog by remember { mutableStateOf(false) }
    var selectedUserToEdit by remember { mutableStateOf<UserEntity?>(null) }

    val activeSession = sessions.find { it.id == currentSessionId }
    val activeUserColor = remember(currentUser) {
        try {
            Color(android.graphics.Color.parseColor(currentUser?.colorHex ?: "#FF0055"))
        } catch (e: Exception) {
            Color(0xFFFF0055)
        }
    }

    val mainScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(mainScrollState)
            .verticalScrollbar(mainScrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Session Selector Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.combinedClickable(
                        onLongClick = {
                            activeSession?.let { s ->
                                selectedSessionToEdit = s
                                editSessionName = s.name
                                editSessionType = s.type
                                showEditSessionDialog = true
                            }
                        },
                        onClick = { showSessionMenu = true }
                    )
                ) {
                    Text(
                        text = "الجلسة النشطة 🎯",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = activeSession?.name ?: "تحميل الجلسة...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row {
                    IconButton(onClick = { showSessionMenu = true }) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Switch Session")
                    }
                    IconButton(onClick = { showCreateSessionDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = "New Session", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Dropdown Menu for sessions
            DropdownMenu(
                expanded = showSessionMenu,
                onDismissRequest = { showSessionMenu = false }
            ) {
                sessions.forEach { s ->
                    val isRunning = engineState == TurnState.RUNNING || engineState == TurnState.OPEN_MODE || engineState == TurnState.PAUSED
                    DropdownMenuItem(
                        text = {
                            val prefix = when (s.type) {
                                "PLAY" -> "🎮"
                                "STUDY" -> "📚"
                                "WORK" -> "💼"
                                "MEETING" -> "👥"
                                "POMODORO" -> "⏱️"
                                else -> "🎯"
                            }
                            Text("$prefix ${s.name}")
                        },
                        onClick = {
                            if (isRunning) {
                                android.widget.Toast.makeText(context, "هناك دور يعمل حاليا", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.selectSession(s.id)
                            }
                            showSessionMenu = false
                        },
                        modifier = Modifier.combinedClickable(
                            onLongClick = {
                                selectedSessionToEdit = s
                                editSessionName = s.name
                                editSessionType = s.type
                                showEditSessionDialog = true
                                showSessionMenu = false
                            },
                            onClick = {
                                if (isRunning) {
                                    android.widget.Toast.makeText(context, "هناك دور يعمل حاليا", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.selectSession(s.id)
                                }
                                showSessionMenu = false
                            }
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                selectedSessionToEdit = s
                                editSessionName = s.name
                                editSessionType = s.type
                                showEditSessionDialog = true
                                showSessionMenu = false
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Main Timer Display Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Active User Badge
                if (currentUser != null) {
                    val uColor = remember(currentUser) {
                        try {
                            Color(android.graphics.Color.parseColor(currentUser!!.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(uColor.copy(alpha = 0.15f), RoundedCornerShape(30.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(currentUser!!.avatarEmoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentUser!!.name,
                            fontWeight = FontWeight.Bold,
                            color = uColor,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "لا يوجد لاعب نشط",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Glowing Circle Timer Graphic
                Box(
                    modifier = Modifier.size(230.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dispRemaining = if (engineState == TurnState.IDLE) {
                        if (isTargetOpenMode) 0L else targetDurationSeconds
                    } else {
                        remainingSeconds
                    }
                    val dispElapsed = if (engineState == TurnState.IDLE) {
                        0L
                    } else {
                        elapsedSeconds
                    }
                    val dispTotal = if (engineState == TurnState.IDLE) {
                        if (isTargetOpenMode) 0L else targetDurationSeconds
                    } else {
                        totalDurationSeconds
                    }
                    val dispState = if (engineState == TurnState.IDLE && isTargetOpenMode) {
                        TurnState.OPEN_MODE
                    } else {
                        engineState
                    }

                    GlowingTimerCircle(
                        remainingSeconds = dispRemaining,
                        elapsedSeconds = dispElapsed,
                        totalDurationSeconds = dispTotal,
                        state = dispState,
                        circleStyle = circleStyle,
                        modifier = Modifier.size(240.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Dynamic Turn State Label
                Text(
                    text = when (engineState) {
                        TurnState.IDLE -> "بانتظار بدء الدور ⏱️"
                        TurnState.RUNNING -> "الدور جاري الآن... 🚀"
                        TurnState.PAUSED -> "تم إيقاف الدور مؤقتاً ⏸️"
                        TurnState.FINISHED -> "انتهى وقت الدور! 🎉"
                        TurnState.OPEN_MODE -> "وضعية الوقت المفتوح اللانهائي ♾️"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Time Controlling Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (engineState) {
                TurnState.IDLE -> {
                    // Start manually or auto
                    IconButton(
                        onClick = { TurnEngine.startTurn(targetDurationSeconds, isTargetOpenMode) },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Start Turn", tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                    }
                }
                TurnState.RUNNING -> {
                    // Pause Button
                    IconButton(
                        onClick = { TurnEngine.pauseTurn() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }

                    // Done/Finish Button
                    IconButton(
                        onClick = { TurnEngine.finishTurn() },
                        modifier = Modifier
                            .size(62.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Finish Turn", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Discard Button
                    IconButton(
                        onClick = { TurnEngine.cancelTurn() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Discard", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                TurnState.PAUSED -> {
                    // Resume Button
                    IconButton(
                        onClick = { TurnEngine.resumeTurn() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Resume", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }

                    // Cancel Button
                    IconButton(
                        onClick = { TurnEngine.cancelTurn() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                TurnState.OPEN_MODE -> {
                    // Finish button
                    IconButton(
                        onClick = { TurnEngine.finishTurn() },
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Finish Mode", tint = Color.White, modifier = Modifier.size(32.dp))
                    }

                    // Cancel
                    IconButton(
                        onClick = { TurnEngine.cancelTurn() },
                        modifier = Modifier
                            .size(52.dp)
                            .background(MaterialTheme.colorScheme.errorContainer, CircleShape)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Discard", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                TurnState.FINISHED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { TurnEngine.rotateToNextUser() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Next User")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("الدور التالي ⏩")
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = { TurnEngine.cancelTurn() }) {
                            Icon(Icons.Default.Cancel, contentDescription = "Discard", tint = Color.Red)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Upcoming Queue Section (الأدوار التالية) - Placed ABOVE duration selection!
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "قائمة الأدوار التالية 👥",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { showAddUserDialog = true }) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Add User", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (users.isEmpty()) {
            Text("لا يوجد مستخدمين لعرض قائمة الأدوار.", color = Color.Gray, fontSize = 14.sp)
        } else {
            val queueScrollState = rememberLazyListState()
            LazyRow(
                state = queueScrollState,
                modifier = Modifier.fillMaxWidth().horizontalScrollbar(queueScrollState),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(users) { index, user ->
                    val isCurrent = user.id == currentUser?.id
                    val uColor = remember(user) {
                        try {
                            Color(android.graphics.Color.parseColor(user.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    }

                    Card(
                        modifier = Modifier
                            .padding(end = 12.dp, bottom = 8.dp)
                            .width(100.dp)
                            .border(
                                if (isCurrent) 2.dp else 1.dp,
                                if (isCurrent) uColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(12.dp)
                            )
                            .combinedClickable(
                                onClick = {
                                    val isRunning = engineState == TurnState.RUNNING || engineState == TurnState.OPEN_MODE || engineState == TurnState.PAUSED
                                    if (isRunning) {
                                        android.widget.Toast.makeText(context, "هناك دور يعمل حاليا", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        TurnEngine.selectUser(user.id)
                                    }
                                },
                                onLongClick = {
                                    selectedUserToEdit = user
                                    showEditUserDialog = true
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) uColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(uColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.avatarEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                user.name,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isCurrent) {
                                Text(
                                    "الحالي 👑",
                                    fontSize = 9.sp,
                                    color = uColor,
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                Text(
                                    "دور #${index + 1}",
                                    fontSize = 9.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 6. Selected Duration & Selector button (Placed BELOW queue list!)
        if (engineState == TurnState.IDLE) {
            Text(
                "مدة دور اللاعب ⏱️",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { showDurationPickerDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(modifier = Modifier.width(8.dp))
                val label = if (isTargetOpenMode) {
                    "المدة المحددة: وقت مفتوح ♾️"
                } else {
                    val hrs = targetDurationSeconds / 3600
                    val mins = (targetDurationSeconds % 3600) / 60
                    val secs = targetDurationSeconds % 60
                    val durationStr = if (hrs > 0) "${hrs} ساعة و ${mins} دقيقة" else if (mins > 0) "${mins} دقيقة" else "${secs} ثانية"
                    "المدة المحددة: $durationStr ⏱️"
                }
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }

    // --- DIALOGS ---

    // 0. General Duration Picker Dialog
    if (showDurationPickerDialog) {
        val timeOptions = listOf(
            15L to "15 ثانية ⚡",
            30L to "30 ثانية ⚡",
            45L to "45 ثانية ⚡",
            60L to "1 دقيقة ⏱️",
            120L to "2 دقيقة ⏱️",
            180L to "3 دقائق ⏱️",
            300L to "5 دقائق ⏱️",
            600L to "10 دقائق 🌟",
            900L to "15 دقيقة 🌟",
            1200L to "20 دقيقة ✨",
            1500L to "25 دقيقة ✨",
            1800L to "30 دقيقة 🔥",
            2700L to "45 دقيقة 🔥",
            3600L to "1 ساعة 🏆",
            5400L to "1.5 ساعة 🏆",
            7200L to "2 ساعة 👑",
            10800L to "3 ساعات 👑",
            14400L to "4 ساعات 👑",
            18000L to "5 ساعات 👑",
            21600L to "6 ساعات 💎",
            28800L to "8 ساعات 💎",
            43200L to "12 ساعة 💎"
        )
        AlertDialog(
            onDismissRequest = { showDurationPickerDialog = false },
            title = { 
                Text(
                    text = "اختر مدة دور اللاعب ⏱️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                ) 
            },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(scrollState)
                        .verticalScrollbar(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "اختر أحد الأوقات الجاهزة، أو حدد وقتاً مفتوحاً/مخصصاً من الخيارات بالأسفل:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    val chunked = timeOptions.chunked(2)
                    chunked.forEach { pair ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            pair.forEach { (seconds, label) ->
                                Card(
                                    onClick = {
                                        TurnEngine.setTargetDuration(seconds, false)
                                        showDurationPickerDialog = false
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (targetDurationSeconds == seconds && !isTargetOpenMode)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (targetDurationSeconds == seconds && !isTargetOpenMode)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (pair.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Card(
                            onClick = {
                                showDurationPickerDialog = false
                                showCustomTimeDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Text(
                                text = "وقت مخصص ⚙️",
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Card(
                            onClick = {
                                TurnEngine.setTargetDuration(0L, true)
                                showDurationPickerDialog = false
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Text(
                                text = "وقت مفتوح ♾️",
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDurationPickerDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 1. Custom Time Picker Dialog
    if (showCustomTimeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomTimeDialog = false },
            title = { Text("تحديد وقت مخصص") },
            text = {
                Column {
                    Text("أدخل وقت دور اللاعب بالساعات والدقائق والثواني:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedTextField(
                            value = customHours,
                            onValueChange = { customHours = it },
                            label = { Text("ساعات") },
                            modifier = Modifier.weight(1f).padding(4.dp),
                            singleLine = true,
                            placeholder = { Text("0") }
                        )
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            label = { Text("دقائق") },
                            modifier = Modifier.weight(1f).padding(4.dp),
                            singleLine = true,
                            placeholder = { Text("5") }
                        )
                        OutlinedTextField(
                            value = customSeconds,
                            onValueChange = { customSeconds = it },
                            label = { Text("ثواني") },
                            modifier = Modifier.weight(1f).padding(4.dp),
                            singleLine = true,
                            placeholder = { Text("0") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val hrs = customHours.toLongOrNull() ?: 0L
                        val mins = customMinutes.toLongOrNull() ?: 0L
                        val secs = customSeconds.toLongOrNull() ?: 0L
                        val totalSecs = (hrs * 3600) + (mins * 60) + secs
                        val finalSecs = if (totalSecs > 0) totalSecs else 300L
                        TurnEngine.setTargetDuration(finalSecs, false)
                        showCustomTimeDialog = false
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomTimeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 2. Create Session Dialog
    if (showCreateSessionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateSessionDialog = false },
            title = { Text("إنشاء جلسة جديدة") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newSessionName,
                        onValueChange = { newSessionName = it },
                        label = { Text("اسم الجلسة") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("نوع الجلسة:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val types = listOf(
                        "PLAY" to "🎮 لعب وترفيه",
                        "STUDY" to "📚 دراسة ومذاكرة",
                        "WORK" to "💼 عمل وإنجاز",
                        "MEETING" to "👥 اجتماع ونقاش",
                        "POMODORO" to "⏱️ بومودورو"
                    )

                    types.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { newSessionType = key }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = newSessionType == key, onClick = { newSessionType = key })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSessionName.isNotBlank()) {
                            viewModel.addNewSession(newSessionName.trim(), newSessionType)
                            newSessionName = ""
                            showCreateSessionDialog = false
                        }
                    }
                ) {
                    Text("إنشاء")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateSessionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Edit Session Dialog
    if (showEditSessionDialog && selectedSessionToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditSessionDialog = false },
            title = { Text("تعديل الجلسة ✏️") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editSessionName,
                        onValueChange = { editSessionName = it },
                        label = { Text("اسم الجلسة") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("نوع الجلسة:", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val types = listOf(
                        "PLAY" to "🎮 لعب وترفيه",
                        "STUDY" to "📚 دراسة ومذاكرة",
                        "WORK" to "💼 عمل وإنجاز",
                        "MEETING" to "👥 اجتماع ونقاش",
                        "POMODORO" to "⏱️ بومودورو"
                    )

                    types.forEach { (key, label) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editSessionType = key }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(selected = editSessionType == key, onClick = { editSessionType = key })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(label)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (sessions.size > 1) {
                        Button(
                            onClick = {
                                viewModel.deleteSession(selectedSessionToEdit!!.id)
                                showEditSessionDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حذف الجلسة كاملة 🗑️")
                        }
                    } else {
                        Text(
                            "لا يمكن حذف هذه الجلسة لأنها الجلسة الوحيدة المتبقية.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val current = selectedSessionToEdit
                        if (current != null && editSessionName.isNotBlank()) {
                            viewModel.updateSession(current.copy(name = editSessionName.trim(), type = editSessionType))
                            showEditSessionDialog = false
                        }
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditSessionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 3. Add User Dialog
    if (showAddUserDialog) {
        val emojis = listOf("😎", "🤖", "🚀", "😺", "🦊", "🐼", "🦁", "🦖", "👾", "🥑", "👨‍💻", "⚽", "🧙", "🧛", "🦄", "🎯", "🎸", "🌟", "🍿", "🍕", "🍔", "🧁", "🍩", "🚗", "🏆", "🎨")
        val colors = listOf("#FF0055", "#00E5FF", "#39FF14", "#FFEA00", "#7D26CD", "#FF9100", "#00FF66", "#E100FF", "#00E676", "#FF5252", "#FFD700", "#FF4081")
        var newUserSoundUri by remember { mutableStateOf<String?>(null) }

        val audioLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                newUserSoundUri = uri.toString()
            }
        }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("إضافة مشارك جديد") },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState).verticalScrollbar(scrollState)) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("اسم المشارك") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("اختر الرمز التعبيري (Avatar):")
                    Spacer(modifier = Modifier.height(8.dp))
                    val emojiRowState = rememberLazyListState()
                    LazyRow(
                        state = emojiRowState,
                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(emojiRowState)
                    ) {
                        items(emojis.size) { idx ->
                            val e = emojis[idx]
                            val selected = newUserEmoji == e
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable { newUserEmoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("اختر لون التخصيص:")
                    Spacer(modifier = Modifier.height(8.dp))
                    val colorRowState = rememberLazyListState()
                    LazyRow(
                        state = colorRowState,
                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(colorRowState)
                    ) {
                        items(colors.size) { idx ->
                            val c = colors[idx]
                            val selected = newUserColorHex == c
                            val col = Color(android.graphics.Color.parseColor(c))
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        if (selected) 3.dp else 0.dp,
                                        MaterialTheme.colorScheme.onBackground,
                                        CircleShape
                                    )
                                    .clickable { newUserColorHex = c }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("نغمة انتهاء الدور المخصصة:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { audioLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (newUserSoundUri != null) MaterialTheme.colorScheme.secondary
                                             else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (newUserSoundUri != null) "تم اختيار نغمة مخصصة 🎵"
                                   else "اختر ملفاً صوتياً (اختياري) 🎵",
                            color = if (newUserSoundUri != null) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (newUserSoundUri != null) {
                        TextButton(onClick = { newUserSoundUri = null }) {
                            Text("إلغاء النغمة المخصصة 🗑️", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank()) {
                            viewModel.addNewUser(newUserName.trim(), newUserEmoji, newUserColorHex, newUserSoundUri)
                            newUserName = ""
                            newUserSoundUri = null
                            showAddUserDialog = false
                        }
                    }
                ) {
                    Text("إضافة")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // 4. Edit User Dialog (Long press trigger)
    if (showEditUserDialog && selectedUserToEdit != null) {
        val user = selectedUserToEdit!!
        var editName by remember(user) { mutableStateOf(user.name) }
        var editEmoji by remember(user) { mutableStateOf(user.avatarEmoji) }
        var editColorHex by remember(user) { mutableStateOf(user.colorHex) }
        var editSoundUri by remember(user) { mutableStateOf(user.customSoundUri) }

        val editAudioLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                editSoundUri = uri.toString()
            }
        }

        val emojis = listOf("😎", "🤖", "🚀", "😺", "🦊", "🐼", "🦁", "🦖", "👾", "🥑", "👨‍💻", "⚽", "🧙", "🧛", "🦄", "🎯", "🎸", "🌟", "🍿", "🍕", "🍔", "🧁", "🍩", "🚗", "🏆", "🎨")
        val colors = listOf("#FF0055", "#00E5FF", "#39FF14", "#FFEA00", "#7D26CD", "#FF9100", "#00FF66", "#E100FF", "#00E676", "#FF5252", "#FFD700", "#FF4081")

        AlertDialog(
            onDismissRequest = { showEditUserDialog = false },
            title = { Text("تعديل المشارك") },
            text = {
                val scrollState = rememberScrollState()
                Column(modifier = Modifier.verticalScroll(scrollState).verticalScrollbar(scrollState)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("الاسم") },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("اختر الرمز التعبيري (Avatar):")
                    Spacer(modifier = Modifier.height(8.dp))
                    val emojiRowState = rememberLazyListState()
                    LazyRow(
                        state = emojiRowState,
                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(emojiRowState)
                    ) {
                        items(emojis.size) { idx ->
                            val e = emojis[idx]
                            val selected = editEmoji == e
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                                    .clickable { editEmoji = e },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(e, fontSize = 22.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("اختر لون التخصيص:")
                    Spacer(modifier = Modifier.height(8.dp))
                    val colorRowState = rememberLazyListState()
                    LazyRow(
                        state = colorRowState,
                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(colorRowState)
                    ) {
                        items(colors.size) { idx ->
                            val c = colors[idx]
                            val selected = editColorHex == c
                            val col = Color(android.graphics.Color.parseColor(c))
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        if (selected) 3.dp else 0.dp,
                                        MaterialTheme.colorScheme.onBackground,
                                        CircleShape
                                    )
                                    .clickable { editColorHex = c }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("نغمة انتهاء الدور المخصصة:")
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { editAudioLauncher.launch("audio/*") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (editSoundUri != null) MaterialTheme.colorScheme.secondary
                                             else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (editSoundUri != null) "تم اختيار نغمة مخصصة 🎵"
                                   else "اختر ملفاً صوتياً (اختياري) 🎵",
                            color = if (editSoundUri != null) MaterialTheme.colorScheme.onSecondary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (editSoundUri != null) {
                        TextButton(onClick = { editSoundUri = null }) {
                            Text("إلغاء النغمة المخصصة 🗑️", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Delete User Button
                    Button(
                        onClick = {
                            viewModel.deleteUser(user)
                            showEditUserDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف المستخدم نهائياً 🗑️", color = Color.White)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateUser(
                                user.copy(
                                    name = editName.trim(),
                                    avatarEmoji = editEmoji,
                                    colorHex = editColorHex,
                                    customSoundUri = editSoundUri
                                )
                            )
                            showEditUserDialog = false
                        }
                    }
                ) {
                    Text("حفظ التعديلات")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditUserDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
