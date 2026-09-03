package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.DeletedHistoryEntity
import com.example.ui.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeletedHistoryScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val deletedList by viewModel.allDeletedHistory.collectAsState()
    val users by viewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("ALL") } // "ALL", "SINGLE", "BULK"
    var selectedUserId by remember { mutableStateOf<Int?>(null) }
    var sortByNewest by remember { mutableStateOf(true) }

    // Dialog states
    var selectedItemForAction by remember { mutableStateOf<DeletedHistoryEntity?>(null) }
    var showDetailsDialog by remember { mutableStateOf<DeletedHistoryEntity?>(null) }
    var showRestoreChoiceDialog by remember { mutableStateOf<DeletedHistoryEntity?>(null) }
    var showPermanentDeleteChoiceDialog by remember { mutableStateOf<DeletedHistoryEntity?>(null) }
    var showClearAllConfirmDialog by remember { mutableStateOf(false) }

    // Filter & Search computation
    val filteredList = remember(deletedList, searchQuery, selectedFilterType, selectedUserId, sortByNewest) {
        val list = deletedList.filter { item ->
            val matchesType = when (selectedFilterType) {
                "SINGLE" -> item.deletionType == "SINGLE"
                "BULK" -> item.deletionType == "BULK"
                else -> true
            }
            val matchesUser = selectedUserId == null || item.userId == selectedUserId
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.userName.contains(searchQuery, ignoreCase = true) ||
                (item.sessionName?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.bulkBatchId?.contains(searchQuery, ignoreCase = true) == true)
            }
            matchesType && matchesUser && matchesSearch
        }
        if (sortByNewest) list.sortedByDescending { it.deletedAt }
        else list.sortedBy { it.deletedAt }
    }

    // Statistics
    val totalDeletedCount = deletedList.size
    val singleCount = deletedList.count { it.deletionType == "SINGLE" }
    val bulkCount = deletedList.count { it.deletionType == "BULK" }
    val totalDurationSec: Long = deletedList.fold(0L) { acc, item -> acc + item.elapsedSeconds }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("سجل الأدوار المحذوفة 🗑️", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (deletedList.isNotEmpty()) {
                        IconButton(onClick = { showClearAllConfirmDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "حذف نهائي للكل", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Secret Archive Badge & Stats Card
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "الأرشيف السري للمحذوفات 🗝️",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                                Text("$totalDeletedCount دور", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox(title = "حذف مفرد", value = "$singleCount", color = Color(0xFF2196F3))
                            StatBox(title = "حذف جماعي", value = "$bulkCount", color = Color(0xFFFF9800))
                            StatBox(title = "إجمالي الوقت", value = formatTimeShort(totalDurationSec), color = Color(0xFF4CAF50))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // 2. Search Field
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("بحث باسم اللاعب أو الجلسة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 3. Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = selectedFilterType == "ALL",
                        onClick = { selectedFilterType = "ALL" },
                        label = { Text("الكل ($totalDeletedCount)") }
                    )
                    FilterChip(
                        selected = selectedFilterType == "SINGLE",
                        onClick = { selectedFilterType = "SINGLE" },
                        label = { Text("مفرد ($singleCount) 🏷️") }
                    )
                    FilterChip(
                        selected = selectedFilterType == "BULK",
                        onClick = { selectedFilterType = "BULK" },
                        label = { Text("جماعي ($bulkCount) 📦") }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { sortByNewest = !sortByNewest }) {
                        Icon(
                            if (sortByNewest) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "ترتيب",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. Bulk Action Quick Buttons
            if (deletedList.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.restoreDeletedBatch("ALL")
                                Toast.makeText(context, "تمت إستعادة جميع الأدوار بنجاح! 🔄", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إستعادة الكل", color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showClearAllConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف نهائي للكل", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            // 5. Deleted Items List
            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "لا توجد أدوار محذوفة مطابقة",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "أي دور يتم حذفه من سجل الأدوار سينتقل هنا تلقائياً ويمكن استعادته في أي وقت.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val user = users.find { it.id == item.userId }
                    val userEmoji = user?.avatarEmoji ?: "👤"
                    DeletedItemCard(
                        item = item,
                        userEmoji = userEmoji,
                        onClick = { selectedItemForAction = item }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // --- Action Selector Dialog ---
    if (selectedItemForAction != null) {
        val item = selectedItemForAction!!
        val user = users.find { it.id == item.userId }
        val userEmoji = user?.avatarEmoji ?: "👤"
        AlertDialog(
            onDismissRequest = { selectedItemForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(userEmoji, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(item.userName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "جلسة: ${item.sessionName ?: "غير محدد"} • مدة: ${formatDuration(item.elapsedSeconds)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "اختر الإجراء المطلوب لهذا الدور المحذوف:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Restore Button
                    OutlinedButton(
                        onClick = {
                            selectedItemForAction = null
                            if (item.deletionType == "BULK" && item.bulkBatchId != null) {
                                showRestoreChoiceDialog = item
                            } else {
                                viewModel.restoreDeletedItem(item.id)
                                Toast.makeText(context, "تمت استعادة دور ${item.userName} بنجاح! 🔄", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4CAF50))
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إستعادة الدور 🔄", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Permanent Delete Button
                    OutlinedButton(
                        onClick = {
                            selectedItemForAction = null
                            if (item.deletionType == "BULK" && item.bulkBatchId != null) {
                                showPermanentDeleteChoiceDialog = item
                            } else {
                                viewModel.permanentlyDeleteTrashItem(item.id)
                                Toast.makeText(context, "تم حذف الدور نهائياً 🗑️", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف نهائي ❌", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. Full Details Button
                    OutlinedButton(
                        onClick = {
                            selectedItemForAction = null
                            showDetailsDialog = item
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("عرض التفاصيل الكاملة 📋", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedItemForAction = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- Restore Choice Dialog (for Bulk Deletions) ---
    if (showRestoreChoiceDialog != null) {
        val item = showRestoreChoiceDialog!!
        val batchMatches = deletedList.count { it.bulkBatchId == item.bulkBatchId }
        AlertDialog(
            onDismissRequest = { showRestoreChoiceDialog = null },
            title = { Text("خيارات الإستعادة 🔄", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "هذا الدور حُذف ضمن حذف جماعي يحتوي على ($batchMatches) أدوار.\n\nهل ترغب في إستعادة هذا الدور فقط أم إستعادة كامل المجموعة التي حذفت معه؟",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        item.bulkBatchId?.let { viewModel.restoreDeletedBatch(it) }
                        showRestoreChoiceDialog = null
                        Toast.makeText(context, "تمت استعادة كامل المجموعة ($batchMatches أدوار) بنجاح! 🎉", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("إستعادة المجموعة كاملة ($batchMatches)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.restoreDeletedItem(item.id)
                        showRestoreChoiceDialog = null
                        Toast.makeText(context, "تمت استعادة هذا الدور فقط بنجاح! 🔄", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("إستعادة هذا الدور فقط")
                }
            }
        )
    }

    // --- Permanent Delete Choice Dialog (for Bulk Deletions) ---
    if (showPermanentDeleteChoiceDialog != null) {
        val item = showPermanentDeleteChoiceDialog!!
        val batchMatches = deletedList.count { it.bulkBatchId == item.bulkBatchId }
        AlertDialog(
            onDismissRequest = { showPermanentDeleteChoiceDialog = null },
            title = { Text("تأكيد الحذف النهائي ⚠️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text(
                    "هذا الدور حُذف ضمن حذف جماعي يحتوي على ($batchMatches) أدوار.\n\nهل ترغب في حذف هذا الدور فقط نهائياً أم حذف كامل أدوار المجموعة نهائياً؟\n(هذا الإجراء لا يمكن التراجع عنه)",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        item.bulkBatchId?.let { viewModel.permanentlyDeleteTrashBatch(it) }
                        showPermanentDeleteChoiceDialog = null
                        Toast.makeText(context, "تم الحذف النهائي لجميع أدوار المجموعة ($batchMatches أدوار)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("حذف المجموعة كاملة نهائياً ($batchMatches)")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        viewModel.permanentlyDeleteTrashItem(item.id)
                        showPermanentDeleteChoiceDialog = null
                        Toast.makeText(context, "تم الحذف النهائي لهذا الدور فقط", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("حذف هذا الدور فقط")
                }
            }
        )
    }

    // --- Details Dialog ---
    if (showDetailsDialog != null) {
        val item = showDetailsDialog!!
        val user = users.find { it.id == item.userId }
        val userEmoji = user?.avatarEmoji ?: "👤"
        val dateFmt = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("تفاصيل الدور المحذوف 📋", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DetailRow(label = "اللاعب:", value = "$userEmoji ${item.userName} (ID: ${item.userId})")
                    DetailRow(label = "الجلسة:", value = "${item.sessionName ?: "غير محدد"} (ID: ${item.sessionId})")
                    DetailRow(label = "مدة الدور الفعلية:", value = "${item.elapsedSeconds} ثانية (${formatDuration(item.elapsedSeconds)})")
                    DetailRow(label = "تاريخ لعب الدور:", value = dateFmt.format(Date(item.originalTimestamp)))
                    DetailRow(label = "تاريخ ووقت الحذف:", value = dateFmt.format(Date(item.deletedAt)))
                    DetailRow(
                        label = "نوع الحذف:",
                        value = if (item.deletionType == "BULK") "حذف جماعي 📦 (كود: ${item.bulkBatchId ?: "غير محدد"})" else "حذف مفرد 🏷️"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailsDialog = null }) {
                    Text("تم")
                }
            }
        )
    }

    // --- Clear All Confirm Dialog ---
    if (showClearAllConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirmDialog = false },
            title = { Text("تفريغ سلة المحذوفات نهائياً ⚠️", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error) },
            text = {
                Text("هل أنت متأكد من حذف جميع الأدوار الموجودة في سلة المحذوفات نهائياً؟ لن تتمكن من استعادتها أبداً.", fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllTrash()
                        showClearAllConfirmDialog = false
                        Toast.makeText(context, "تم تفريغ سلة المحذوفات نهائياً", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، تفريغ الكل نهائياً")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun DeletedItemCard(
    item: DeletedHistoryEntity,
    userEmoji: String,
    onClick: () -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("yyyy/MM/dd • HH:mm", Locale.getDefault()) }
    val deletedDateStr = remember(item.deletedAt) { dateFmt.format(Date(item.deletedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(userEmoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.userName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "• ${item.sessionName ?: ""}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "مدة الدور: ${formatDuration(item.elapsedSeconds)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    "حُذف في: $deletedDateStr",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Deletion Type Badge
            Column(horizontalAlignment = Alignment.End) {
                val isBulk = item.deletionType == "BULK"
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isBulk) Color(0xFFFF9800).copy(alpha = 0.15f) else Color(0xFF2196F3).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (isBulk) "جماعي 📦" else "مفرد 🏷️",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBulk) Color(0xFFE65100) else Color(0xFF1976D2),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatBox(title: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End)
    }
}

private fun formatDuration(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    return if (hrs > 0) "${hrs}س ${mins}د ${secs}ث"
    else if (mins > 0) "${mins}د ${mins}ث"
    else "${secs} ثانية"
}

private fun formatTimeShort(totalSec: Long): String {
    val hrs = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    return if (hrs > 0) "${hrs}س ${mins}د" else "${mins}د"
}
