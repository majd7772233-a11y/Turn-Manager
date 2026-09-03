package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.database.DatabaseModule
import com.example.database.UserEntity
import com.example.engine.TurnEngine
import com.example.engine.TurnState
import com.example.service.TurnService
import kotlinx.coroutines.*
import kotlin.random.Random

/**
 * SmartNotificationManager:
 * Intelligently monitors device active usage without battery drain.
 * ONLY fires witty, humorous, and competitive banter notifications when:
 * 1. Screen is ON and actively in use.
 * 2. NO turn is currently running (TurnEngine is IDLE).
 * 3. Never triggers when phone is sleeping/off/unused (e.g. at night).
 */
class SmartNotificationManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null

    private var isScreenInteractive = false
    private var screenOnTimestamp = 0L
    private var activeScreenMinutesWithoutTurn = 0
    private var consecutiveNudgesSent = 0
    private var isReceiverRegistered = false

    companion object {
        private const val TAG = "SmartNotifManager"
        const val SMART_CHANNEL_ID = "smart_turn_nudges_channel"
        const val SMART_NUDGE_NOTIFICATION_ID = 2001
        const val BANTER_NOTIFICATION_ID = 2002

        const val EXTRA_START_RANDOM_TURN = "EXTRA_START_RANDOM_TURN"
        const val EXTRA_TARGET_USER_ID = "EXTRA_TARGET_USER_ID"
        const val EXTRA_RANDOM_DURATION_SEC = "EXTRA_RANDOM_DURATION_SEC"

        // Generates random duration between 1 second and 2 hours (7200 seconds)
        fun generateRandomDurationSeconds(): Long {
            val randomChoice = Random.nextInt(0, 10)
            return when {
                // 30% chance: clean minute intervals (1m to 45m)
                randomChoice in 0..2 -> (1..45).random() * 60L
                // 30% chance: hour intervals (1h to 2h)
                randomChoice in 3..5 -> listOf(3600L, 4500L, 5400L, 6300L, 7200L).random()
                // 20% chance: fun mixed seconds (e.g. 45s, 90s, 3m 30s)
                randomChoice in 6..7 -> listOf(15L, 30L, 45L, 90L, 150L, 210L, 330L).random()
                // 20% chance: completely random second between 1s and 7200s
                else -> Random.nextLong(1L, 7201L)
            }
        }

        fun formatSecondsToDisplay(seconds: Long): String {
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hrs > 0 && mins > 0 -> "$hrs س و $mins د"
                hrs > 0 -> "$hrs ساعة"
                mins > 0 && secs > 0 -> "$mins د و $secs ث"
                mins > 0 -> "$mins دقيقة"
                else -> "$secs ثانية"
            }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    onScreenTurnedOn()
                }
                Intent.ACTION_SCREEN_OFF -> {
                    onScreenTurnedOff()
                }
            }
        }
    }

    fun startMonitoring() {
        createChannel()
        registerScreenReceiver()
        checkInitialScreenState()
        startPeriodicCheckLoop()
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        unregisterScreenReceiver()
    }

    private fun checkInitialScreenState() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive == true
        if (isInteractive) {
            onScreenTurnedOn()
        } else {
            onScreenTurnedOff()
        }
    }

    private fun onScreenTurnedOn() {
        isScreenInteractive = true
        screenOnTimestamp = System.currentTimeMillis()
        Log.d(TAG, "Screen is ON and interactive. Smart monitor tracking started.")
    }

    private fun onScreenTurnedOff() {
        isScreenInteractive = false
        activeScreenMinutesWithoutTurn = 0
        Log.d(TAG, "Screen is OFF. Going into complete silent mode. No notifications.")
        // Clear non-critical banter notifications when phone goes to sleep
        dismissSmartNotifications()
    }

    private fun registerScreenReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            context.registerReceiver(screenReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterScreenReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(screenReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isReceiverRegistered = false
        }
    }

    private fun startPeriodicCheckLoop() {
        monitorJob?.cancel()
        monitorJob = scope.launch {
            while (isActive) {
                // Check every 60 seconds
                delay(60_000L)

                try {
                    // Check if smart notifications are enabled in preferences
                    val repository = DatabaseModule.getRepository(context)
                    val isEnabledPref = repository.getPreferenceValue("pref_smart_notifications_enabled", "true")
                    if (isEnabledPref != "true") {
                        continue
                    }

                    // Strict condition: Screen must be interactive and ON
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isDeviceCurrentlyActive = powerManager?.isInteractive == true && isScreenInteractive

                    if (!isDeviceCurrentlyActive) {
                        // Phone is off / asleep -> Do nothing! Completely silent!
                        continue
                    }

                    val currentState = TurnEngine.state.value
                    if (currentState != TurnState.IDLE) {
                        // Turn is currently running or paused -> Reset idle counter
                        activeScreenMinutesWithoutTurn = 0
                        consecutiveNudgesSent = 0
                        continue
                    }

                    // Increment active screen minutes while in IDLE
                    activeScreenMinutesWithoutTurn++
                    Log.d(TAG, "Active device usage without turn: $activeScreenMinutesWithoutTurn minutes")

                    // Evaluation points
                    when (activeScreenMinutesWithoutTurn) {
                        10 -> {
                            // Level 1 Nudge (10 minutes active without turn)
                            showLevel1FunnyNudge()
                            consecutiveNudgesSent++
                        }
                        20 -> {
                            // Level 2 Nudge (20 minutes active without turn)
                            showLevel2FunnyNudge()
                            consecutiveNudgesSent++
                        }
                        35, 50, 70 -> {
                            // High level Banter & Racing / Competitive Teasing
                            showCompetitiveBanterNotification()
                            consecutiveNudgesSent++
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in smart monitoring loop", e)
                }
            }
        }
    }

    private suspend fun showLevel1FunnyNudge() {
        val users = TurnEngine.usersList
        val currentUser = TurnEngine.currentUser.value ?: users.firstOrNull()
        val userName = currentUser?.name ?: "يا بطل"

        val title = "ليش ما تعمل دور؟ 🤔"
        val body = "المغالطة مش مليحة.. شايفك ماسك الجوال، قوم اعمل دور الآن وسيبك من التمطيط! 🏃‍♂️💨"

        sendSmartNudge(
            title = title,
            message = body,
            notificationId = SMART_NUDGE_NOTIFICATION_ID,
            targetUser = currentUser,
            includeRandomTurnAction = true
        )
    }

    private suspend fun showLevel2FunnyNudge() {
        val users = TurnEngine.usersList
        val currentUser = TurnEngine.currentUser.value ?: users.firstOrNull()
        val userName = currentUser?.name ?: "يا كسلان"

        val title = "مش ناوي تعمل دور صح؟ 🤨"
        val body = "له له له! لسه فاتح الجوال وناسي الدور؟ قوم اعمل دور الآن بلا كسل ولا لف ودوران! ⚡"

        sendSmartNudge(
            title = title,
            message = body,
            notificationId = SMART_NUDGE_NOTIFICATION_ID,
            targetUser = currentUser,
            includeRandomTurnAction = true
        )
    }

    private suspend fun showCompetitiveBanterNotification() {
        val users = TurnEngine.usersList
        if (users.isEmpty()) return

        // Sort users by turns count or activity
        val sortedUsers = users.sortedByDescending { it.turnsCount }
        val topUser = sortedUsers.first()
        val lowestUser = sortedUsers.last()

        val (title, message, targetUser) = if (users.size >= 2 && topUser.id != lowestUser.id) {
            val phrases = listOf(
                Triple(
                    "سباق وتحدي الأدوار! 🔥",
                    "يا ${lowestUser.name}! لا تدع ${topUser.name} يهزمك بالإحصائيات (${topUser.turnsCount} أدوار مقابل ${lowestUser.turnsCount})! ادخل عوّض الآن 🏆",
                    lowestUser
                ),
                Triple(
                    "وين رحت يا ${lowestUser.name}؟ 😎",
                    "${topUser.name} مسيطر على الجلسة ومولّع الأرقام.. ادخل خذ دورك واقلب الطاولة عليه! ⚡",
                    lowestUser
                ),
                Triple(
                    "نداء العمالقة! 👑",
                    "يا ${lowestUser.name}، ${topUser.name} جالس يضحك على أرقامك! اضغط دور عشوائي وفاجئه الآن 🎲",
                    lowestUser
                )
            )
            phrases.random()
        } else {
            val soloPhrases = listOf(
                Triple("الساحة تناديك! 🎯", "الوقت يمشي والعداد واقف.. اعمل دور واكسر أرقامك القياسية السابقة! 🚀", topUser),
                Triple("تحدي النفس والوقت ⏳", "أنت قد التحدي! جرب دور عشوائي سريع وشوف تقدر تنجز فيه ولا لا! 🎲", topUser)
            )
            soloPhrases.random()
        }

        sendSmartNudge(
            title = title,
            message = message,
            notificationId = BANTER_NOTIFICATION_ID,
            targetUser = targetUser,
            includeRandomTurnAction = true
        )
    }

    private fun sendSmartNudge(
        title: String,
        message: String,
        notificationId: Int,
        targetUser: UserEntity?,
        includeRandomTurnAction: Boolean
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open Main App
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pMain = PendingIntent.getActivity(
            context,
            notificationId + 10,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, SMART_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pMain)
            .setColor(Color.parseColor("#9C27B0"))

        // Action 1: "بدء دور ▶️" (Opens app and focuses on starting a turn)
        val startTurnIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("ACTION_FROM_NOTIFICATION", "START_TURN")
            targetUser?.let { putExtra(EXTRA_TARGET_USER_ID, it.id) }
        }
        val pStartTurn = PendingIntent.getActivity(
            context,
            notificationId + 20,
            startTurnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        builder.addAction(android.R.drawable.ic_media_play, "بدء دور ▶️", pStartTurn)

        // Action 2: "دور عشوائي 🎲" (generates duration between 1s and 2 hours for target user)
        if (includeRandomTurnAction) {
            val randomDurationSec = generateRandomDurationSeconds()
            val formattedTime = formatSecondsToDisplay(randomDurationSec)

            val randomTurnIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_START_RANDOM_TURN, true)
                putExtra(EXTRA_RANDOM_DURATION_SEC, randomDurationSec)
                targetUser?.let { putExtra(EXTRA_TARGET_USER_ID, it.id) }
            }
            val pRandomTurn = PendingIntent.getActivity(
                context,
                notificationId + 30,
                randomTurnIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_rotate,
                "دور عشوائي ($formattedTime) 🎲",
                pRandomTurn
            )
        }

        manager.notify(notificationId, builder.build())
    }

    fun dismissSmartNotifications() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(SMART_NUDGE_NOTIFICATION_ID)
        manager.cancel(BANTER_NOTIFICATION_ID)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SMART_CHANNEL_ID,
                "تنبيهات وتحديات ذكية ⚡",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "إشعارات فكاهية وتحديات تنافسية أثناء استخدام الهاتف لتذكيرك بعمل الأدوار"
                enableLights(true)
                lightColor = Color.MAGENTA
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
}
