package com.example.repository

import com.example.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.*

class TurnRepository(private val turnDao: TurnDao) {

    // Users
    val allUsers: Flow<List<UserEntity>> = turnDao.getAllUsersFlow()

    suspend fun getUser(id: Int): UserEntity? = turnDao.getUserById(id)

    suspend fun insertUser(user: UserEntity): Long {
        val id = turnDao.insertUser(user)
        initializeAchievementsForUser(id.toInt())
        return id
    }

    suspend fun updateUser(user: UserEntity) = turnDao.updateUser(user)

    suspend fun deleteUser(user: UserEntity) = turnDao.deleteUser(user)

    // Sessions
    val allSessions: Flow<List<SessionEntity>> = turnDao.getAllSessionsFlow()

    suspend fun insertSession(session: SessionEntity): Long {
        val id = turnDao.insertSession(session)
        val sessions = turnDao.getAllSessions()
        val users = turnDao.getAllUsers()
        for (user in users) {
            onSessionCreated(user.id, sessions.size)
        }
        return id
    }

    suspend fun deleteSession(sessionId: Int) = turnDao.deleteSessionById(sessionId)

    suspend fun updateSession(session: SessionEntity) = turnDao.updateSession(session)

    // History
    val allHistory: Flow<List<HistoryEntity>> = turnDao.getAllHistoryFlow()

    fun getHistoryForSession(sessionId: Int): Flow<List<HistoryEntity>> =
        turnDao.getHistoryBySessionFlow(sessionId)

    // Move single item to trash (Secret Deleted History)
    suspend fun moveHistoryItemToTrash(historyId: Int) {
        val item = turnDao.getHistoryById(historyId) ?: return
        val sessions = turnDao.getAllSessions()
        val session = sessions.find { it.id == item.sessionId }
        val deletedItem = DeletedHistoryEntity(
            originalId = item.id,
            sessionId = item.sessionId,
            userId = item.userId,
            userName = item.userName,
            userColorHex = item.userColorHex,
            actionType = item.actionType,
            originalTimestamp = item.timestamp,
            elapsedSeconds = item.elapsedSeconds,
            deletedAt = System.currentTimeMillis(),
            deletionType = "SINGLE",
            bulkBatchId = null,
            sessionName = session?.name ?: "جلسة غير محددة"
        )
        turnDao.insertDeletedHistory(deletedItem)
        turnDao.deleteHistoryById(historyId)
    }

    // Move entire session history to trash as a grouped batch card
    suspend fun moveSessionHistoryToTrash(sessionId: Int) {
        val items = turnDao.getHistoryBySession(sessionId)
        if (items.isEmpty()) return

        val sessions = turnDao.getAllSessions()
        val session = sessions.find { it.id == sessionId }
        val sessionName = session?.name ?: "جلسة غير محددة"
        val batchId = "batch_${System.currentTimeMillis()}_${sessionId}"
        val now = System.currentTimeMillis()

        val deletedItems = items.map { item ->
            DeletedHistoryEntity(
                originalId = item.id,
                sessionId = item.sessionId,
                userId = item.userId,
                userName = item.userName,
                userColorHex = item.userColorHex,
                actionType = item.actionType,
                originalTimestamp = item.timestamp,
                elapsedSeconds = item.elapsedSeconds,
                deletedAt = now,
                deletionType = "BULK",
                bulkBatchId = batchId,
                sessionName = sessionName
            )
        }
        turnDao.insertDeletedHistoryList(deletedItems)
        turnDao.deleteHistoryBySession(sessionId)
    }

    // Restore single deleted item back to history
    suspend fun restoreDeletedItem(deletedId: Int) {
        val item = turnDao.getDeletedHistoryById(deletedId) ?: return
        val restoredHistory = HistoryEntity(
            sessionId = item.sessionId,
            userId = item.userId,
            userName = item.userName,
            userColorHex = item.userColorHex,
            actionType = item.actionType,
            timestamp = item.originalTimestamp,
            elapsedSeconds = item.elapsedSeconds
        )
        turnDao.insertHistory(restoredHistory)
        turnDao.deleteDeletedHistoryById(deletedId)
        unlockUserAchievement(item.userId, "recycler_hero")
    }

    // Restore entire batch back to history
    suspend fun restoreDeletedBatch(batchId: String) {
        val items = turnDao.getDeletedHistoryByBatchId(batchId)
        for (item in items) {
            val restoredHistory = HistoryEntity(
                sessionId = item.sessionId,
                userId = item.userId,
                userName = item.userName,
                userColorHex = item.userColorHex,
                actionType = item.actionType,
                timestamp = item.originalTimestamp,
                elapsedSeconds = item.elapsedSeconds
            )
            turnDao.insertHistory(restoredHistory)
            unlockUserAchievement(item.userId, "batch_savior")
        }
        turnDao.deleteDeletedHistoryByBatchId(batchId)
    }

    // Permanently delete single item
    suspend fun permanentlyDeleteTrashItem(deletedId: Int) {
        val item = turnDao.getDeletedHistoryById(deletedId)
        if (item != null) {
            unlockUserAchievement(item.userId, "clean_slate")
        }
        turnDao.deleteDeletedHistoryById(deletedId)
    }

    // Permanently delete batch
    suspend fun permanentlyDeleteTrashBatch(batchId: String) {
        turnDao.deleteDeletedHistoryByBatchId(batchId)
    }

    // Clear all trash
    suspend fun clearAllTrash() {
        turnDao.deleteAllDeletedHistory()
    }

    // Deleted History flows
    val allDeletedHistory: Flow<List<DeletedHistoryEntity>> = turnDao.getAllDeletedHistoryFlow()
    val singleDeletedHistory: Flow<List<DeletedHistoryEntity>> = turnDao.getDeletedHistoryByTypeFlow("SINGLE")
    val bulkDeletedHistory: Flow<List<DeletedHistoryEntity>> = turnDao.getDeletedHistoryByTypeFlow("BULK")

    suspend fun insertHistoryRecord(
        sessionId: Int,
        userId: Int,
        userName: String,
        userColorHex: String,
        actionType: String,
        elapsedSeconds: Long,
        isAutoFinished: Boolean = false,
        hadLongPause: Boolean = false
    ) {
        val history = HistoryEntity(
            sessionId = sessionId,
            userId = userId,
            userName = userName,
            userColorHex = userColorHex,
            actionType = actionType,
            elapsedSeconds = elapsedSeconds
        )
        turnDao.insertHistory(history)

        val sessions = turnDao.getAllSessions()
        val session = sessions.find { it.id == sessionId }
        val sessionType = session?.type ?: "PLAY"

        // If it's a finish, we update user statistics and achievements!
        if (actionType == "FINISH" && elapsedSeconds > 0) {
            val user = turnDao.getUserById(userId)
            if (user != null) {
                val newCount = user.turnsCount + 1
                val newTotal = user.totalDurationSeconds + elapsedSeconds
                val newAvg = newTotal / newCount
                val updatedUser = user.copy(
                    turnsCount = newCount,
                    totalDurationSeconds = newTotal,
                    averageDurationSeconds = newAvg
                )
                turnDao.updateUser(updatedUser)

                // Trigger achievement updates with verified parameters
                updateAchievementsAfterTurn(
                    userId = userId,
                    elapsedSeconds = elapsedSeconds,
                    userTurnsCount = newCount,
                    userTotalSeconds = newTotal,
                    sessionType = sessionType,
                    sessionId = sessionId,
                    isAutoFinished = isAutoFinished,
                    hadLongPause = hadLongPause
                )
            }
        } else if (actionType == "PAUSE") {
            // Count pauses to check for Pause Master
            val pauseCountKey = "pause_count_user_$userId"
            val currentPauseStr = turnDao.getPreference(pauseCountKey)?.value ?: "0"
            val currentPause = currentPauseStr.toInt() + 1
            turnDao.insertPreference(PreferenceEntity(pauseCountKey, currentPause.toString()))
            
            val achs = turnDao.getAchievementsByUser(userId)
            achs.find { it.id == "pause_curious" }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = currentPause.coerceAtMost(3)
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 3))
                }
            }
            achs.find { it.id == "pause_master" }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = currentPause.coerceAtMost(10)
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 10))
                }
            }
        }
    }

    // Achievements
    val allAchievements: Flow<List<AchievementEntity>> = turnDao.getAllAchievementsFlow()

    suspend fun initializeDefaultAchievements() {
        val existingUsers = turnDao.getAllUsers()
        for (user in existingUsers) {
            initializeAchievementsForUser(user.id)
        }
    }

    suspend fun initializeAchievementsForUser(userId: Int) {
        val defaults = getFullAchievementsList(userId)
        val existing = turnDao.getAchievementsByUser(userId)
        val existingMap = existing.associateBy { it.id }

        for (ach in defaults) {
            if (!existingMap.containsKey(ach.id)) {
                turnDao.insertAchievement(ach)
            }
        }
    }

    private fun getFullAchievementsList(userId: Int): List<AchievementEntity> {
        return listOf(
            // 1-10: Milestones & Turn Counts
            AchievementEntity(userId, "first_turn", "أول خطوة 🏆", "أكمل أول دور لك بنجاح", false, 0, 1),
            AchievementEntity(userId, "duo_turn", "الثنائية الرائعة 👥", "أكمل دورين ناجحين", false, 0, 2),
            AchievementEntity(userId, "hat_trick", "هاتريك الأبطال 🎩", "أكمل 3 أدوار ناجحة", false, 0, 3),
            AchievementEntity(userId, "club_five", "خماسي القوة ✋", "أكمل 5 أدوار بنجاح", false, 0, 5),
            AchievementEntity(userId, "club_ten", "نادي العشرة 🔟", "أكمل 10 أدوار إجمالاً", false, 0, 10),
            AchievementEntity(userId, "turns_25", "ربع قرن من الأدوار 🎖️", "أكمل 25 دوراً بنجاح", false, 0, 25),
            AchievementEntity(userId, "half_century", "نصف مئوية الإنجاز 🏅", "أكمل 50 دوراً إجمالياً", false, 0, 50),
            AchievementEntity(userId, "legendary_century", "مئوية العظماء 💯", "أكمل 100 دور بنجاح", false, 0, 100),
            AchievementEntity(userId, "titan_250", "تيتان الأدوار 🗿", "أكمل 250 دوراً بنجاح", false, 0, 250),
            AchievementEntity(userId, "grandmaster_500", "عملاق الألفية 👑", "أكمل 500 دور بنجاح", false, 0, 500),

            // 11-20: Sessions & Categories
            AchievementEntity(userId, "sages_work", "عبقري الأعمال 💼", "أكمل 5 أدوار في جلسات العمل", false, 0, 5),
            AchievementEntity(userId, "work_tycoon", "إمبراطور الشركات 🏢", "أكمل 20 دوراً في جلسات العمل", false, 0, 20),
            AchievementEntity(userId, "study_monster", "دحيح المذاكرة 📚", "أكمل 5 أدوار في جلسات الدراسة", false, 0, 5),
            AchievementEntity(userId, "phd_scholar", "بروفيسور المعرفة 🎓", "أكمل 20 دوراً في جلسات الدراسة", false, 0, 20),
            AchievementEntity(userId, "true_gamer", "غيمر أسطوري 🎮", "أكمل 10 أدوار في جلسات الألعاب", false, 0, 10),
            AchievementEntity(userId, "esports_champ", "بطل الإيسبورتس 🕹️", "أكمل 25 دوراً في جلسات الألعاب", false, 0, 25),
            AchievementEntity(userId, "important_meeting", "لقاء الكبار 🤝", "أكمل 5 أدوار في جلسات الاجتماعات", false, 0, 5),
            AchievementEntity(userId, "board_member", "عضو مجلس الإدارة 🏛️", "أكمل 20 دوراً في جلسات الاجتماعات", false, 0, 20),
            AchievementEntity(userId, "pomodoro_master", "سيد البومودورو 🍅", "أكمل 5 أدوار في جلسات بومودورو", false, 0, 5),
            AchievementEntity(userId, "pomodoro_zen", "راهب التركيز الفائق 🧘‍♂️", "أكمل 20 دوراً في جلسات بومودورو", false, 0, 20),

            // 21-28: Cumulative Duration
            AchievementEntity(userId, "full_hour", "ساعة ذهبية ⏱️", "أكمل إجمالي ساعة كاملة من الأدوار", false, 0, 3600),
            AchievementEntity(userId, "three_hours", "ثلاثية الساعات ⏳", "أكمل إجمالي 3 ساعات من الأدوار", false, 0, 10800),
            AchievementEntity(userId, "half_day", "نصف يوم إنتاجية 🌓", "أكمل إجمالي 12 ساعة من الأدوار", false, 0, 43200),
            AchievementEntity(userId, "hero_day", "بطل الـ24 ساعة 🌞", "أكمل إجمالي 24 ساعة من الأدوار", false, 0, 86400),
            AchievementEntity(userId, "hours_50", "سيد الزمن 👑", "أكمل إجمالي 50 ساعة من الأدوار", false, 0, 180000),
            AchievementEntity(userId, "hours_100", "أسطورة المئة ساعة 💎", "أكمل إجمالي 100 ساعة من الأدوار", false, 0, 360000),
            AchievementEntity(userId, "pause_curious", "متذوق الإيقاف ⏸️", "قم بإيقاف الدور مؤقتاً 3 مرات", false, 0, 3),
            AchievementEntity(userId, "pause_master", "محترف التجميد ❄️", "قم بإيقاف الأدوار مؤقتاً 10 مرات", false, 0, 10),

            // 29-38: Turn Durations & Speed
            AchievementEntity(userId, "open_time_voyager", "رحالة الفضاء المفتوح 🌌", "أكمل دوراً بالوقت المفتوح لأكثر من 15 دقيقة", false, 0, 900),
            AchievementEntity(userId, "open_time_beast", "وحش الوقت اللانهائي ♾️", "أكمل دوراً بالوقت المفتوح لأكثر من 45 دقيقة", false, 0, 2700),
            AchievementEntity(userId, "lightning_fast", "البرق الخاطف ⚡", "أنهِ دوراً بنجاح في أقل من 15 ثانية", false, 0, 1),
            AchievementEntity(userId, "patient_monk", "الصبور الهادئ 🧘", "أكمل دوراً واحداً مدته تزيد عن ساعة", false, 0, 1),
            AchievementEntity(userId, "ultra_marathon", "ماراثون الجبابرة 🏃‍♂️", "أكمل دوراً واحداً مدته تزيد عن ساعتين", false, 0, 1),
            AchievementEntity(userId, "speed_demon", "الشيطان السريع 🏎️", "أكمل دوراً مدته بين 15 و 30 ثانية", false, 0, 1),
            AchievementEntity(userId, "coffee_break", "استراحة قهوة ☕", "أكمل دوراً مدته تزيد عن 15 دقيقة", false, 0, 1),
            AchievementEntity(userId, "power_nap", "غفوة الطاقة ⚡", "أكمل دوراً مدته تزيد عن 20 دقيقة", false, 0, 1),
            AchievementEntity(userId, "precision_time", "دقة الميكرومتر 🎯", "أكمل دوراً تلقائياً حتى انتهاء المؤقت تماماً", false, 0, 1),
            AchievementEntity(userId, "dramatic_pause", "وقفة درامية 🎭", "استأنف دوراً بعد إيقاف مؤقت طويل وأكمله", false, 0, 1),

            // 39-48: Time of Day & Days Streaks
            AchievementEntity(userId, "quiet_night", "ساهر الليل والنجوم 🌙", "أنهِ دوراً بين الساعة 12:00 منتصف الليل و 4:00 فجراً", false, 0, 1),
            AchievementEntity(userId, "early_bird", "عصفور الصباح الباكر 🐦", "أنهِ دوراً بين الساعة 5:00 صباحاً و 8:00 صباحاً", false, 0, 1),
            AchievementEntity(userId, "afternoon_rush", "نشاط وقت الظهيرة ☀️", "أنهِ دوراً بين الساعة 12:00 ظهراً و 3:00 عصراً", false, 0, 1),
            AchievementEntity(userId, "tea_time", "رشفة شاي المساء 🍵", "أنهِ دوراً بين الساعة 4:00 عصراً و 7:00 مساءً", false, 0, 1),
            AchievementEntity(userId, "evening_relax", "هدوء المساء 🌆", "أنهِ دوراً بين الساعة 7:00 مساءً و 11:00 مساءً", false, 0, 1),
            AchievementEntity(userId, "weekend_warrior", "محارب عطلة الأسبوع ⚔️", "أكمل دوراً في يوم الجمعة أو السبت", false, 0, 1),
            AchievementEntity(userId, "day_streak_3", "استمرار 3 أيام 📅", "مارس الأدوار في 3 أيام مختلفة", false, 0, 3),
            AchievementEntity(userId, "day_streak_7", "أسبوع كامل من الالتزام 🔥", "مارس الأدوار في 7 أيام مختلفة", false, 0, 7),
            AchievementEntity(userId, "day_streak_14", "أسبوعان بلا توقف 🚀", "مارس الأدوار في 14 يوماً مختلفاً", false, 0, 14),
            AchievementEntity(userId, "day_streak_30", "شهر من الفولاذ 🛡️", "مارس الأدوار في 30 يوماً مختلفاً", false, 0, 30),

            // 49-56: Daily Intensity & Social
            AchievementEntity(userId, "hyperactive_day", "شعلة النشاط اليومي ⚡", "أكمل 5 أدوار في نفس اليوم", false, 0, 5),
            AchievementEntity(userId, "crazy_ten_day", "مجنون الـ10 أدوار 🌪️", "أكمل 10 أدوار في نفس اليوم", false, 0, 10),
            AchievementEntity(userId, "party_starter", "روح الفريق 🎉", "شارك في جلسة تحتوي على 4 مستخدمين أو أكثر", false, 0, 1),
            AchievementEntity(userId, "big_clan", "قبيلة الأدوار 👥", "شارك في جلسة تحتوي على 6 مستخدمين أو أكثر", false, 0, 1),
            AchievementEntity(userId, "king_session", "ملك الجلسة 👑", "كن المستخدم الأكثر تسجيلاً للأدوار في الجلسة الحالية", false, 0, 1),
            AchievementEntity(userId, "planner_architect", "مهندس الجلسات 📐", "أنشئ 3 جلسات تنظيمية مختلفة", false, 0, 3),
            AchievementEntity(userId, "empire_sessions", "إمبراطورية الجلسات 🏰", "أنشئ 6 جلسات تنظيمية مختلفة", false, 0, 6),
            AchievementEntity(userId, "gold_standard", "المعيار الذهبي 🌟", "اجعل متوسط مدة أدوارك يتجاوز 15 دقيقة", false, 0, 1),

            // 57-67: Customization, Secrets & Trash Archive (Unique & Secret Achievements)
            AchievementEntity(userId, "secret_agent", "العميل السري 🕵️‍♂️", "اكتشف وافتح سجل الأدوار المحذوفة السري!", false, 0, 1),
            AchievementEntity(userId, "recycler_hero", "بطل إعادة التدوير ♻️", "استعد دوراً واحداً من سجل الأدوار المحذوفة", false, 0, 1),
            AchievementEntity(userId, "clean_slate", "الصفحة البيضاء 🧹", "احذف دوراً نهائياً من الأرشيف السري", false, 0, 1),
            AchievementEntity(userId, "batch_savior", "منقذ الدفعات 📦", "استعد دفعة سجل كاملة من الأرشيف السري", false, 0, 1),
            AchievementEntity(userId, "circle_connoisseur", "متذوق الأشكال الساحرة 🎨", "قم بتغيير شكل دائرة المؤقت من الإعدادات", false, 0, 1),
            AchievementEntity(userId, "theme_explorer", "مستكشف الأبعاد 🌌", "قم بتبديل ثيم التطبيق من الإعدادات", false, 0, 1),
            AchievementEntity(userId, "audio_virtuoso", "موسيقار النغمات 🎼", "حدد نغمة مخصصة لنهاية دورك", false, 0, 1),
            AchievementEntity(userId, "custom_time_genius", "عبقري الوقت المخصص ⚙️", "حدد وقتاً مخصصاً لدورك من لوحة الأوقات", false, 0, 1),
            AchievementEntity(userId, "notification_commander", "قائد الإشعارات 🔔", "تحكم بالدور وبدّل الوقت من لوحة الإشعارات", false, 0, 1),
            AchievementEntity(userId, "widget_wizard", "ساحر الويدجت 📱", "تفاعل مع أدوارك من ويدجت الشاشة الرئيسية", false, 0, 1),
            AchievementEntity(userId, "zen_master", "معلم السلام الداخلي 🌸", "أكمل 5 أدوار دون أي إيقاف مؤقت", false, 0, 5)
        )
    }

    suspend fun unlockUserAchievement(userId: Int, id: String) {
        val achievements = turnDao.getAchievementsByUser(userId)
        val ach = achievements.find { it.id == id }
        if (ach != null && !ach.isUnlocked) {
            turnDao.updateAchievement(ach.copy(isUnlocked = true, progress = ach.maxProgress))
        }
    }

    private suspend fun updateAchievementsAfterTurn(
        userId: Int,
        elapsedSeconds: Long,
        userTurnsCount: Int,
        userTotalSeconds: Long,
        sessionType: String,
        sessionId: Int,
        isAutoFinished: Boolean,
        hadLongPause: Boolean
    ) {
        val achs = turnDao.getAchievementsByUser(userId)
        if (achs.isEmpty()) return

        // 1. Milestone counts
        unlockUserAchievement(userId, "first_turn")

        val countAchs = listOf(
            "duo_turn" to 2,
            "hat_trick" to 3,
            "club_five" to 5,
            "club_ten" to 10,
            "turns_25" to 25,
            "half_century" to 50,
            "legendary_century" to 100,
            "titan_250" to 250,
            "grandmaster_500" to 500
        )
        for ((achId, target) in countAchs) {
            achs.find { it.id == achId }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = userTurnsCount.coerceAtMost(target)
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= target))
                }
            }
        }

        // 2. Cumulative seconds
        val timeAchs = listOf(
            "full_hour" to 3600,
            "three_hours" to 10800,
            "half_day" to 43200,
            "hero_day" to 86400,
            "hours_50" to 180000,
            "hours_100" to 360000
        )
        for ((achId, target) in timeAchs) {
            achs.find { it.id == achId }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = userTotalSeconds.coerceAtMost(target.toLong()).toInt()
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = userTotalSeconds >= target))
                }
            }
        }

        // 3. Category session counts
        when (sessionType) {
            "WORK" -> {
                achs.find { it.id == "sages_work" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(5)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 5))
                    }
                }
                achs.find { it.id == "work_tycoon" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(20)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 20))
                    }
                }
            }
            "STUDY" -> {
                achs.find { it.id == "study_monster" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(5)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 5))
                    }
                }
                achs.find { it.id == "phd_scholar" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(20)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 20))
                    }
                }
            }
            "PLAY" -> {
                achs.find { it.id == "true_gamer" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(10)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 10))
                    }
                }
                achs.find { it.id == "esports_champ" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(25)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 25))
                    }
                }
            }
            "MEETING" -> {
                achs.find { it.id == "important_meeting" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(5)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 5))
                    }
                }
                achs.find { it.id == "board_member" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(20)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 20))
                    }
                }
            }
            "POMODORO" -> {
                achs.find { it.id == "pomodoro_master" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(5)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 5))
                    }
                }
                achs.find { it.id == "pomodoro_zen" }?.let { ach ->
                    if (!ach.isUnlocked) {
                        val p = (ach.progress + 1).coerceAtMost(20)
                        turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 20))
                    }
                }
            }
        }

        // 4. Turn duration specific checks
        if (elapsedSeconds in 1..14) {
            unlockUserAchievement(userId, "lightning_fast")
        }
        if (elapsedSeconds in 15..30) {
            unlockUserAchievement(userId, "speed_demon")
        }
        if (elapsedSeconds >= 900) {
            unlockUserAchievement(userId, "coffee_break")
        }
        if (elapsedSeconds >= 1200) {
            unlockUserAchievement(userId, "power_nap")
        }
        if (elapsedSeconds >= 3600) {
            unlockUserAchievement(userId, "patient_monk")
        }
        if (elapsedSeconds >= 7200) {
            unlockUserAchievement(userId, "ultra_marathon")
        }

        // Open mode achievements
        if (elapsedSeconds >= 900) {
            achs.find { it.id == "open_time_voyager" }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = elapsedSeconds.coerceAtMost(900).toInt()
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 900))
                }
            }
        }
        if (elapsedSeconds >= 2700) {
            achs.find { it.id == "open_time_beast" }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = elapsedSeconds.coerceAtMost(2700).toInt()
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 2700))
                }
            }
        }

        if (isAutoFinished) {
            unlockUserAchievement(userId, "precision_time")
        }
        if (hadLongPause) {
            unlockUserAchievement(userId, "dramatic_pause")
        }

        // 5. Time of day checks
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

        if (hour in 0..4) {
            unlockUserAchievement(userId, "quiet_night")
        }
        if (hour in 5..8) {
            unlockUserAchievement(userId, "early_bird")
        }
        if (hour in 12..15) {
            unlockUserAchievement(userId, "afternoon_rush")
        }
        if (hour in 16..18) {
            unlockUserAchievement(userId, "tea_time")
        }
        if (hour in 19..23) {
            unlockUserAchievement(userId, "evening_relax")
        }
        if (dayOfWeek == Calendar.FRIDAY || dayOfWeek == Calendar.SATURDAY) {
            unlockUserAchievement(userId, "weekend_warrior")
        }

        // 6. Day Streak Tracking (Strict unique calendar days)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val daysPrefKey = "pref_unique_days_user_$userId"
        val savedDaysStr = turnDao.getPreference(daysPrefKey)?.value ?: ""
        val uniqueDays = savedDaysStr.split(",").filter { it.isNotBlank() }.toMutableSet()
        if (uniqueDays.add(todayStr)) {
            turnDao.insertPreference(PreferenceEntity(daysPrefKey, uniqueDays.joinToString(",")))
        }

        val totalDays = uniqueDays.size
        val dayStreakAchs = listOf(
            "day_streak_3" to 3,
            "day_streak_7" to 7,
            "day_streak_14" to 14,
            "day_streak_30" to 30
        )
        for ((achId, target) in dayStreakAchs) {
            achs.find { it.id == achId }?.let { ach ->
                if (!ach.isUnlocked) {
                    val p = totalDays.coerceAtMost(target)
                    turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= target))
                }
            }
        }

        // 7. Turns finished TODAY
        val todayKey = "pref_turns_today_user_${userId}_$todayStr"
        val turnsToday = (turnDao.getPreference(todayKey)?.value?.toIntOrNull() ?: 0) + 1
        turnDao.insertPreference(PreferenceEntity(todayKey, turnsToday.toString()))

        achs.find { it.id == "hyperactive_day" }?.let { ach ->
            if (!ach.isUnlocked) {
                val p = turnsToday.coerceAtMost(5)
                turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 5))
            }
        }
        achs.find { it.id == "crazy_ten_day" }?.let { ach ->
            if (!ach.isUnlocked) {
                val p = turnsToday.coerceAtMost(10)
                turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 10))
            }
        }

        // 8. Session user count check
        val allUsersInDb = turnDao.getAllUsers()
        if (allUsersInDb.size >= 4) {
            unlockUserAchievement(userId, "party_starter")
        }
        if (allUsersInDb.size >= 6) {
            unlockUserAchievement(userId, "big_clan")
        }

        // 9. King of Session check (Most turns in this session)
        val historyInSession = turnDao.getHistoryBySession(sessionId)
        val turnsPerUserInSession = historyInSession
            .filter { it.actionType == "FINISH" }
            .groupBy { it.userId }
            .mapValues { it.value.size }
        val maxTurnsInSession = turnsPerUserInSession.values.maxOrNull() ?: 0
        val myTurnsInSession = turnsPerUserInSession[userId] ?: 0
        if (myTurnsInSession > 0 && myTurnsInSession >= maxTurnsInSession && turnsPerUserInSession.size > 1) {
            unlockUserAchievement(userId, "king_session")
        }

        // 10. Average duration
        val avg = if (userTurnsCount > 0) userTotalSeconds / userTurnsCount else 0L
        if (avg >= 900L) { // 15 mins
            unlockUserAchievement(userId, "gold_standard")
        }
    }

    suspend fun onSessionCreated(userId: Int, totalSessionsCount: Int) {
        val achs = turnDao.getAchievementsByUser(userId)
        achs.find { it.id == "planner_architect" }?.let { ach ->
            if (!ach.isUnlocked) {
                val p = totalSessionsCount.coerceAtMost(3)
                turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 3))
            }
        }
        achs.find { it.id == "empire_sessions" }?.let { ach ->
            if (!ach.isUnlocked) {
                val p = totalSessionsCount.coerceAtMost(6)
                turnDao.updateAchievement(ach.copy(progress = p, isUnlocked = p >= 6))
            }
        }
    }

    suspend fun onSecretArchiveOpened(userId: Int) {
        unlockUserAchievement(userId, "secret_agent")
    }

    suspend fun onCircleStyleChanged(userId: Int) {
        unlockUserAchievement(userId, "circle_connoisseur")
    }

    suspend fun onThemeChanged(userId: Int) {
        unlockUserAchievement(userId, "theme_explorer")
    }

    suspend fun onCustomSoundSet(userId: Int) {
        unlockUserAchievement(userId, "audio_virtuoso")
    }

    suspend fun onCustomDurationUsed(userId: Int) {
        unlockUserAchievement(userId, "custom_time_genius")
    }

    suspend fun onNotificationActionUsed(userId: Int) {
        unlockUserAchievement(userId, "notification_commander")
    }

    suspend fun onWidgetActionUsed(userId: Int) {
        unlockUserAchievement(userId, "widget_wizard")
    }

    // Preferences
    val allPreferences: Flow<List<PreferenceEntity>> = turnDao.getAllPreferencesFlow()

    suspend fun getPreferenceValue(key: String, defaultValue: String): String {
        return turnDao.getPreference(key)?.value ?: defaultValue
    }

    suspend fun savePreference(key: String, value: String) {
        turnDao.insertPreference(PreferenceEntity(key, value))
    }
}
