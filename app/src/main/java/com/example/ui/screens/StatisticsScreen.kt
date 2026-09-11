package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.UserEntity
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.verticalScrollbar
import java.text.SimpleDateFormat
import java.util.*

enum class StatFilterMode { DAY, WEEK, MONTH }

@Composable
fun StatisticsScreen(
    viewModel: MainViewModel
) {
    val users by viewModel.users.collectAsState()
    val history by viewModel.history.collectAsState()

    var filterMode by remember { mutableStateOf(StatFilterMode.DAY) }
    var referenceDate by remember { mutableStateOf(Calendar.getInstance()) }

    // Calculate start and end timestamp ranges for the selected filter interval
    val range = remember(filterMode, referenceDate) {
        val startCal = referenceDate.clone() as Calendar
        val endCal = referenceDate.clone() as Calendar
        
        when (filterMode) {
            StatFilterMode.DAY -> {
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            StatFilterMode.WEEK -> {
                startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                endCal.set(Calendar.DAY_OF_WEEK, endCal.firstDayOfWeek)
                endCal.add(Calendar.DAY_OF_WEEK, 6)
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
            StatFilterMode.MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.set(Calendar.SECOND, 0)
                startCal.set(Calendar.MILLISECOND, 0)
                
                endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                endCal.set(Calendar.SECOND, 59)
                endCal.set(Calendar.MILLISECOND, 999)
            }
        }
        startCal.timeInMillis..endCal.timeInMillis
    }

    // Filter finished histories matching current period
    val filteredHistoryForStats = remember(history, range) {
        history.filter { it.actionType == "FINISH" && it.timestamp in range }
    }

    // Re-aggregate and map user stats inside current period
    val filteredUsersStats = remember(users, filteredHistoryForStats) {
        users.map { user ->
            val userHistory = filteredHistoryForStats.filter { it.userId == user.id }
            val turnsCount = userHistory.size
            val totalDurationSeconds = userHistory.sumOf { it.elapsedSeconds }
            val avgDurationSeconds = if (turnsCount > 0) totalDurationSeconds / turnsCount else 0L
            user.copy(
                turnsCount = turnsCount,
                totalDurationSeconds = totalDurationSeconds,
                averageDurationSeconds = avgDurationSeconds
            )
        }
    }

    // Calculated metrics
    val totalTurns = remember(filteredUsersStats) { filteredUsersStats.sumOf { it.turnsCount } }
    val totalSeconds = remember(filteredUsersStats) { filteredUsersStats.sumOf { it.totalDurationSeconds } }
    val totalHoursStr = remember(totalSeconds) { String.format("%.2f", totalSeconds / 3600.0) }

    val longestTurn = remember(filteredHistoryForStats) { filteredHistoryForStats.maxOfOrNull { it.elapsedSeconds } ?: 0L }
    val shortestTurn = remember(filteredHistoryForStats) { filteredHistoryForStats.minOfOrNull { it.elapsedSeconds } ?: 0L }
    val averageTurn = remember(totalTurns, totalSeconds) { if (totalTurns > 0) totalSeconds / totalTurns else 0L }

    val mostActiveUser = remember(filteredUsersStats) { filteredUsersStats.filter { it.totalDurationSeconds > 0L }.maxByOrNull { it.totalDurationSeconds } }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(filterMode) {
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val newCal = referenceDate.clone() as Calendar
                        if (totalDragX > 100f) {
                            // Swipe Right -> RTL language: next period
                            when (filterMode) {
                                StatFilterMode.DAY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
                                StatFilterMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
                                StatFilterMode.MONTH -> newCal.add(Calendar.MONTH, 1)
                            }
                            referenceDate = newCal
                        } else if (totalDragX < -100f) {
                            // Swipe Left -> RTL language: previous period
                            when (filterMode) {
                                StatFilterMode.DAY -> newCal.add(Calendar.DAY_OF_YEAR, -1)
                                StatFilterMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, -1)
                                StatFilterMode.MONTH -> newCal.add(Calendar.MONTH, -1)
                            }
                            referenceDate = newCal
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount
                    }
                )
            }
            .verticalScroll(scrollState)
            .verticalScrollbar(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "الإحصائيات والتحليلات 📊",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            "تصفح وحلل أداء المشاركين مع إمكانية السحب للتنقل",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start).padding(bottom = 16.dp)
        )

        // 1. Time Filters Tabs
        TabRow(
            selectedTabIndex = filterMode.ordinal,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Tab(
                selected = filterMode == StatFilterMode.DAY,
                onClick = { filterMode = StatFilterMode.DAY },
                text = { Text("اليوم") }
            )
            Tab(
                selected = filterMode == StatFilterMode.WEEK,
                onClick = { filterMode = StatFilterMode.WEEK },
                text = { Text("الأسبوع") }
            )
            Tab(
                selected = filterMode == StatFilterMode.MONTH,
                onClick = { filterMode = StatFilterMode.MONTH },
                text = { Text("الشهر") }
            )
        }

        // 2. Active Period Control Bar
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                IconButton(onClick = {
                    val newCal = referenceDate.clone() as Calendar
                    when (filterMode) {
                        StatFilterMode.DAY -> newCal.add(Calendar.DAY_OF_YEAR, -1)
                        StatFilterMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, -1)
                        StatFilterMode.MONTH -> newCal.add(Calendar.MONTH, -1)
                    }
                    referenceDate = newCal
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Previous Period")
                }

                Text(
                    text = getPeriodLabel(filterMode, referenceDate),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    val newCal = referenceDate.clone() as Calendar
                    when (filterMode) {
                        StatFilterMode.DAY -> newCal.add(Calendar.DAY_OF_YEAR, 1)
                        StatFilterMode.WEEK -> newCal.add(Calendar.WEEK_OF_YEAR, 1)
                        StatFilterMode.MONTH -> newCal.add(Calendar.MONTH, 1)
                    }
                    referenceDate = newCal
                }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next Period")
                }
            }
        }

        // 3. Grid of metrics
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "إجمالي الساعات",
                value = "$totalHoursStr س",
                icon = Icons.Default.QueryBuilder,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            StatCard(
                title = "عدد الأدوار",
                value = "$totalTurns دور",
                icon = Icons.Default.Refresh,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(
                title = "أطول دور",
                value = formatDuration(longestTurn),
                icon = Icons.Default.Timer,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            )
            StatCard(
                title = "متوسط الدور",
                value = formatDuration(averageTurn),
                icon = Icons.Default.HourglassEmpty,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Active User Share Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "المشارك الأكثر نشاطاً في الفترة 👑",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (mostActiveUser != null && mostActiveUser.totalDurationSeconds > 0) {
                    val uColor = remember(mostActiveUser) {
                        try {
                            Color(android.graphics.Color.parseColor(mostActiveUser.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(uColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(mostActiveUser.avatarEmoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(mostActiveUser.name, fontWeight = FontWeight.Bold)
                                Text("مشاركة الفترة: ${formatDuration(mostActiveUser.totalDurationSeconds)}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Text(
                            text = "${mostActiveUser.turnsCount} أدوار",
                            fontWeight = FontWeight.Bold,
                            color = uColor
                        )
                    }
                } else {
                    Text("لا يوجد نشاط مسجل في هذه الفترة بعد.", color = Color.Gray, fontSize = 14.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Custom Graphical Charts Canvas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "توزيع أوقات المشاركة 📈",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))

                if (filteredUsersStats.none { it.totalDurationSeconds > 0L }) {
                    Box(
                        modifier = Modifier.height(150.dp).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("أكمل أدواراً في هذه الفترة لتحديث المخطط البياني.", color = Color.Gray, fontSize = 13.sp)
                    }
                } else {
                    CustomUsersBarChart(users = filteredUsersStats)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Professional Canvas + OpenGL Advanced Analytics Graph
        AdvancedTurnAnalyticsCanvasCard(
            history = filteredHistoryForStats,
            users = filteredUsersStats
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun CustomUsersBarChart(users: List<UserEntity>) {
    val activeUsersWithData = remember(users) {
        users.filter { it.totalDurationSeconds > 0L }
    }
    val maxDuration = remember(activeUsersWithData) {
        activeUsersWithData.maxOfOrNull { it.totalDurationSeconds }?.toFloat() ?: 1f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        activeUsersWithData.forEach { user ->
            val uColor = remember(user) {
                try {
                    Color(android.graphics.Color.parseColor(user.colorHex))
                } catch (e: Exception) {
                    Color.Gray
                }
            }

            val ratio = user.totalDurationSeconds.toFloat() / maxDuration

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${user.avatarEmoji} ${user.name}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(90.dp),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(16.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            size = size,
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                        drawRoundRect(
                            color = uColor,
                            size = Size(size.width * ratio, size.height),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = formatDuration(user.totalDurationSeconds),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

fun getPeriodLabel(mode: StatFilterMode, calendar: Calendar): String {
    return when (mode) {
        StatFilterMode.DAY -> {
            val sdf = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
            sdf.format(calendar.time)
        }
        StatFilterMode.WEEK -> {
            val startCal = calendar.clone() as Calendar
            startCal.set(Calendar.DAY_OF_WEEK, startCal.firstDayOfWeek)
            val endCal = startCal.clone() as Calendar
            endCal.add(Calendar.DAY_OF_WEEK, 6)
            
            val sdf = SimpleDateFormat("d MMMM", Locale("ar"))
            "أسبوع ${sdf.format(startCal.time)} - ${sdf.format(endCal.time)}"
        }
        StatFilterMode.MONTH -> {
            val sdf = SimpleDateFormat("MMMM yyyy", Locale("ar"))
            sdf.format(calendar.time)
        }
    }
}

private fun formatDuration(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) "${hrs}س ${mins}د" else if (mins > 0) "${mins}د ${secs}ث" else "${secs}ث"
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

enum class CanvasChartType {
    BEZIER_CURVE,
    PILLAR_BARS
}

@Composable
fun AdvancedTurnAnalyticsCanvasCard(
    history: List<com.example.database.HistoryEntity>,
    users: List<UserEntity>
) {
    var selectedChartType by remember { mutableStateOf(CanvasChartType.BEZIER_CURVE) }
    var scrubIndex by remember { mutableStateOf<Int?>(null) }

    val validHistory = remember(history) {
        history.sortedBy { it.timestamp }
    }

    val totalCount = validHistory.size
    val maxDuration = remember(validHistory) {
        (validHistory.maxOfOrNull { it.elapsedSeconds } ?: 60L).toFloat().coerceAtLeast(1f)
    }
    val avgDuration = remember(validHistory) {
        if (validHistory.isNotEmpty()) validHistory.map { it.elapsedSeconds }.average().toFloat() else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF00E5FF), Color(0xFF7C4DFF))
                                ),
                                RoundedCornerShape(10.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "الرسم البياني التحليلي المتقدم 📈",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "محرك Canvas الرسومي مع تفاعل مباشر بالسحب",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chart Type Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedChartType == CanvasChartType.BEZIER_CURVE,
                    onClick = {
                        selectedChartType = CanvasChartType.BEZIER_CURVE
                        scrubIndex = null
                    },
                    label = { Text("منحنى التطور الزمني 🌊", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedChartType == CanvasChartType.PILLAR_BARS,
                    onClick = {
                        selectedChartType = CanvasChartType.PILLAR_BARS
                        scrubIndex = null
                    },
                    label = { Text("أعمدة مقارنة اللاعبين 📊", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tooltip or Summary Card
            val activeScrubRecord = scrubIndex?.let { idx ->
                if (idx in validHistory.indices) validHistory[idx] else null
            }

            if (activeScrubRecord != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val sdf = SimpleDateFormat("hh:mm a", Locale("ar"))
                        val timeStr = sdf.format(Date(activeScrubRecord.timestamp))
                        Text(
                            "👤 ${activeScrubRecord.userName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "⏱️ ${formatDuration(activeScrubRecord.elapsedSeconds)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "🕒 $timeStr",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "إجمالي الأدوار: $totalCount",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        "المتوسط: ${formatDuration(avgDuration.toLong())}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        "القمة: ${formatDuration(maxDuration.toLong())}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            // Canvas Chart Body
            if (validHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "لا توجد أدوار منجزة في هذه الفترة لعرض المنحنى.",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            } else {
                val primaryColor = MaterialTheme.colorScheme.primary
                val secondaryColor = MaterialTheme.colorScheme.secondary
                val tertiaryColor = MaterialTheme.colorScheme.tertiary
                val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .pointerInput(validHistory, selectedChartType) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val count = if (selectedChartType == CanvasChartType.BEZIER_CURVE) validHistory.size else users.filter { it.totalDurationSeconds > 0 }.size
                                    if (count > 0) {
                                        val idx = ((offset.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                        scrubIndex = idx
                                    }
                                },
                                onDragEnd = {
                                    // Keep current inspect or reset after delay
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val count = if (selectedChartType == CanvasChartType.BEZIER_CURVE) validHistory.size else users.filter { it.totalDurationSeconds > 0 }.size
                                    if (count > 0) {
                                        val idx = ((change.position.x / size.width.toFloat()) * count).toInt().coerceIn(0, count - 1)
                                        scrubIndex = idx
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                        val w = size.width
                        val h = size.height
                        val paddingBottom = 16.dp.toPx()
                        val paddingTop = 12.dp.toPx()
                        val chartH = h - paddingBottom - paddingTop

                        // Draw dashed horizontal background gridlines (3 levels)
                        for (i in 0..3) {
                            val y = paddingTop + chartH * (i / 3f)
                            drawLine(
                                color = surfaceVariant.copy(alpha = 0.5f),
                                start = androidx.compose.ui.geometry.Offset(0f, y),
                                end = androidx.compose.ui.geometry.Offset(w, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )
                        }

                        if (selectedChartType == CanvasChartType.BEZIER_CURVE) {
                            // Draw Bézier Smooth Area Curve
                            val points = validHistory.mapIndexed { index, item ->
                                val x = if (validHistory.size > 1) {
                                    (index.toFloat() / (validHistory.size - 1)) * w
                                } else {
                                    w / 2f
                                }
                                val yRatio = (item.elapsedSeconds.toFloat() / maxDuration).coerceIn(0f, 1f)
                                val y = paddingTop + chartH * (1f - yRatio)
                                androidx.compose.ui.geometry.Offset(x, y)
                            }

                            if (points.size >= 2) {
                                val strokePath = androidx.compose.ui.graphics.Path()
                                val fillPath = androidx.compose.ui.graphics.Path()

                                strokePath.moveTo(points[0].x, points[0].y)
                                fillPath.moveTo(points[0].x, paddingTop + chartH)
                                fillPath.lineTo(points[0].x, points[0].y)

                                for (i in 0 until points.size - 1) {
                                    val p0 = points[i]
                                    val p1 = points[i + 1]
                                    val controlX1 = (p0.x + p1.x) / 2f
                                    val controlY1 = p0.y
                                    val controlX2 = (p0.x + p1.x) / 2f
                                    val controlY2 = p1.y

                                    strokePath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                                }

                                fillPath.lineTo(points.last().x, paddingTop + chartH)
                                fillPath.close()

                                // Area Gradient Fill
                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            primaryColor.copy(alpha = 0.45f),
                                            secondaryColor.copy(alpha = 0.15f),
                                            Color.Transparent
                                        ),
                                        startY = paddingTop,
                                        endY = paddingTop + chartH
                                    )
                                )

                                // Glowing Curve Line
                                drawPath(
                                    path = strokePath,
                                    brush = Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF00E5FF),
                                            primaryColor,
                                            secondaryColor,
                                            Color(0xFFFF4081)
                                        )
                                    ),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 3.5.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                        join = androidx.compose.ui.graphics.StrokeJoin.Round
                                    )
                                )

                                // Draw Data Points
                                points.forEachIndexed { idx, pt ->
                                    val isSelected = scrubIndex == idx
                                    val ptRadius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx()
                                    val ptColor = if (isSelected) Color(0xFFFFD700) else primaryColor

                                    if (isSelected) {
                                        // Glow ring
                                        drawCircle(
                                            color = ptColor.copy(alpha = 0.3f),
                                            radius = ptRadius * 2f,
                                            center = pt
                                        )
                                        // Vertical scrubber guideline
                                        drawLine(
                                            color = ptColor.copy(alpha = 0.7f),
                                            start = androidx.compose.ui.geometry.Offset(pt.x, paddingTop),
                                            end = androidx.compose.ui.geometry.Offset(pt.x, paddingTop + chartH),
                                            strokeWidth = 1.5.dp.toPx(),
                                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                        )
                                    }

                                    drawCircle(
                                        color = Color.White,
                                        radius = ptRadius + 1.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = ptColor,
                                        radius = ptRadius,
                                        center = pt
                                    )
                                }
                            } else if (points.size == 1) {
                                val pt = points[0]
                                drawCircle(
                                    color = primaryColor,
                                    radius = 6.dp.toPx(),
                                    center = pt
                                )
                            }
                        } else {
                            // Pillar Bars Mode
                            val activeUsers = users.filter { it.totalDurationSeconds > 0 }
                            if (activeUsers.isNotEmpty()) {
                                val barMax = (activeUsers.maxOfOrNull { it.totalDurationSeconds } ?: 1L).toFloat()
                                val barCount = activeUsers.size
                                val totalSlotWidth = w / barCount
                                val barWidth = (totalSlotWidth * 0.55f).coerceIn(12.dp.toPx(), 44.dp.toPx())

                                activeUsers.forEachIndexed { index, u ->
                                    val cx = (index * totalSlotWidth) + (totalSlotWidth / 2f)
                                    val ratio = (u.totalDurationSeconds.toFloat() / barMax).coerceIn(0.05f, 1f)
                                    val barHeight = chartH * ratio
                                    val topY = paddingTop + (chartH - barHeight)
                                    val leftX = cx - (barWidth / 2f)

                                    val uColor = try {
                                        Color(android.graphics.Color.parseColor(u.colorHex))
                                    } catch (e: Exception) {
                                        primaryColor
                                    }

                                    // Bar Background track
                                    drawRoundRect(
                                        color = surfaceVariant.copy(alpha = 0.3f),
                                        topLeft = androidx.compose.ui.geometry.Offset(leftX, paddingTop),
                                        size = Size(barWidth, chartH),
                                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                    )

                                    // Gradient Pillar
                                    drawRoundRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                uColor,
                                                uColor.copy(alpha = 0.6f)
                                            ),
                                            startY = topY,
                                            endY = paddingTop + chartH
                                        ),
                                        topLeft = androidx.compose.ui.geometry.Offset(leftX, topY),
                                        size = Size(barWidth, barHeight),
                                        cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                                    )

                                    // Glowing Cap
                                    drawCircle(
                                        color = Color.White,
                                        radius = (barWidth / 4f).coerceAtLeast(2.dp.toPx()),
                                        center = androidx.compose.ui.geometry.Offset(cx, topY + (barWidth / 3f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
