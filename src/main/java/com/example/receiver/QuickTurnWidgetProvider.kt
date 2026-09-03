package com.example.receiver

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.engine.TurnEngine
import com.example.engine.TurnState
import com.example.service.TurnService

class QuickTurnWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, QuickTurnWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.quick_turn_widget)

            val state = TurnEngine.state.value
            val currentUser = TurnEngine.currentUser.value
            val remaining = TurnEngine.remainingSeconds.value
            val elapsed = TurnEngine.elapsedSeconds.value
            val total = TurnEngine.totalDurationSeconds.value

            val targetDuration = TurnEngine.targetDurationSeconds.value
            val isTargetOpen = TurnEngine.isTargetOpenMode.value

            // Content Intent to open app when clicking any non-button element or widget title
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                200,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)

            if (state == TurnState.IDLE) {
                views.setViewVisibility(R.id.widget_idle_container, View.VISIBLE)
                views.setViewVisibility(R.id.widget_active_container, View.GONE)
                views.setTextViewText(R.id.widget_status_badge, "جاهز 🟢")

                val userText = currentUser?.let { "${it.avatarEmoji} ${it.name}" } ?: "لا يوجد لاعب"
                views.setTextViewText(R.id.widget_selected_user_text, userText)

                val timeText = formatSelectedDuration(targetDuration, isTargetOpen)
                views.setTextViewText(R.id.widget_selected_time_text, timeText)

                // Wire service triggers
                val cycleUserIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_WIDGET_CYCLE_USER }
                val pCycleUser = PendingIntent.getService(context, 201, cycleUserIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_select_user, pCycleUser)

                val cycleTimeIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_WIDGET_CYCLE_TIME }
                val pCycleTime = PendingIntent.getService(context, 202, cycleTimeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_select_time, pCycleTime)
                views.setOnClickPendingIntent(R.id.widget_time_center_area, pCycleTime)
                views.setOnClickPendingIntent(R.id.widget_time_up, pCycleTime)

                val cycleTimeBackIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_WIDGET_CYCLE_TIME_BACKWARD }
                val pCycleTimeBack = PendingIntent.getService(context, 207, cycleTimeBackIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_time_down, pCycleTimeBack)

                val startIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_WIDGET_START }
                val pStart = PendingIntent.getService(context, 203, startIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_btn_start, pStart)

            } else {
                views.setViewVisibility(R.id.widget_idle_container, View.GONE)
                views.setViewVisibility(R.id.widget_active_container, View.VISIBLE)

                views.setTextViewText(R.id.widget_status_badge, if (state == TurnState.PAUSED) "مؤقت ⏸️" else "جاري ⚡")

                val userText = currentUser?.let { "${it.avatarEmoji} ${it.name}" } ?: "غير معروف"
                views.setTextViewText(R.id.widget_active_user_text, userText)

                if (state == TurnState.OPEN_MODE) {
                    views.setTextViewText(R.id.widget_timer_text, formatTime(elapsed))
                    views.setProgressBar(R.id.widget_progress_bar, 100, 100, true)
                } else {
                    views.setTextViewText(R.id.widget_timer_text, formatTime(remaining))
                    val progressPercent = if (total > 0) ((total - remaining).toFloat() / total * 100).toInt() else 0
                    views.setProgressBar(R.id.widget_progress_bar, 100, progressPercent, false)
                }

                val pauseBtnText = if (state == TurnState.PAUSED) "استئناف ▶" else "إيقاف ⏸"
                views.setTextViewText(R.id.widget_btn_pause_text, pauseBtnText)

                val pauseAction = if (state == TurnState.PAUSED) TurnService.ACTION_RESUME else TurnService.ACTION_PAUSE
                val pauseIntent = Intent(context, TurnService::class.java).apply { action = pauseAction }
                val pPause = PendingIntent.getService(context, 204, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_btn_pause, pPause)

                val finishIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_FINISH }
                val pFinish = PendingIntent.getService(context, 205, finishIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_btn_finish, pFinish)

                val cancelIntent = Intent(context, TurnService::class.java).apply { action = TurnService.ACTION_CANCEL }
                val pCancel = PendingIntent.getService(context, 206, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                views.setOnClickPendingIntent(R.id.widget_btn_cancel, pCancel)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
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

        private fun formatSelectedDuration(targetDuration: Long, isTargetOpen: Boolean): String {
            if (isTargetOpen) return "وقت مفتوح ♾️"
            val hrs = targetDuration / 3600
            val mins = (targetDuration % 3600) / 60
            val secs = targetDuration % 60

            return when {
                hrs > 0 && mins > 0 && secs > 0 -> "$hrs س و $mins د و $secs ث ⏱️"
                hrs > 0 && mins > 0 -> when {
                    hrs == 1L && mins == 30L -> "ساعة ونصف (1:30س) ⏱️"
                    hrs == 1L && mins == 15L -> "ساعة وربع (1:15س) ⏱️"
                    hrs == 1L && mins == 45L -> "ساعة و 45 د ⏱️"
                    hrs == 1L -> "ساعة و $mins د ⏱️"
                    hrs == 2L && mins == 30L -> "ساعتان ونصف (2:30س) ⏱️"
                    hrs == 2L && mins == 15L -> "ساعتان وربع (2:15س) ⏱️"
                    hrs == 2L && mins == 45L -> "ساعتان و 45 د ⏱️"
                    hrs == 2L -> "ساعتان و $mins د ⏱️"
                    hrs == 3L && mins == 30L -> "3 ساعات ونصف ⏱️"
                    hrs == 3L && mins == 15L -> "3 ساعات وربع ⏱️"
                    hrs == 3L && mins == 45L -> "3 ساعات و 45 د ⏱️"
                    else -> "$hrs س و $mins د ⏱️"
                }
                hrs > 0 -> when (hrs) {
                    1L -> "ساعة واحدة (1س) ⏱️"
                    2L -> "ساعتان (2س) ⏱️"
                    3L -> "3 ساعات (3س) ⏱️"
                    4L -> "4 ساعات (4س) ⏱️"
                    else -> "$hrs ساعات ⏱️"
                }
                mins > 0 && secs > 0 -> "$mins د و $secs ث ⏱️"
                mins > 0 -> when (mins) {
                    1L -> "دقيقة واحدة (1د) ⏱️"
                    2L -> "دقيقتان (2د) ⏱️"
                    3L -> "3 دقائق ⏱️"
                    4L -> "4 دقائق ⏱️"
                    5L -> "5 دقائق ⏱️"
                    10L -> "10 دقائق ⏱️"
                    15L -> "15 دقيقة (ربع ساعة) ⏱️"
                    20L -> "20 دقيقة (ثلث ساعة) ⏱️"
                    25L -> "25 دقيقة ⏱️"
                    30L -> "30 دقيقة (نصف ساعة) ⏱️"
                    45L -> "45 دقيقة (إلا ربع) ⏱️"
                    else -> "$mins دقيقة ⏱️"
                }
                else -> "$secs ثانية ⏱️"
            }
        }
    }
}
