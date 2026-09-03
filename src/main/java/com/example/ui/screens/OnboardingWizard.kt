package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.viewmodel.UserOnboardingData
import com.example.ui.components.verticalScrollbar
import com.example.ui.components.horizontalScrollbar

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material.icons.filled.VolumeUp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingWizard(
    onComplete: (userList: List<UserOnboardingData>, sessionName: String, sessionType: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    // Step States
    val userList = remember { 
        mutableStateListOf(
            UserOnboardingData("مستخدم 1", "😎", "#FF0055"),
            UserOnboardingData("مستخدم 2", "🤖", "#00E5FF")
        )
    }
    var newUserName by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("😺") }
    var selectedColorHex by remember { mutableStateOf("#39FF14") }
    var selectedSoundUri by remember { mutableStateOf<String?>(null) }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedSoundUri = uri.toString()
        }
    }
    
    var sessionName by remember { mutableStateOf("جلسة اللعب الأولى") }
    var sessionType by remember { mutableStateOf("PLAY") }

    val emojis = listOf("😎", "🤖", "🚀", "😺", "🦊", "🐼", "🦁", "🦖", "👾", "🥑", "👩‍💻", "⚽", "🧙", "🧛", "🦄", "🎯")
    val colors = listOf("#FF0055", "#00E5FF", "#39FF14", "#FFEA00", "#7D26CD", "#FF9100", "#00FF66", "#E100FF", "#00E676", "#FF5252")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("معالج التهيئة للأدوار", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Progress dots
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(3) { index ->
                    val active = index + 1 == step
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) MaterialTheme.colorScheme.primary 
                                else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f)
                            )
                    )
                }
            }

            // Animated step container
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    1 -> {
                        // Welcome screen
                        val welcomeScrollState = rememberScrollState()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(welcomeScrollState)
                                .verticalScrollbar(welcomeScrollState)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "مرحباً بك في تطبيق الأدوار 👋",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "المنصة الاحترافية الأولى لإدارة الوقت وتبادل الأدوار والمهام بين عدة مستخدمين بكل سهولة وعدل.",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("🎯 مميزات التطبيق الأساسية:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("• إدارة الأدوار بدقة متناهية مع مؤقت ذكي دائري.", fontSize = 14.sp)
                                    Text("• إشعار تفاعلي مستمر مع متحكمات الوقت.", fontSize = 14.sp)
                                    Text("• سجل تاريخي متكامل وإحصائيات بيانية رائعة.", fontSize = 14.sp)
                                    Text("• نظام إنجازات وتخصيص ثيمات كامل.", fontSize = 14.sp)
                                }
                            }
                        }
                    }
                    2 -> {
                        // User Setup Screen
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "أضف المشاركين في الأدوار 👥",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Users Quick list
                            Card(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "المستخدمون الحاليون (${userList.size}):",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        if (userList.isEmpty()) {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("لا يوجد مستخدمين مضافين بعد.", color = Color.Gray)
                                            }
                                        } else {
                                            val listState = rememberLazyListState()
                                            androidx.compose.foundation.lazy.LazyColumn(
                                                state = listState,
                                                modifier = Modifier.fillMaxSize().verticalScrollbar(listState)
                                            ) {
                                                items(userList) { onboardingUser ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp)
                                                            .background(
                                                                MaterialTheme.colorScheme.background,
                                                                RoundedCornerShape(8.dp)
                                                            )
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            val uColor = remember(onboardingUser.colorHex) {
                                                                try { Color(android.graphics.Color.parseColor(onboardingUser.colorHex)) } catch (e: Exception) { Color.Gray }
                                                            }
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(32.dp)
                                                                    .clip(CircleShape)
                                                                    .background(uColor.copy(alpha = 0.2f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(onboardingUser.emoji, fontSize = 18.sp)
                                                            }
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(onboardingUser.name, fontWeight = FontWeight.Medium)
                                                            if (onboardingUser.customSoundUri != null) {
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("🎵", fontSize = 12.sp)
                                                            }
                                                        }
                                                        IconButton(
                                                            onClick = { userList.remove(onboardingUser) }
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Avatar/Emoji Picker
                                    val emojiRowState = rememberLazyListState()
                                    LazyRow(
                                        state = emojiRowState,
                                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(emojiRowState),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(emojis) { emoji ->
                                            val selected = selectedEmoji == emoji
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                        else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { selectedEmoji = emoji },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(emoji, fontSize = 20.sp)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Color Picker
                                    val colorRowState = rememberLazyListState()
                                    LazyRow(
                                        state = colorRowState,
                                        modifier = Modifier.fillMaxWidth().horizontalScrollbar(colorRowState),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(colors) { hex ->
                                            val selected = selectedColorHex == hex
                                            val c = Color(android.graphics.Color.parseColor(hex))
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 4.dp)
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(c)
                                                    .border(
                                                        if (selected) 2.dp else 0.dp,
                                                        MaterialTheme.colorScheme.onBackground,
                                                        CircleShape
                                                    )
                                                    .clickable { selectedColorHex = hex }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Sound Picker Button
                                    Button(
                                        onClick = { audioLauncher.launch("audio/*") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (selectedSoundUri != null) MaterialTheme.colorScheme.secondary
                                                             else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (selectedSoundUri != null) "تم اختيار نغمة انتهاء مخصصة 🎵"
                                                   else "نغمة انتهاء الدور المخصصة (اختياري) 🎵",
                                            fontSize = 11.sp,
                                            color = if (selectedSoundUri != null) MaterialTheme.colorScheme.onSecondary
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (selectedSoundUri != null) {
                                        TextButton(onClick = { selectedSoundUri = null }) {
                                            Text("إلغاء النغمة المخصصة 🗑️", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Add Form
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newUserName,
                                            onValueChange = { newUserName = it },
                                            placeholder = { Text("اسم المستخدم") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (newUserName.isNotBlank()) {
                                                    userList.add(UserOnboardingData(newUserName.trim(), selectedEmoji, selectedColorHex, selectedSoundUri))
                                                    newUserName = ""
                                                    selectedSoundUri = null
                                                    // Auto pick next emoji/color
                                                    selectedEmoji = emojis[(userList.size) % emojis.size]
                                                    selectedColorHex = colors[(userList.size) % colors.size]
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Add")
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // Session Setup Screen
                        val sessionScrollState = rememberScrollState()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Top,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(sessionScrollState)
                                .verticalScrollbar(sessionScrollState)
                        ) {
                            Text(
                                text = "تهيئة الجلسة الأولى 🏁",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "يمكنك تنظيم أدوارك داخل جلسات مختلفة مثل اللعب، المذاكرة، أو العمل.",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = sessionName,
                                onValueChange = { sessionName = it },
                                label = { Text("اسم الجلسة") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("نوع الجلسة:", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Session Types Selector Grid
                            val types = listOf(
                                "PLAY" to "🎮 لعب وترفيه",
                                "STUDY" to "📚 دراسة ومذاكرة",
                                "WORK" to "💼 عمل وإنجاز",
                                "MEETING" to "👥 اجتماع ونقاش",
                                "POMODORO" to "⏱️ بومودورو"
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                types.forEach { (typeKey, typeLabel) ->
                                    val selected = sessionType == typeKey
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable { sessionType = typeKey }
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = selected,
                                            onClick = { sessionType = typeKey }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(typeLabel, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    TextButton(
                        onClick = { step-- }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("السابق")
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            if (step == 2 && userList.size < 2) {
                                // Must add at least 2 users
                            } else {
                                step++
                            }
                        } else {
                            // Onboarding completed!
                            onComplete(userList.toList(), sessionName.trim(), sessionType)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    enabled = (step != 2 || userList.size >= 2) && (step != 3 || sessionName.isNotBlank())
                ) {
                    Text(if (step == 3) "بدء التطبيق 🚀" else "التالي")
                    Spacer(modifier = Modifier.width(4.dp))
                    if (step < 3) {
                        Icon(Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
