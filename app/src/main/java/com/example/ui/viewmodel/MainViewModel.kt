package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.*
import com.example.engine.TurnEngine
import com.example.engine.TurnState
import com.example.repository.TurnRepository
import com.example.service.TurnService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

data class UserOnboardingData(
    val name: String,
    val emoji: String,
    val colorHex: String,
    val customSoundUri: String? = null
)


@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TurnRepository = DatabaseModule.getRepository(application)

    // Flows from Repository
    val users = repository.allUsers.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val history = repository.allHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val achievements = repository.allAchievements.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val preferences = repository.allPreferences.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Deleted History flows (Secret Archive / Trash)
    val allDeletedHistory = repository.allDeletedHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val singleDeletedHistory = repository.singleDeletedHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val bulkDeletedHistory = repository.bulkDeletedHistory.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // User-specific Achievements Flow
    private val _selectedAchievementUserId = MutableStateFlow<Int?>(null)
    val selectedAchievementUserId = _selectedAchievementUserId.asStateFlow()

    fun selectAchievementUser(userId: Int) {
        _selectedAchievementUserId.value = userId
    }

    val userAchievements = _selectedAchievementUserId.flatMapLatest { userId ->
        if (userId != null) {
            DatabaseModule.getDatabase(getApplication()).turnDao().getAchievementsByUserFlow(userId)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // UI state flows
    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _currentSessionId = MutableStateFlow<Int?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _currentTheme = MutableStateFlow("Dark")
    val currentTheme = _currentTheme.asStateFlow()

    private val _circleStyle = MutableStateFlow("Glow Circle")
    val circleStyle = _circleStyle.asStateFlow()

    private val _soundPack = MutableStateFlow("Default")
    val soundPack = _soundPack.asStateFlow()

    private val _vibrationEnabled = MutableStateFlow(true)
    val vibrationEnabled = _vibrationEnabled.asStateFlow()

    // History Preferences (Time & Date formats, sorting, visibility)
    private val _historyDurationFormat = MutableStateFlow("DIGITAL_MM_SS") // "DIGITAL_MM_SS", "DIGITAL_M_SS", "DIGITAL_HH_MM_SS", "TEXT_ARABIC", "TEXT_COMPACT", "BADGE_PLUS", "TOTAL_SECONDS"
    val historyDurationFormat = _historyDurationFormat.asStateFlow()

    private val _historyTimeFormat = MutableStateFlow("12H") // "12H", "24H"
    val historyTimeFormat = _historyTimeFormat.asStateFlow()

    private val _historyDateFormat = MutableStateFlow("FULL_ARABIC") // "FULL_ARABIC", "RELATIVE", "SLASH_YMD", "SLASH_DMY"
    val historyDateFormat = _historyDateFormat.asStateFlow()

    private val _historySortOrder = MutableStateFlow("NEWEST") // "NEWEST", "OLDEST", "LONGEST", "SHORTEST"
    val historySortOrder = _historySortOrder.asStateFlow()

    private val _historyShowExactTimestamps = MutableStateFlow(true)
    val historyShowExactTimestamps = _historyShowExactTimestamps.asStateFlow()

    private val _historyShowTimelineDots = MutableStateFlow(true)
    val historyShowTimelineDots = _historyShowTimelineDots.asStateFlow()

    init {
        viewModelScope.launch {
            // Initialize default achievements
            repository.initializeDefaultAchievements()

            // Load Preferences
            _currentTheme.value = repository.getPreferenceValue("pref_theme", "Dark")
            _circleStyle.value = repository.getPreferenceValue("pref_circle_style", "Glow Circle")
            _soundPack.value = repository.getPreferenceValue("pref_sound_pack", "Default")
            _vibrationEnabled.value = repository.getPreferenceValue("pref_vibrate", "true").toBoolean()

            // Load History Preferences
            _historyDurationFormat.value = repository.getPreferenceValue("pref_history_duration_fmt", "DIGITAL_MM_SS")
            _historyTimeFormat.value = repository.getPreferenceValue("pref_history_time_fmt", "12H")
            _historyDateFormat.value = repository.getPreferenceValue("pref_history_date_fmt", "FULL_ARABIC")
            _historySortOrder.value = repository.getPreferenceValue("pref_history_sort_order", "NEWEST")
            _historyShowExactTimestamps.value = repository.getPreferenceValue("pref_history_exact_times", "true").toBoolean()
            _historyShowTimelineDots.value = repository.getPreferenceValue("pref_history_timeline_dots", "true").toBoolean()

            // Fetch actual users and sessions from Room directly (to avoid Lazy StateFlow race conditions)
            val dbUsers = repository.allUsers.first()
            val dbSessions = repository.allSessions.first()

            if (dbUsers.isNotEmpty()) {
                if (dbSessions.isNotEmpty()) {
                    val savedSessionIdStr = repository.getPreferenceValue("pref_active_session_id", "")
                    val savedSessionId = savedSessionIdStr.toIntOrNull()
                    val activeSession = dbSessions.find { it.id == savedSessionId } ?: dbSessions.first()
                    
                    _currentSessionId.value = activeSession.id
                    TurnEngine.setupSession(activeSession, dbUsers)
                } else {
                    val defaultSession = SessionEntity(name = "جلسة اللعب الأولى", type = "PLAY")
                    val newId = repository.insertSession(defaultSession).toInt()
                    _currentSessionId.value = newId
                    repository.savePreference("pref_active_session_id", newId.toString())
                    TurnEngine.setupSession(defaultSession.copy(id = newId), dbUsers)
                }
            }

            // Start foreground service if already onboarded
            if (dbUsers.isNotEmpty()) {
                startServiceIfNeeded()
            }

            // Mark as loaded to render home screen/onboarding
            _isLoaded.value = true

            // Keep selected session synced reactively if list changes, but NEVER create a session here
            launch {
                sessions.collect { list ->
                    if (list.isNotEmpty()) {
                        val currentId = _currentSessionId.value
                        if (currentId == null || list.none { it.id == currentId }) {
                            val nextSession = list.first()
                            _currentSessionId.value = nextSession.id
                            val currentUsers = users.value
                            if (TurnEngine.currentSession == null || TurnEngine.currentSession?.id != nextSession.id) {
                                TurnEngine.setupSession(nextSession, currentUsers)
                            }
                        }
                    }
                }
            }

            // Keep selectedAchievementUserId updated when users load
            launch {
                users.collect { list ->
                    if (_selectedAchievementUserId.value == null && list.isNotEmpty()) {
                        _selectedAchievementUserId.value = list.first().id
                    }
                }
            }
        }
    }

    fun startServiceIfNeeded() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, TurnService::class.java).apply {
            action = TurnService.ACTION_START_SERVICE
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Onboarding setup: creates users and first session
    fun setupOnboarding(userList: List<UserOnboardingData>, sessionName: String, sessionType: String) {
        viewModelScope.launch {
            val session = SessionEntity(name = sessionName, type = sessionType)
            val sessionId = repository.insertSession(session).toInt()
            _currentSessionId.value = sessionId
            repository.savePreference("pref_active_session_id", sessionId.toString())

            val userEntities = mutableListOf<UserEntity>()

            userList.forEach { onboardingUser ->
                val user = UserEntity(
                    name = onboardingUser.name,
                    avatarEmoji = onboardingUser.emoji,
                    colorHex = onboardingUser.colorHex,
                    soundPack = "Default",
                    customSoundUri = onboardingUser.customSoundUri
                )
                val userId = repository.insertUser(user).toInt()
                userEntities.add(user.copy(id = userId))
            }

            // Sync with engine
            val finalSession = session.copy(id = sessionId)
            TurnEngine.setupSession(finalSession, userEntities)

            // Auto start Foreground Service to manage timers
            startServiceIfNeeded()
        }
    }

    // Change current session
    fun selectSession(sessionId: Int) {
        viewModelScope.launch {
            val list = sessions.value
            val session = list.find { it.id == sessionId }
            if (session != null) {
                _currentSessionId.value = sessionId
                repository.savePreference("pref_active_session_id", sessionId.toString())
                val activeUsers = users.value
                TurnEngine.setupSession(session, activeUsers)
            }
        }
    }

    fun addNewSession(name: String, type: String) {
        viewModelScope.launch {
            val session = SessionEntity(name = name, type = type)
            val newId = repository.insertSession(session).toInt()
            _currentSessionId.value = newId
            repository.savePreference("pref_active_session_id", newId.toString())
            
            // Re-load engine
            val activeUsers = users.value
            TurnEngine.setupSession(session.copy(id = newId), activeUsers)
        }
    }

    fun updateSession(session: SessionEntity) {
        viewModelScope.launch {
            repository.updateSession(session)
            // If the edited session is the currently active one, update it in TurnEngine as well
            if (_currentSessionId.value == session.id) {
                val activeUsers = users.value
                TurnEngine.setupSession(session, activeUsers)
            }
        }
    }

    fun addNewUser(name: String, emoji: String, colorHex: String, customSoundUri: String? = null) {
        viewModelScope.launch {
            val user = UserEntity(
                name = name,
                avatarEmoji = emoji,
                colorHex = colorHex,
                customSoundUri = customSoundUri
            )
            repository.insertUser(user)
            
            // Re-sync engine users
            val currentSession = sessions.value.find { it.id == _currentSessionId.value }
            if (currentSession != null) {
                val updatedUsers = repository.allUsers.first()
                TurnEngine.setupSession(currentSession, updatedUsers)
            }
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.moveHistoryItemToTrash(id)
        }
    }

    fun deleteHistoryBySession(sessionId: Int) {
        viewModelScope.launch {
            repository.moveSessionHistoryToTrash(sessionId)
        }
    }

    fun restoreDeletedItem(id: Int) {
        viewModelScope.launch {
            repository.restoreDeletedItem(id)
        }
    }

    fun restoreDeletedBatch(batchId: String) {
        viewModelScope.launch {
            repository.restoreDeletedBatch(batchId)
        }
    }

    fun permanentlyDeleteTrashItem(id: Int) {
        viewModelScope.launch {
            repository.permanentlyDeleteTrashItem(id)
        }
    }

    fun permanentlyDeleteTrashBatch(batchId: String) {
        viewModelScope.launch {
            repository.permanentlyDeleteTrashBatch(batchId)
        }
    }

    fun clearAllTrash() {
        viewModelScope.launch {
            repository.clearAllTrash()
        }
    }

    fun onSecretArchiveOpened() {
        val user = TurnEngine.currentUser.value
        if (user != null) {
            viewModelScope.launch {
                repository.onSecretArchiveOpened(user.id)
            }
        }
    }

    fun updateUser(user: UserEntity) {
        viewModelScope.launch {
            repository.updateUser(user)
            
            // Re-sync engine users
            val currentSession = sessions.value.find { it.id == _currentSessionId.value }
            if (currentSession != null) {
                val updatedUsers = repository.allUsers.first()
                TurnEngine.setupSession(currentSession, updatedUsers)
            }
        }
    }

    fun deleteUser(user: UserEntity) {
        viewModelScope.launch {
            repository.deleteUser(user)
            // Re-sync engine
            val currentSession = sessions.value.find { it.id == _currentSessionId.value }
            if (currentSession != null) {
                val updatedUsers = repository.allUsers.first()
                TurnEngine.setupSession(currentSession, updatedUsers)
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            val savedSessionIdStr = repository.getPreferenceValue("pref_active_session_id", "")
            if (savedSessionIdStr == sessionId.toString()) {
                repository.savePreference("pref_active_session_id", "")
            }
        }
    }

    // Preference customization
    fun updateTheme(themeName: String) {
        _currentTheme.value = themeName
        viewModelScope.launch {
            repository.savePreference("pref_theme", themeName)
            val user = TurnEngine.currentUser.value
            if (user != null) {
                repository.onThemeChanged(user.id)
            }
        }
    }

    fun updateCircleStyle(styleName: String) {
        _circleStyle.value = styleName
        viewModelScope.launch {
            repository.savePreference("pref_circle_style", styleName)
            val user = TurnEngine.currentUser.value
            if (user != null) {
                repository.onCircleStyleChanged(user.id)
            }
        }
    }

    fun updateSoundPack(packName: String) {
        _soundPack.value = packName
        viewModelScope.launch {
            repository.savePreference("pref_sound_pack", packName)
        }
    }

    fun updateVibration(enabled: Boolean) {
        _vibrationEnabled.value = enabled
        viewModelScope.launch {
            repository.savePreference("pref_vibrate", enabled.toString())
        }
    }

    // History Preferences Setters
    fun updateHistoryDurationFormat(format: String) {
        _historyDurationFormat.value = format
        viewModelScope.launch {
            repository.savePreference("pref_history_duration_fmt", format)
        }
    }

    fun updateHistoryTimeFormat(format: String) {
        _historyTimeFormat.value = format
        viewModelScope.launch {
            repository.savePreference("pref_history_time_fmt", format)
        }
    }

    fun updateHistoryDateFormat(format: String) {
        _historyDateFormat.value = format
        viewModelScope.launch {
            repository.savePreference("pref_history_date_fmt", format)
        }
    }

    fun updateHistorySortOrder(order: String) {
        _historySortOrder.value = order
        viewModelScope.launch {
            repository.savePreference("pref_history_sort_order", order)
        }
    }

    fun updateHistoryShowExactTimestamps(show: Boolean) {
        _historyShowExactTimestamps.value = show
        viewModelScope.launch {
            repository.savePreference("pref_history_exact_times", show.toString())
        }
    }

    fun updateHistoryShowTimelineDots(show: Boolean) {
        _historyShowTimelineDots.value = show
        viewModelScope.launch {
            repository.savePreference("pref_history_timeline_dots", show.toString())
        }
    }

    // EXPORTS & BACKUPS

    fun exportToJson(context: Context): String {
        return try {
            val root = JSONObject()
            
            val usersArray = JSONArray()
            users.value.forEach { u ->
                val jo = JSONObject().apply {
                    put("name", u.name)
                    put("avatarEmoji", u.avatarEmoji)
                    put("colorHex", u.colorHex)
                    put("turnsCount", u.turnsCount)
                    put("totalDurationSeconds", u.totalDurationSeconds)
                }
                usersArray.put(jo)
            }
            root.put("users", usersArray)

            val sessionsArray = JSONArray()
            sessions.value.forEach { s ->
                val jo = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("type", s.type)
                }
                sessionsArray.put(jo)
            }
            root.put("sessions", sessionsArray)

            root.toString(4)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun exportToCsv(): String {
        val sb = java.lang.StringBuilder()
        sb.append("Name,Turns Count,Total Time (Seconds),Average (Seconds)\n")
        users.value.forEach { u ->
            sb.append("${u.name},${u.turnsCount},${u.totalDurationSeconds},${u.averageDurationSeconds}\n")
        }
        return sb.toString()
    }

    fun exportToTxt(): String {
        val sb = java.lang.StringBuilder()
        sb.append("تقرير تطبيق الأدوار\n")
        sb.append("================\n\n")
        sb.append("قائمة المستخدمين:\n")
        users.value.forEach { u ->
            sb.append("- ${u.avatarEmoji} ${u.name}: ${u.turnsCount} أدوار، إجمالي الوقت: ${u.totalDurationSeconds} ثانية\n")
        }
        sb.append("\nالجلسات النشطة:\n")
        sessions.value.forEach { s ->
            sb.append("- جلسة: ${s.name} (${s.type})\n")
        }
        return sb.toString()
    }

    fun restoreFromJson(context: Context, uri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val stringBuilder = StringBuilder()
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line)
                    }
                }
            }
            val json = JSONObject(stringBuilder.toString())
            viewModelScope.launch {
                // Restore users
                val usersArray = json.optJSONArray("users")
                if (usersArray != null) {
                    for (i in 0 until usersArray.length()) {
                        val jo = usersArray.getJSONObject(i)
                        val name = jo.getString("name")
                        val avatar = jo.optString("avatarEmoji", "😎")
                        val color = jo.optString("colorHex", "#FF0055")
                        repository.insertUser(UserEntity(name = name, avatarEmoji = avatar, colorHex = color))
                    }
                }
                // Restore sessions
                val sessionsArray = json.optJSONArray("sessions")
                if (sessionsArray != null) {
                    for (i in 0 until sessionsArray.length()) {
                        val jo = sessionsArray.getJSONObject(i)
                        val name = jo.getString("name")
                        val type = jo.optString("type", "PLAY")
                        repository.insertSession(SessionEntity(name = name, type = type))
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
