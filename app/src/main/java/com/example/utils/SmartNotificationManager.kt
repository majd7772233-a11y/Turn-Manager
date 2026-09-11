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
 * Monitors device active usage without battery drain.
 * ONLY fires witty, humorous, competitive banter notifications when:
 * 1. Screen is actively in use (interactive).
 * 2. NO turn is currently running (TurnEngine is IDLE).
 * 3. Exactly after 10 minutes of active phone usage without starting a turn.
 * 4. If no turn is started after first notification, another notification fires every 10 minutes.
 * 5. Automatically dismisses immediately when ANY turn starts.
 */
class SmartNotificationManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null

    private var isScreenInteractive = false
    private var activeScreenMinutesWithoutTurn = 0
    private var isReceiverRegistered = false

    private var lastNudgeTitle: String = ""
    private var lastNudgeMessage: String = ""

    companion object {
        private const val TAG = "SmartNotifManager"
        const val SMART_CHANNEL_ID = "smart_turn_nudges_channel"
        const val SMART_NUDGE_NOTIFICATION_ID = 2001

        const val EXTRA_START_RANDOM_TURN = "EXTRA_START_RANDOM_TURN"
        const val EXTRA_TARGET_USER_ID = "EXTRA_TARGET_USER_ID"
        const val EXTRA_RANDOM_DURATION_SEC = "EXTRA_RANDOM_DURATION_SEC"

        // Generates random duration between 1 second and 1.5 hours (5400 seconds)
        fun generateRandomDurationSeconds(): Long {
            val randomChoice = Random.nextInt(0, 10)
            return when {
                // 10% chance: quick seconds (1s, 15s, 30s, 45s)
                randomChoice == 0 -> listOf(1L, 15L, 30L, 45L).random()
                // 40% chance: common minute intervals (1m, 2m, 3m, 5m, 10m, 15m, 20m, 30m, 45m)
                randomChoice in 1..4 -> listOf(60L, 120L, 180L, 300L, 600L, 900L, 1200L, 1800L, 2700L).random()
                // 30% chance: hour intervals (1h, 1h 15m, 1h 30m)
                randomChoice in 5..7 -> listOf(3600L, 4500L, 5400L).random()
                // 20% chance: random range
                else -> (1..90).random() * 60L
            }
        }

        fun formatSecondsToDisplay(seconds: Long): String {
            return formatDurationText(seconds, false)
        }

        fun formatDurationText(seconds: Long, isOpen: Boolean): String {
            if (isOpen) return "مفتوح ♾️"
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hrs > 0 && mins > 0 -> "$hrs س $mins د"
                hrs > 0 -> "$hrs س"
                mins > 0 && secs > 0 -> "$mins د $secs ث"
                mins > 0 -> "$mins د"
                else -> "$secs ث"
            }
        }
    }

    // Rich library of humorous, witty, challenging, motivational Arabic notification messages
    private val smartNudgeLibrary = listOf(
        Pair("ليش ما تعمل دور؟ 🤔", "المغالطة مش مليحة.. شايفك ماسك الجوال، قوم اعمل دور الآن وسيبك من التمطيط! 🏃‍♂️💨"),
        Pair("مش ناوي تعمل دور صح؟ 🤨", "له له له! لسه فاتح الجوال وناسي الدور؟ قوم اعمل دور الآن بلا كسل ولا لف ودوران! ⚡"),
        Pair("يا حبيبنا الدور واقف! ⏳", "شاشتك شغالة ومولعة والعداد نايم في العسل.. اضغط زر البدء وخلينا نلعب! 🎮"),
        Pair("الكسل ممنوع هنا! 🚫", "الجوال بيدك والوقت يمر.. مين اللي عليه الدور؟ لا تخليه يفلت منك! 🎯"),
        Pair("نداء عاجل للأبطال! 🚨", "كل ثانية تمشي بدون دور تعتبر خسارة فادحة في متعة الجلسة.. ابدأ الآن! 🏆"),
        Pair("وينك يا وحش؟ 🔥", "الدور يناديك بأعلى صوت.. دوس بدء وورّينا سرعتك وإنجازك! 🚀"),
        Pair("تحدي السرعة والتركيز ⏱️", "شايفك مركز بالشاشة.. ليش ما تترجم هذا التركيز في دور أسطوري يحطم الأرقام؟ 💥"),
        Pair("صفارة الإنذار الرياضية 📣", "الحكم جاهز واللاعبين ينتظرون.. لا تعلق الجلسة، ابدأ دورك فورا! ⚽"),
        Pair("الوقت من ذهب! 🥇", "لا تضيع الدقائق.. دور سريع خفيف لطيف يجدد النشاط والحماس! ✨"),
        Pair("سؤال وجيه وصريح 🧐", "ماسك الجوال تتفرج ولا ناوي تبهرنا بدور جديد؟ الزر تحتك اضغطه ولا تحتار! 💡"),
        Pair("غرفة التحكم تناديك 🛰️", "الأنظمة جاهزة للملاحة.. حدد اللاعب والوقت وانطلق برحلة الدور القادمة! 🛸"),
        Pair("يا فنان وين النشاط؟ 🎨", "الأدوار تحتاج لمستك الإبداعية.. اختار وقتك ودوس بدء بدون تردد! 🌟"),
        Pair("العداد في انتظارك 📊", "إحصائياتك محتاجة دفعة قوية.. دور واحد يرفعك لصدارة الترتيب! 📈"),
        Pair("الفرصة لا تتكرر! 🎁", "جرب حظك الآن مع دور عشوائي مفاجئ وشوف التحدي الجديد! 🎲"),
        Pair("تراك طولت يا غالي! ☕", "مرت عشر دقائق وأنت تتصفح.. ريح بالك واعمل دور واستمتع! 🥳"),
        Pair("التاج الملكي لمن؟ 👑", "المنافسة مشتعلة والعرش ينتظر البطل.. ابدأ دورك واثبت وجودك! 🛡️"),
        Pair("سجل بطولاتك يناديك 📜", "كل دور توثقه يظل ذكرى حلوة.. لا تفوت توثيق هذه اللحظة! 💎"),
        Pair("طاقتك وين راحت؟ ⚡", "شعلل الجو وأشعل الحماس بدور فوري.. كلنا بانتظارك! 🦁"),
        Pair("رادار الجلسة كشفك! 📡", "الرادار يؤكد وجودك على الهاتف.. مفيش مفر، الدور عليك! 🎯"),
        Pair("حركة وتفاعل يا كابتن! 🧗", "الدور ما ياخذ ثواني عشان تبدأه.. دوس بدء ولا تأجل! 🏁")
    )

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
        Log.d(TAG, "Screen is ON and interactive. Smart monitor tracking started.")
    }

    private fun onScreenTurnedOff() {
        isScreenInteractive = false
        activeScreenMinutesWithoutTurn = 0
        Log.d(TAG, "Screen is OFF. Going into complete silent mode. No notifications.")
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
                // Check every 60 seconds (1 minute)
                delay(60_000L)

                try {
                    val repository = DatabaseModule.getRepository(context)
                    val isEnabledPref = repository.getPreferenceValue("pref_smart_notifications_enabled", "true")
                    if (isEnabledPref != "true") {
                        continue
                    }

                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val isDeviceCurrentlyActive = powerManager?.isInteractive == true && isScreenInteractive

                    if (!isDeviceCurrentlyActive) {
                        continue
                    }

                    val currentState = TurnEngine.state.value
                    if (currentState != TurnState.IDLE) {
                        // Turn is currently running or paused -> Reset idle counter and dismiss
                        activeScreenMinutesWithoutTurn = 0
                        dismissSmartNotifications()
                        continue
                    }

                    // Increment active screen minutes while in IDLE
                    activeScreenMinutesWithoutTurn++
                    Log.d(TAG, "Active device usage without turn: $activeScreenMinutesWithoutTurn minutes")

                    // Show notification every 10 minutes (10, 20, 30, 40, 50, 60...)
                    if (activeScreenMinutesWithoutTurn > 0 && activeScreenMinutesWithoutTurn % 10 == 0) {
                        val nudge = smartNudgeLibrary.random()
                        lastNudgeTitle = nudge.first
                        lastNudgeMessage = nudge.second
                        sendSmartNudgeNotification(lastNudgeTitle, lastNudgeMessage)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in smart monitoring loop", e)
                }
            }
        }
    }

    fun refreshCurrentNudge() {
        if (lastNudgeTitle.isEmpty()) {
            val nudge = smartNudgeLibrary.first()
            lastNudgeTitle = nudge.first
            lastNudgeMessage = nudge.second
        }
        sendSmartNudgeNotification(lastNudgeTitle, lastNudgeMessage)
    }

    fun sendSmartNudgeNotification(
        title: String,
        message: String
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open Main App on notification body click
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pMain = PendingIntent.getActivity(
            context,
            SMART_NUDGE_NOTIFICATION_ID + 10,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val currentUser = TurnEngine.currentUser.value
        val userName = currentUser?.name ?: "اللاعب"
        val userEmoji = currentUser?.avatarEmoji ?: "👤"
        val targetDurationSec = TurnEngine.targetDurationSeconds.value
        val isTargetOpen = TurnEngine.isTargetOpenMode.value
        val durationStr = formatDurationText(targetDurationSec, isTargetOpen)

        // Action 1: Switch / Cycle User Name ("اللاعب: [اسم المستخدم]")
        val cycleUserIntent = Intent(context, TurnService::class.java).apply {
            action = TurnService.ACTION_SMART_CYCLE_USER
        }
        val pCycleUser = PendingIntent.getService(
            context,
            SMART_NUDGE_NOTIFICATION_ID + 20,
            cycleUserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Switch / Cycle Duration ("المدة: [الوقت]")
        val cycleTimeIntent = Intent(context, TurnService::class.java).apply {
            action = TurnService.ACTION_SMART_CYCLE_TIME
        }
        val pCycleTime = PendingIntent.getService(
            context,
            SMART_NUDGE_NOTIFICATION_ID + 21,
            cycleTimeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Start Turn ("بدء الدور ▶️")
        val startTurnIntent = Intent(context, TurnService::class.java).apply {
            action = TurnService.ACTION_SMART_START_TURN
        }
        val pStartTurn = PendingIntent.getService(
            context,
            SMART_NUDGE_NOTIFICATION_ID + 22,
            startTurnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 4: Start Random Turn ("دور عشوائي 🎲")
        val randomTurnIntent = Intent(context, TurnService::class.java).apply {
            action = TurnService.ACTION_SMART_START_RANDOM
        }
        val pRandomTurn = PendingIntent.getService(
            context,
            SMART_NUDGE_NOTIFICATION_ID + 23,
            randomTurnIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, SMART_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\n📌 اللاعب الحالي: $userEmoji $userName | المدة: $durationStr"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pMain)
            .setColor(Color.parseColor("#9C27B0"))
            .addAction(android.R.drawable.ic_menu_myplaces, "اللاعب: $userName 🔄", pCycleUser)
            .addAction(android.R.drawable.ic_menu_recent_history, "المدة: $durationStr ⏱️", pCycleTime)
            .addAction(android.R.drawable.ic_media_play, "بدء الدور ▶️", pStartTurn)
            .addAction(android.R.drawable.ic_menu_rotate, "دور عشوائي 🎲", pRandomTurn)

        manager.notify(SMART_NUDGE_NOTIFICATION_ID, builder.build())
    }

    fun dismissSmartNotifications() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(SMART_NUDGE_NOTIFICATION_ID)
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
