package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
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
import com.example.database.HistoryEntity
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.components.verticalScrollbar
import com.example.ui.theme.LocalIsLiquidGlass
import com.example.ui.theme.liquidGlassCardColors
import com.example.ui.theme.liquidGlassContainer
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel
) {
    val history by viewModel.history.collectAsState()
    val currentSessionId by viewModel.currentSessionId.collectAsState()

    // History Customization Preferences
    val durationFormat by viewModel.historyDurationFormat.collectAsState()
    val timeFormat by viewModel.historyTimeFormat.collectAsState()
    val dateFormat by viewModel.historyDateFormat.collectAsState()
    val sortOrder by viewModel.historySortOrder.collectAsState()
    val showExactTimestamps by viewModel.historyShowExactTimestamps.collectAsState()
    val showTimelineDots by viewModel.historyShowTimelineDots.collectAsState()

    var showDeleteSingleDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<HistoryEntity?>(null) }
    var showClearSessionDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Filter history to current session
    val sessionHistory = remember(history, currentSessionId) {
        history.filter { it.sessionId == currentSessionId }
    }

    // Apply sorting preference
    val sortedHistory = remember(sessionHistory, sortOrder) {
        when (sortOrder) {
            "OLDEST" -> sessionHistory.sortedBy { it.timestamp }
            "LONGEST" -> sessionHistory.sortedByDescending { it.elapsedSeconds }
            "SHORTEST" -> sessionHistory.sortedBy { it.elapsedSeconds }
            else -> sessionHistory.sortedByDescending { it.timestamp } // "NEWEST"
        }
    }

    // Group history items by day (yyyy-MM-dd)
    val groupedHistory = remember(sortedHistory) {
        sortedHistory.groupBy {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.format(Date(it.timestamp))
        }
    }

    val isLiquidGlass = LocalIsLiquidGlass.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isLiquidGlass) Color.Transparent else MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Timeline Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "سجل الأدوار والنشاط 📜",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "الأدوار المنتهية (${sessionHistory.size} دور مسجل)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            if (sessionHistory.isNotEmpty()) {
                IconButton(
                    onClick = { showClearSessionDialog = true }
                ) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "مسح سجل الجلسة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (sessionHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "السجل فارغ حتى الآن!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "ابدأ وأكمل بعض الأدوار لتراها هنا.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScrollbar(listState),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                groupedHistory.forEach { (dateStr, records) ->
                    val dateHeader = formatHistoryDateHeader(dateStr, dateFormat)

                    item {
                        DateSeparator(dateHeader)
                    }

                    items(records, key = { it.id }) { record ->
                        TimelineItem(
                            record = record,
                            durationFormat = durationFormat,
                            timeFormat = timeFormat,
                            showExactTimestamps = showExactTimestamps,
                            showTimelineDots = showTimelineDots,
                            onLongClick = {
                                recordToDelete = record
                                showDeleteSingleDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Single Record Delete Dialog (Clean - without mentioning secret trash)
    if (showDeleteSingleDialog && recordToDelete != null) {
        val target = recordToDelete!!
        AlertDialog(
            onDismissRequest = { showDeleteSingleDialog = false },
            title = { Text("حذف دور من السجل 🗑️", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف دور (${target.userName}) من السجل؟", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHistoryItem(target.id)
                        showDeleteSingleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSingleDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Clear Session History Dialog (Clean - without mentioning secret trash)
    if (showClearSessionDialog) {
        AlertDialog(
            onDismissRequest = { showClearSessionDialog = false },
            title = { Text("مسح سجل الجلسة ⚠️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = { Text("هل أنت متأكد من رغبتك في مسح سجل هذه الجلسة بالكامل؟", fontSize = 14.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        currentSessionId?.let { viewModel.deleteHistoryBySession(it) }
                        showClearSessionDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، مسح السجل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearSessionDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun DateSeparator(dateText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
        Text(
            text = "  $dateText  ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelineItem(
    record: HistoryEntity,
    durationFormat: String,
    timeFormat: String,
    showExactTimestamps: Boolean,
    showTimelineDots: Boolean,
    onLongClick: () -> Unit
) {
    val uColor = remember(record.userColorHex) {
        try {
            Color(android.graphics.Color.parseColor(record.userColorHex))
        } catch (e: Exception) {
            Color.Gray
        }
    }

    val pattern = if (timeFormat == "24H") "HH:mm:ss" else "hh:mm:ss a"
    val locale = if (timeFormat == "24H") Locale.getDefault() else Locale("ar")

    // End time is when the record was saved
    val formattedEnd = remember(record.timestamp, timeFormat) {
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.format(Date(record.timestamp))
    }

    // Start time is end time minus the duration
    val formattedStart = remember(record, timeFormat) {
        val startTime = record.timestamp - (record.elapsedSeconds * 1000)
        val sdf = SimpleDateFormat(pattern, locale)
        sdf.format(Date(startTime))
    }

    val formattedDuration = remember(record.elapsedSeconds, durationFormat) {
        formatHistoryDuration(record.elapsedSeconds, durationFormat)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical Timeline node indicators
        if (showTimelineDots) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(uColor)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(44.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        // History content Card with long click support
        Card(
            modifier = Modifier
                .weight(1f)
                .liquidGlassContainer(RoundedCornerShape(14.dp))
                .combinedClickable(
                    onLongClick = onLongClick,
                    onClick = {}
                ),
            shape = RoundedCornerShape(14.dp),
            colors = liquidGlassCardColors(),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(uColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = record.userName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = uColor,
                            fontSize = 15.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = record.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (showExactTimestamps) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "من $formattedStart إلى $formattedEnd",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Styled Duration Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = uColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = formattedDuration,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = uColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Formats duration seconds according to user preference
 */
fun formatHistoryDuration(totalSec: Long, format: String): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60

    return when (format) {
        "DIGITAL_MM_SS" -> {
            if (hrs > 0) {
                String.format(Locale.ENGLISH, "%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(Locale.ENGLISH, "%02d:%02d", mins, secs)
            }
        }
        "DIGITAL_M_SS" -> {
            if (hrs > 0) {
                String.format(Locale.ENGLISH, "%d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(Locale.ENGLISH, "%d:%02d", mins, secs)
            }
        }
        "DIGITAL_HH_MM_SS" -> {
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", hrs, mins, secs)
        }
        "TEXT_ARABIC" -> {
            when {
                hrs > 0 -> "${hrs} ساعة و ${mins} دقيقة و ${secs} ثانية"
                mins > 0 -> "${mins} دقيقة و ${secs} ثانية"
                else -> "${secs} ثانية"
            }
        }
        "TEXT_COMPACT" -> {
            when {
                hrs > 0 -> "${hrs}س ${mins}د ${secs}ث"
                mins > 0 -> "${mins}د ${secs}ث"
                else -> "${secs}ث"
            }
        }
        "BADGE_PLUS" -> {
            when {
                hrs > 0 -> "+ ${hrs}س : ${mins}د"
                mins > 0 -> "+ ${mins}د : ${secs}ث"
                else -> "+ ${secs}ث"
            }
        }
        "TOTAL_SECONDS" -> {
            "$totalSec ثانية"
        }
        else -> {
            String.format(Locale.ENGLISH, "%02d:%02d", mins, secs)
        }
    }
}

/**
 * Formats date header according to date format preference
 */
fun formatHistoryDateHeader(dateStr: String, dateFormat: String): String {
    return try {
        val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: return dateStr
        val calToday = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { time = parsedDate }

        when (dateFormat) {
            "RELATIVE" -> {
                val diffDays = (calToday.get(Calendar.DAY_OF_YEAR) - calItem.get(Calendar.DAY_OF_YEAR))
                val sameYear = calToday.get(Calendar.YEAR) == calItem.get(Calendar.YEAR)
                if (sameYear && diffDays == 0) {
                    "اليوم 📍"
                } else if (sameYear && diffDays == 1) {
                    "أمس ⏳"
                } else if (sameYear && diffDays == 2) {
                    "أول أمس 🗓️"
                } else {
                    SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(parsedDate)
                }
            }
            "SLASH_YMD" -> {
                SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH).format(parsedDate)
            }
            "SLASH_DMY" -> {
                SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(parsedDate)
            }
            else -> { // "FULL_ARABIC"
                SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar")).format(parsedDate)
            }
        }
    } catch (e: Exception) {
        dateStr
    }
}
