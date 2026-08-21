package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Launch
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.TurnManagerTheme

class TurnEndActivity : ComponentActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set flags to show above lock screen and turn screen on
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            )
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Retrieve extras
        val userName = intent.getStringExtra("userName") ?: "المشارك التالي"
        val userEmoji = intent.getStringExtra("userEmoji") ?: "⏱️"
        val userColorHex = intent.getStringExtra("userColorHex") ?: "#FF0055"
        val customSoundUri = intent.getStringExtra("customSoundUri")
        val elapsedSeconds = intent.getLongExtra("elapsedSeconds", 0L)

        val uColor = try {
            ComposeColor(android.graphics.Color.parseColor(userColorHex))
        } catch (e: Exception) {
            ComposeColor(0xFFFF0055)
        }

        // Start sound
        startAlarm(customSoundUri)

        // Start vibration
        startVibration()

        setContent {
            TurnManagerTheme {
                TurnEndOverlayContent(
                    userName = userName,
                    userEmoji = userEmoji,
                    userColor = uColor,
                    elapsedSeconds = elapsedSeconds,
                    onDone = {
                        stopAllAndFinish()
                    },
                    onOpenApp = {
                        openMainActivity()
                    }
                )
            }
        }
    }

    private fun startAlarm(soundUriStr: String?) {
        mediaPlayer = MediaPlayer()
        try {
            if (!soundUriStr.isNullOrEmpty()) {
                mediaPlayer?.setDataSource(this, Uri.parse(soundUriStr))
            } else {
                val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer?.setDataSource(this, alertUri)
            }
            mediaPlayer?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            mediaPlayer?.isLooping = true
            mediaPlayer?.prepare()
            mediaPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback try simple alert
            try {
                mediaPlayer?.reset()
                val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                mediaPlayer?.setDataSource(this, alertUri)
                mediaPlayer?.prepare()
                mediaPlayer?.start()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startVibration() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(pattern, 0)
            }
        }
    }

    private fun openMainActivity() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(launchIntent)
        stopAllAndFinish()
    }

    private fun stopAllAndFinish() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            notificationManager.cancel(1002)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onDestroy() {
        stopAllAndFinish()
        super.onDestroy()
    }
}

@Composable
fun TurnEndOverlayContent(
    userName: String,
    userEmoji: String,
    userColor: ComposeColor,
    elapsedSeconds: Long,
    onDone: () -> Unit,
    onOpenApp: () -> Unit
) {
    // Pulse scale animation for visual richness
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val mins = elapsedSeconds / 60
    val secs = elapsedSeconds % 60
    val durationText = if (mins > 0) "${mins} دقيقة و ${secs} ثانية" else "${secs} ثانية"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ComposeColor.Black.copy(alpha = 0.5f)), // Translucent container
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp)
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(userColor, ComposeColor(0xFFFFD700))
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ComposeColor(0xFA1E1E1E)) // Translucent dark slate card
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(userColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = userColor,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(pulseScale)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "انتهى دورك! 🔔",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ComposeColor.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Avatar Container with pulsing animation
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(userColor.copy(alpha = 0.15f))
                        .border(3.dp, userColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = userEmoji, fontSize = 52.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = userName,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = userColor,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Turn Info Panel
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(ComposeColor.White.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "مدة الدور المستغرقة:",
                            fontSize = 12.sp,
                            color = ComposeColor.Gray
                        )
                        Text(
                            text = durationText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = ComposeColor.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Large Done Button
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(1.dp, ComposeColor.White.copy(alpha = 0.1f), RoundedCornerShape(28.dp)),
                    colors = ButtonDefaults.buttonColors(containerColor = userColor),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ComposeColor.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تم، إنهاء الدور 🤝",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ComposeColor.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Open App Button
                TextButton(
                    onClick = onOpenApp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Launch, contentDescription = null, tint = ComposeColor.Gray)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "فتح التطبيق بالكامل 🚀",
                        fontSize = 14.sp,
                        color = ComposeColor.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
