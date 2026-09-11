package com.example.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.TurnEndActivity
import com.example.database.DatabaseModule
import com.example.database.UserEntity
import com.example.engine.TurnEngine
import com.example.engine.TurnEvent
import com.example.engine.TurnState
import com.example.utils.AudioEngine
import com.example.receiver.QuickTurnWidgetProvider
import com.example.utils.SmartNotificationManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.combine

class TurnService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isServiceRunning = false
    private var smartNotificationManager: SmartNotificationManager? = null
    private var isNotificationTemporarilyHidden = false
    private var wasAutoPausedByScreenOff = false
    private var isScreenReceiverRegistered = false

    companion object {
        const val CHANNEL_ID = "turn_manager_channel"
        const val NOTIFICATION_ID = 1001

        // Action Strings
        const val ACTION_START_SERVICE = "com.example.action.START_SERVICE"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_SERVICE"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_RESUME = "com.example.action.RESUME"
        const val ACTION_CANCEL = "com.example.action.CANCEL"
        const val ACTION_FINISH = "com.example.action.FINISH"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_TEMP_HIDE_NOTIFICATION = "com.example.action.TEMP_HIDE_NOTIFICATION"
        const val ACTION_RESTORE_NOTIFICATION = "com.example.action.RESTORE_NOTIFICATION"
        const val ACTION_APP_FOREGROUND = "com.example.action.APP_FOREGROUND"
        const val ACTION_APP_BACKGROUND = "com.example.action.APP_BACKGROUND"

        // Smart Notification Interactive Action Strings
        const val ACTION_SMART_CYCLE_USER = "com.example.action.SMART_CYCLE_USER"
        const val ACTION_SMART_CYCLE_TIME = "com.example.action.SMART_CYCLE_TIME"
        const val ACTION_SMART_START_TURN = "com.example.action.SMART_START_TURN"
        const val ACTION_SMART_START_RANDOM = "com.example.action.SMART_START_RANDOM"

        @Volatile
        var isAppInForeground: Boolean = false

        // Widget-specific action strings (also used for interactive notification)
        const val ACTION_WIDGET_START = "com.example.action.WIDGET_START"
        const val ACTION_WIDGET_CYCLE_USER = "com.example.action.WIDGET_CYCLE_USER"
        const val ACTION_WIDGET_CYCLE_TIME = "com.example.action.WIDGET_CYCLE_TIME"
        const val ACTION_WIDGET_CYCLE_TIME_BACKWARD = "com.example.action.WIDGET_CYCLE_TIME_BACKWARD"

        val TIME_CYCLE_LIST = listOf(
            Pair(60L, false),     // 1 دقيقة
            Pair(120L, false),    // دقيقتان
            Pair(180L, false),    // 3 دقائق
            Pair(240L, false),    // 4 دقائق
            Pair(300L, false),    // 5 دقائق
            Pair(360L, false),    // 6 دقائق
            Pair(420L, false),    // 7 دقائق
            Pair(480L, false),    // 8 دقائق
            Pair(540L, false),    // 9 دقائق
            Pair(600L, false),    // 10 دقائق
            Pair(720L, false),    // 12 دقيقة
            Pair(900L, false),    // 15 دقيقة (ربع ساعة)
            Pair(1200L, false),   // 20 دقيقة (ثلث ساعة)
            Pair(1500L, false),   // 25 دقيقة
            Pair(1800L, false),   // 30 دقيقة (نصف ساعة)
            Pair(2100L, false),   // 35 دقيقة
            Pair(2400L, false),   // 40 دقيقة
            Pair(2700L, false),   // 45 دقيقة (إلا ربع)
            Pair(3000L, false),   // 50 دقيقة
            Pair(3300L, false),   // 55 دقيقة
            Pair(3600L, false),   // 1 ساعة
            Pair(4500L, false),   // ساعة وربع
            Pair(5400L, false),   // ساعة ونصف
            Pair(6300L, false),   // ساعة و 45 دقيقة
            Pair(7200L, false),   // ساعتان
            Pair(8100L, false),   // ساعتان وربع
            Pair(9000L, false),   // ساعتان ونصف
            Pair(9900L, false),   // ساعتان و 45 دقيقة
            Pair(10800L, false),  // 3 ساعات
            Pair(11700L, false),  // 3 ساعات وربع
            Pair(12600L, false),  // 3 ساعات ونصف
            Pair(13500L, false),  // 3 ساعات و 45 دقيقة
            Pair(14400L, false),  // 4 ساعات (الحد الأقصى)
            Pair(0L, true)        // وقت مفتوح
        )
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private val screenPowerReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    val currentState = TurnEngine.state.value
                    if (currentState == TurnState.RUNNING || currentState == TurnState.OPEN_MODE) {
                        wasAutoPausedByScreenOff = true
                        TurnEngine.pauseTurn()
                    }
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    if (wasAutoPausedByScreenOff && TurnEngine.state.value == TurnState.PAUSED) {
                        wasAutoPausedByScreenOff = false
                        TurnEngine.resumeTurn()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeEngine()
        registerScreenPowerReceiver()
        
        // Initialize Smart Humorous Notification Monitor
        smartNotificationManager = SmartNotificationManager(this).apply {
            startMonitoring()
        }
    }

    private fun registerScreenPowerReceiver() {
        if (!isScreenReceiverRegistered) {
            val filter = android.content.IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            registerReceiver(screenPowerReceiver, filter)
            isScreenReceiverRegistered = true
        }
    }

    private fun unregisterScreenPowerReceiver() {
        if (isScreenReceiverRegistered) {
            try {
                unregisterReceiver(screenPowerReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isScreenReceiverRegistered = false
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_SERVICE -> {
                isNotificationTemporarilyHidden = false
                if (!isServiceRunning) {
                    startForegroundWithNotification()
                    isServiceRunning = true
                }
            }
            ACTION_APP_FOREGROUND -> {
                isAppInForeground = true
                isNotificationTemporarilyHidden = false
                refreshNotificationDisplay()
            }
            ACTION_APP_BACKGROUND -> {
                isAppInForeground = false
                refreshNotificationDisplay()
            }
            ACTION_RESTORE_NOTIFICATION -> {
                isNotificationTemporarilyHidden = false
                refreshNotificationDisplay()
            }
            ACTION_TEMP_HIDE_NOTIFICATION -> {
                isNotificationTemporarilyHidden = true
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                manager.cancel(NOTIFICATION_ID)
            }
            ACTION_STOP_SERVICE -> {
                stopSelf()
            }
            ACTION_PAUSE -> {
                wasAutoPausedByScreenOff = false
                TurnEngine.pauseTurn()
            }
            ACTION_RESUME -> {
                wasAutoPausedByScreenOff = false
                TurnEngine.resumeTurn()
            }
            ACTION_CANCEL -> {
                wasAutoPausedByScreenOff = false
                TurnEngine.cancelTurn()
            }
            ACTION_FINISH -> {
                wasAutoPausedByScreenOff = false
                TurnEngine.finishTurn(saveRecord = true)
            }
            ACTION_NEXT -> {
                wasAutoPausedByScreenOff = false
                TurnEngine.rotateToNextUser()
            }
            ACTION_WIDGET_START -> {
                wasAutoPausedByScreenOff = false
                isNotificationTemporarilyHidden = false
                val targetDuration = TurnEngine.targetDurationSeconds.value
                val isTargetOpen = TurnEngine.isTargetOpenMode.value
                TurnEngine.startTurn(targetDuration, isTargetOpen)
                smartNotificationManager?.dismissSmartNotifications()
                QuickTurnWidgetProvider.updateAllWidgets(this)
            }
            ACTION_WIDGET_CYCLE_USER -> {
                val users = TurnEngine.usersList
                if (users.isNotEmpty()) {
                    val current = TurnEngine.currentUser.value
                    val currentIndex = users.indexOfFirst { it.id == current?.id }
                    val nextIndex = (currentIndex + 1) % users.size
                    TurnEngine.selectUser(users[nextIndex].id)
                }
                QuickTurnWidgetProvider.updateAllWidgets(this)
            }
            ACTION_WIDGET_CYCLE_TIME -> {
                val currentSecs = TurnEngine.targetDurationSeconds.value
                val isOpen = TurnEngine.isTargetOpenMode.value
                val currentIndex = TIME_CYCLE_LIST.indexOfFirst { it.first == currentSecs && it.second == isOpen }
                val nextIndex = if (currentIndex != -1) (currentIndex + 1) % TIME_CYCLE_LIST.size else 0
                val (nextSecs, nextOpen) = TIME_CYCLE_LIST[nextIndex]
                TurnEngine.setTargetDuration(nextSecs, nextOpen)
                QuickTurnWidgetProvider.updateAllWidgets(this)

                val user = TurnEngine.currentUser.value
                if (user != null) {
                    serviceScope.launch(Dispatchers.IO) {
                        DatabaseModule.getRepository(this@TurnService).onNotificationActionUsed(user.id)
                    }
                }
            }
            ACTION_WIDGET_CYCLE_TIME_BACKWARD -> {
                val currentSecs = TurnEngine.targetDurationSeconds.value
                val isOpen = TurnEngine.isTargetOpenMode.value
                val currentIndex = TIME_CYCLE_LIST.indexOfFirst { it.first == currentSecs && it.second == isOpen }
                val prevIndex = if (currentIndex > 0) currentIndex - 1 else TIME_CYCLE_LIST.size - 1
                val (prevSecs, prevOpen) = TIME_CYCLE_LIST[prevIndex]
                TurnEngine.setTargetDuration(prevSecs, prevOpen)
                QuickTurnWidgetProvider.updateAllWidgets(this)
            }
            ACTION_SMART_CYCLE_USER -> {
                val users = TurnEngine.usersList
                if (users.isNotEmpty()) {
                    val current = TurnEngine.currentUser.value
                    val currentIndex = users.indexOfFirst { it.id == current?.id }
                    val nextIndex = (currentIndex + 1) % users.size
                    TurnEngine.selectUser(users[nextIndex].id)
                }
                QuickTurnWidgetProvider.updateAllWidgets(this)
                smartNotificationManager?.refreshCurrentNudge()
            }
            ACTION_SMART_CYCLE_TIME -> {
                val currentSecs = TurnEngine.targetDurationSeconds.value
                val isOpen = TurnEngine.isTargetOpenMode.value
                val currentIndex = TIME_CYCLE_LIST.indexOfFirst { it.first == currentSecs && it.second == isOpen }
                val nextIndex = if (currentIndex != -1) (currentIndex + 1) % TIME_CYCLE_LIST.size else 0
                val (nextSecs, nextOpen) = TIME_CYCLE_LIST[nextIndex]
                TurnEngine.setTargetDuration(nextSecs, nextOpen)
                QuickTurnWidgetProvider.updateAllWidgets(this)
                smartNotificationManager?.refreshCurrentNudge()
            }
            ACTION_SMART_START_TURN -> {
                wasAutoPausedByScreenOff = false
                isNotificationTemporarilyHidden = false
                val targetDuration = TurnEngine.targetDurationSeconds.value
                val isTargetOpen = TurnEngine.isTargetOpenMode.value
                TurnEngine.startTurn(targetDuration, isTargetOpen)
                smartNotificationManager?.dismissSmartNotifications()
                QuickTurnWidgetProvider.updateAllWidgets(this)
            }
            ACTION_SMART_START_RANDOM -> {
                wasAutoPausedByScreenOff = false
                isNotificationTemporarilyHidden = false
                val randomDuration = SmartNotificationManager.generateRandomDurationSeconds()
                TurnEngine.setTargetDuration(randomDuration, isOpenMode = false)
                TurnEngine.startTurn(randomDuration, isOpenMode = false)
                smartNotificationManager?.dismissSmartNotifications()
                QuickTurnWidgetProvider.updateAllWidgets(this)
            }
        }
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification(
            "مستعد للبدء", 
            "اضغط لبدء الدور", 
            TurnState.IDLE, 
            null, 
            300L, 
            false
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun observeEngine() {
        // Collect turn events
        serviceScope.launch {
            TurnEngine.events.collect { event ->
                when (event) {
                    is TurnEvent.Started -> {
                        AudioEngine.playSound("START")
                        AudioEngine.vibrate(this@TurnService, "START")
                    }
                    is TurnEvent.Paused -> {
                        AudioEngine.playSound("PAUSE")
                        AudioEngine.vibrate(this@TurnService, "PAUSE")
                    }
                    is TurnEvent.Resumed -> {
                        AudioEngine.playSound("RESUME")
                        AudioEngine.vibrate(this@TurnService, "START")
                    }
                    is TurnEvent.WarningTick -> {
                        AudioEngine.playSound("WARNING")
                        AudioEngine.vibrate(this@TurnService, "WARNING")
                    }
                    is TurnEvent.Canceled -> {
                        AudioEngine.playSound("CANCELED")
                        AudioEngine.vibrate(this@TurnService, "PAUSE")
                    }
                    is TurnEvent.TimeExpired -> {
                        AudioEngine.playSound("FINISHED")
                        AudioEngine.vibrate(this@TurnService, "FINISHED")
                        
                        // Launch full-screen overlay/alarm activity ONLY when countdown actually expires
                        val overlayIntent = Intent(this@TurnService, TurnEndActivity::class.java).apply {
                            putExtra("userName", event.user.name)
                            putExtra("userEmoji", event.user.avatarEmoji)
                            putExtra("userColorHex", event.user.colorHex)
                            putExtra("customSoundUri", event.user.customSoundUri)
                            putExtra("elapsedSeconds", event.elapsedSeconds)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        }
                        try {
                            startActivity(overlayIntent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        // Trigger high priority full screen overlay notification to bypass Android background activity limits
                        showFullScreenAlarmNotification(event.user, event.elapsedSeconds, overlayIntent)

                        // Save history in background context safely
                        launch(Dispatchers.IO) {
                            val repository = DatabaseModule.getRepository(this@TurnService)
                            val session = TurnEngine.currentSession
                            if (session != null) {
                                repository.insertHistoryRecord(
                                    sessionId = session.id,
                                    userId = event.user.id,
                                    userName = event.user.name,
                                    userColorHex = event.user.colorHex,
                                    actionType = "FINISH",
                                    elapsedSeconds = event.elapsedSeconds
                                )
                            }
                        }
                    }
                    is TurnEvent.ManuallySaved -> {
                        // Gentle sound and vibration, NO full-screen alarm, NO loud finished alert
                        AudioEngine.playSound("START")
                        AudioEngine.vibrate(this@TurnService, "START")

                        // Show friendly Toast on UI thread
                        launch(Dispatchers.Main) {
                            android.widget.Toast.makeText(
                                this@TurnService,
                                "تم إنهاء وحفظ دور ${event.user.name} بنجاح ✨",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }

                        // Save history in background context safely
                        launch(Dispatchers.IO) {
                            val repository = DatabaseModule.getRepository(this@TurnService)
                            val session = TurnEngine.currentSession
                            if (session != null) {
                                repository.insertHistoryRecord(
                                    sessionId = session.id,
                                    userId = event.user.id,
                                    userName = event.user.name,
                                    userColorHex = event.user.colorHex,
                                    actionType = "FINISH",
                                    elapsedSeconds = event.elapsedSeconds
                                )
                            }
                        }
                    }
                    is TurnEvent.UserChanged -> {
                        AudioEngine.playSound("START")
                    }
                    is TurnEvent.SessionEnded -> {
                        AudioEngine.playSound("CANCELED")
                    }
                }
            }
        }

        // Combine engine state changes to update Notification
        serviceScope.launch {
            combine(
                TurnEngine.state,
                TurnEngine.remainingSeconds,
                TurnEngine.elapsedSeconds,
                TurnEngine.currentUser,
                TurnEngine.targetDurationSeconds,
                TurnEngine.isTargetOpenMode
            ) { array ->
                val state = array[0] as TurnState
                val remaining = array[1] as Long
                val elapsed = array[2] as Long
                val user = array[3] as UserEntity?
                val targetDuration = array[4] as Long
                val isTargetOpen = array[5] as Boolean

                val title = when (state) {
                    TurnState.IDLE -> "الدور التالي: ${user?.name ?: "غير محدد"}"
                    TurnState.RUNNING -> "الدور الحالي: ${user?.name ?: "غير محدد"}"
                    TurnState.OPEN_MODE -> "الدور الحالي (مفتوح): ${user?.name ?: "غير محدد"}"
                    TurnState.PAUSED -> "موقف مؤقتاً: ${user?.name ?: "غير محدد"}"
                    TurnState.FINISHED -> "انتهى دور: ${user?.name ?: "غير محدد"}"
                }

                val desc = when (state) {
                    TurnState.IDLE -> "بانتظار البدء..."
                    TurnState.RUNNING -> "المتبقي: ${formatTime(remaining)}"
                    TurnState.OPEN_MODE -> "الوقت المنقضي: ${formatTime(elapsed)}"
                    TurnState.PAUSED -> "الوقت المتبقي: ${formatTime(remaining)}"
                    TurnState.FINISHED -> "تم حفظ دور ${user?.name}"
                }

                buildNotification(title, desc, state, user, targetDuration, isTargetOpen)
            }.collect { notification ->
                if (!isNotificationTemporarilyHidden) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startForeground(
                            NOTIFICATION_ID,
                            notification,
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                        )
                    } else {
                        startForeground(NOTIFICATION_ID, notification)
                    }
                    manager.notify(NOTIFICATION_ID, notification)
                }

                // Trigger App Widget update automatically on every change
                try {
                    QuickTurnWidgetProvider.updateAllWidgets(this@TurnService)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun refreshNotificationDisplay() {
        val state = TurnEngine.state.value
        val user = TurnEngine.currentUser.value
        val remaining = TurnEngine.remainingSeconds.value
        val elapsed = TurnEngine.elapsedSeconds.value
        val targetDuration = TurnEngine.targetDurationSeconds.value
        val isTargetOpen = TurnEngine.isTargetOpenMode.value

        val title = when (state) {
            TurnState.IDLE -> "الدور التالي: ${user?.name ?: "غير محدد"}"
            TurnState.RUNNING -> "الدور الحالي: ${user?.name ?: "غير محدد"}"
            TurnState.OPEN_MODE -> "الدور الحالي (مفتوح): ${user?.name ?: "غير محدد"}"
            TurnState.PAUSED -> "موقف مؤقتاً: ${user?.name ?: "غير محدد"}"
            TurnState.FINISHED -> "انتهى دور: ${user?.name ?: "غير محدد"}"
        }

        val desc = when (state) {
            TurnState.IDLE -> "بانتظار البدء..."
            TurnState.RUNNING -> "المتبقي: ${formatTime(remaining)}"
            TurnState.OPEN_MODE -> "الوقت المنقضي: ${formatTime(elapsed)}"
            TurnState.PAUSED -> "الوقت المتبقي: ${formatTime(remaining)}"
            TurnState.FINISHED -> "تم حفظ دور ${user?.name}"
        }

        val notification = buildNotification(title, desc, state, user, targetDuration, isTargetOpen)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showFullScreenAlarmNotification(user: UserEntity, elapsedSeconds: Long, overlayIntent: Intent) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val alarmChannelId = "turn_manager_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alarmChannel = NotificationChannel(
                alarmChannelId,
                "منبه انتهاء الدور",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "قناة إشعارات منبه انتهاء دور اللاعب"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(alarmChannel)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            102,
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, alarmChannelId)
            .setContentTitle("انتهى دور ${user.name}! ⏱️")
            .setContentText("اضغط لإنهاء الدور وتدوير اللاعبين")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)

        manager.notify(1002, builder.build())
    }

    private fun buildNotification(
        title: String, 
        content: String, 
        state: TurnState,
        currentUser: UserEntity?,
        targetDuration: Long,
        isTargetOpen: Boolean
    ): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val isRunningState = state == TurnState.RUNNING || state == TurnState.OPEN_MODE || state == TurnState.PAUSED
        val isOngoing = isRunningState || isAppInForeground

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_media_play) // Standard android icon to guarantee compilation
            .setContentIntent(pendingIntent)
            .setOngoing(isOngoing)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        // Actions
        val pauseIntent = Intent(this, TurnService::class.java).apply { action = ACTION_PAUSE }
        val resumeIntent = Intent(this, TurnService::class.java).apply { action = ACTION_RESUME }
        val cancelIntent = Intent(this, TurnService::class.java).apply { action = ACTION_CANCEL }
        val finishIntent = Intent(this, TurnService::class.java).apply { action = ACTION_FINISH }
        
        val startWidgetIntent = Intent(this, TurnService::class.java).apply { action = ACTION_WIDGET_START }
        val cycleUserWidgetIntent = Intent(this, TurnService::class.java).apply { action = ACTION_WIDGET_CYCLE_USER }
        val cycleTimeWidgetIntent = Intent(this, TurnService::class.java).apply { action = ACTION_WIDGET_CYCLE_TIME }

        val pPause = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pResume = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pCancel = PendingIntent.getService(this, 3, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pFinish = PendingIntent.getService(this, 4, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pStartWidget = PendingIntent.getService(this, 5, startWidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pCycleUser = PendingIntent.getService(this, 6, cycleUserWidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pCycleTime = PendingIntent.getService(this, 7, cycleTimeWidgetIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val hideIntent = Intent(this, TurnService::class.java).apply { action = ACTION_TEMP_HIDE_NOTIFICATION }
        val pHide = PendingIntent.getService(this, 8, hideIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        // If not in foreground and not running, allow swipe-to-dismiss
        if (!isOngoing) {
            builder.setDeleteIntent(pHide)
        }

        when (state) {
            TurnState.RUNNING, TurnState.OPEN_MODE -> {
                builder.addAction(android.R.drawable.ic_media_pause, "إيقاف مؤقت", pPause)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", pCancel)
                builder.addAction(android.R.drawable.ic_menu_save, "إنهاء وحفظ", pFinish)
            }
            TurnState.PAUSED -> {
                builder.addAction(android.R.drawable.ic_media_play, "استئناف", pResume)
                builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "إلغاء", pCancel)
                builder.addAction(android.R.drawable.ic_menu_save, "إنهاء وحفظ", pFinish)
            }
            TurnState.IDLE, TurnState.FINISHED -> {
                val userLabel = currentUser?.let { "${it.avatarEmoji} ${it.name}" } ?: "محدد"
                val timeLabel = if (isTargetOpen) {
                    "مفتوح ♾️"
                } else {
                    val hrs = targetDuration / 3600
                    val mins = (targetDuration % 3600) / 60
                    val secs = targetDuration % 60
                    if (hrs > 0 && mins > 0) "${hrs}س ${mins}د"
                    else if (hrs > 0) "${hrs} ساعة"
                    else if (mins > 0) "${mins} دقيقة"
                    else "${secs} ثانية"
                }
                builder.addAction(android.R.drawable.ic_menu_my_calendar, "اللاعب: $userLabel", pCycleUser)
                builder.addAction(android.R.drawable.ic_menu_recent_history, "الوقت: $timeLabel", pCycleTime)
                builder.addAction(android.R.drawable.ic_media_play, "بدء ▶️", pStartWidget)

                // Show X / Hide action ONLY when outside the app
                if (!isAppInForeground) {
                    builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "✖ إخفاء", pHide)
                }
            }
        }

        return builder.build()
    }

    private fun formatTime(totalSec: Long): String {
        val hrs = totalSec / 3600
        val mins = (totalSec % 3600) / 60
        val secs = totalSec % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "إدارة الوقت للأدوار",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "قناة إشعارات تطبيق الأدوار لتحديث الوقت الحالي والمتحكمات"
                setShowBadge(false)
                setSound(null, null)
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        unregisterScreenPowerReceiver()
        smartNotificationManager?.stopMonitoring()
        smartNotificationManager = null
        serviceScope.cancel()
    }
}
