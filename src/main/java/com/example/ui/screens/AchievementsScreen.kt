package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.database.AchievementEntity
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AchievementsScreen(
    viewModel: MainViewModel
) {
    val achievements by viewModel.userAchievements.collectAsState()
    val users by viewModel.users.collectAsState()
    val selectedUserId by viewModel.selectedAchievementUserId.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // "ALL", "UNLOCKED", "LOCKED"
    var selectedAchievementInfo by remember { mutableStateOf<AchievementEntity?>(null) }

    val unlockedCount = remember(achievements) { achievements.count { it.isUnlocked } }
    val totalCount = achievements.size
    var isHeaderVisible by remember { mutableStateOf(true) }
    val gridState = rememberLazyGridState()

    val filteredAchievements = remember(achievements, searchQuery, selectedFilter) {
        achievements.filter { item ->
            val matchesFilter = when (selectedFilter) {
                "UNLOCKED" -> item.isUnlocked
                "LOCKED" -> !item.isUnlocked
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                item.title.contains(searchQuery, ignoreCase = true) ||
                item.description.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Achievement overview summary with close button
        AnimatedVisibility(visible = isHeaderVisible) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFD700).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "خزانة الإنجازات والأوسمة 🏆",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "تم فتح $unlockedCount من أصل $totalCount إنجازاً (${if (totalCount > 0) (unlockedCount * 100 / totalCount) else 0}%)",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                    }

                    // Close (X) button to dismiss card and maximize screen space
                    IconButton(
                        onClick = { isHeaderVisible = false },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "إغلاق البطاقة لتوسيع الشاشة",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Mini banner toggle when header is closed
        if (!isHeaderVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "خزانة الإنجازات والأوسمة ($unlockedCount/$totalCount) 🏆",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(
                    onClick = { isHeaderVisible = true },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("إظهار الملخص ⬇️", fontSize = 11.sp)
                }
            }
        }

        // Horizontal User Selection list for individual achievements
        if (users.isNotEmpty()) {
            Text(
                "اختر المشارك لعرض أوسمته الخاصة 👤:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.align(Alignment.Start).padding(bottom = 6.dp)
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(users) { user ->
                    val selected = user.id == selectedUserId
                    val uColor = remember(user.colorHex) {
                        try { Color(android.graphics.Color.parseColor(user.colorHex)) } catch (e: Exception) { Color.Gray }
                    }
                    Card(
                        modifier = Modifier.clickable { viewModel.selectAchievementUser(user.id) },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) uColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) uColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(user.avatarEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = user.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) uColor else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Search & Filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("الكل ($totalCount)") }
            )
            FilterChip(
                selected = selectedFilter == "UNLOCKED",
                onClick = { selectedFilter = "UNLOCKED" },
                label = { Text("المفتوحة ($unlockedCount) ⭐") }
            )
            FilterChip(
                selected = selectedFilter == "LOCKED",
                onClick = { selectedFilter = "LOCKED" },
                label = { Text("المغلقة (${totalCount - unlockedCount}) 🔒") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (achievements.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            // Adaptive Grid layout
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAchievements, key = { it.id }) { item ->
                    AchievementGridItem(item = item, onClick = {
                        selectedAchievementInfo = item
                    })
                }
            }
        }
    }

    // Achievements Info Details Dialog
    if (selectedAchievementInfo != null) {
        val item = selectedAchievementInfo!!
        val progressRatio = if (item.maxProgress > 0) item.progress.toFloat() / item.maxProgress.toFloat() else 0f
        AlertDialog(
            onDismissRequest = { selectedAchievementInfo = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.isUnlocked) "🏆 إنجاز مكتمل!" else "🔒 إنجاز قيد التحدي",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.isUnlocked) Color(0xFFFFD700).copy(alpha = 0.2f)
                                else Color.Gray.copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (item.isUnlocked) "⭐" else "🔒",
                            fontSize = 34.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Progress Indicator in Dialog
                    LinearProgressIndicator(
                        progress = { progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (item.isUnlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "التقدم الحالي: ${item.progress} / ${item.maxProgress}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { selectedAchievementInfo = null }) {
                    Text("تم")
                }
            }
        )
    }
}

@Composable
fun AchievementGridItem(
    item: AchievementEntity,
    onClick: () -> Unit
) {
    val unlocked = item.isUnlocked
    val progressRatio = if (item.maxProgress > 0) item.progress.toFloat() / item.maxProgress.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }
            .border(
                1.dp,
                if (unlocked) Color(0xFFFFD700).copy(alpha = 0.5f) else Color.Transparent,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.surface 
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Badge
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (unlocked) Color(0xFFFFD700).copy(alpha = 0.15f)
                        else Color.Gray.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (unlocked) {
                    Icon(
                        Icons.Default.EmojiEvents,
                        contentDescription = "Unlocked",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Middle Text
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = if (unlocked) MaterialTheme.colorScheme.onSurface else Color.Gray,
                    maxLines = 1
                )
                Text(
                    text = item.description,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 12.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Bottom Progress
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { progressRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (unlocked) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (unlocked) "مكتمل ⭐" else "قيد التقدم",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (unlocked) Color(0xFFFFD700) else Color.Gray
                    )
                    Text(
                        text = "${item.progress}/${item.maxProgress}",
                        fontSize = 8.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
