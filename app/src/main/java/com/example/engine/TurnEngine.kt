package com.example.engine

import com.example.database.SessionEntity
import com.example.database.UserEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TurnState {
    IDLE, RUNNING, PAUSED, FINISHED, OPEN_MODE
}

sealed class TurnEvent {
    object Started : TurnEvent()
    object Paused : TurnEvent()
    object Resumed : TurnEvent()
    data class TimeExpired(val user: UserEntity, val elapsedSeconds: Long) : TurnEvent()
    data class ManuallySaved(val user: UserEntity, val elapsedSeconds: Long) : TurnEvent()
    data class UserChanged(val newUser: UserEntity) : TurnEvent()
    object SessionEnded : TurnEvent()
    object Canceled : TurnEvent()
    object WarningTick : TurnEvent() // Triggered near the end (last 10s)
}

object TurnEngine {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timerJob: Job? = null

    // State flows
    private val _state = MutableStateFlow(TurnState.IDLE)
    val state: StateFlow<TurnState> = _state.asStateFlow()

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0L)
    val remainingSeconds: StateFlow<Long> = _remainingSeconds.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _totalDurationSeconds = MutableStateFlow(0L)
    val totalDurationSeconds: StateFlow<Long> = _totalDurationSeconds.asStateFlow()

    // Pre-selected target duration while IDLE
    private val _targetDurationSeconds = MutableStateFlow(300L)
    val targetDurationSeconds: StateFlow<Long> = _targetDurationSeconds.asStateFlow()

    private val _isTargetOpenMode = MutableStateFlow(false)
    val isTargetOpenMode: StateFlow<Boolean> = _isTargetOpenMode.asStateFlow()

    // Event Flow
    private val _events = MutableSharedFlow<TurnEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<TurnEvent> = _events.asSharedFlow()

    // Active session and list of users in cycle
    var currentSession: SessionEntity? = null
        private set

    private var activeUsers: List<UserEntity> = emptyList()
    val usersList: List<UserEntity> get() = activeUsers
    private var currentUserIndex = 0
    private var isWarningTriggered = false

    // Initialize or Reset the Engine with a list of users and a session
    fun setupSession(session: SessionEntity, users: List<UserEntity>) {
        val isCurrentlyActive = _state.value == TurnState.RUNNING || _state.value == TurnState.OPEN_MODE || _state.value == TurnState.PAUSED
        if (isCurrentlyActive && currentSession?.id == session.id) {
            // Update active users list without disrupting running turn!
            activeUsers = users
            val currUser = _currentUser.value
            if (currUser != null) {
                val updatedIndex = users.indexOfFirst { it.id == currUser.id }
                if (updatedIndex != -1) {
                    currentUserIndex = updatedIndex
                    _currentUser.value = users[updatedIndex]
                }
            }
            return
        }

        cancelTimer()
        currentSession = session
        activeUsers = users
        currentUserIndex = 0
        _state.value = TurnState.IDLE
        _elapsedSeconds.value = 0L
        _remainingSeconds.value = 0L
        _totalDurationSeconds.value = 0L
        isWarningTriggered = false
        if (users.isNotEmpty()) {
            _currentUser.value = users[0]
            scope.launch {
                _events.emit(TurnEvent.UserChanged(users[0]))
            }
        } else {
            _currentUser.value = null
        }
    }

    fun setTargetDuration(seconds: Long, isOpenMode: Boolean) {
        _targetDurationSeconds.value = seconds
        _isTargetOpenMode.value = isOpenMode
    }

    // Start a Turn
    fun startTurn(durationSec: Long, isOpenMode: Boolean) {
        cancelTimer()
        val user = _currentUser.value ?: return

        _elapsedSeconds.value = 0L
        isWarningTriggered = false
        
        if (isOpenMode) {
            _totalDurationSeconds.value = 0L
            _remainingSeconds.value = 0L
            _state.value = TurnState.OPEN_MODE
        } else {
            _totalDurationSeconds.value = durationSec
            _remainingSeconds.value = durationSec
            _state.value = TurnState.RUNNING
        }

        scope.launch {
            _events.emit(TurnEvent.Started)
        }
        startTimerTicker(isOpenMode)
    }

    // Pause Turn
    fun pauseTurn() {
        if (_state.value == TurnState.RUNNING || _state.value == TurnState.OPEN_MODE) {
            cancelTimer()
            val oldState = _state.value
            _state.value = TurnState.PAUSED
            scope.launch {
                _events.emit(TurnEvent.Paused)
            }
        }
    }

    // Resume Turn
    fun resumeTurn() {
        if (_state.value == TurnState.PAUSED) {
            val isOpenMode = _totalDurationSeconds.value == 0L
            if (isOpenMode) {
                _state.value = TurnState.OPEN_MODE
            } else {
                _state.value = TurnState.RUNNING
            }
            scope.launch {
                _events.emit(TurnEvent.Resumed)
            }
            startTimerTicker(isOpenMode)
        }
    }

    // Complete/Finish Turn and rotate
    fun finishTurn(saveRecord: Boolean = true) {
        val user = _currentUser.value ?: return
        val elapsed = _elapsedSeconds.value
        cancelTimer()
        _state.value = TurnState.IDLE

        scope.launch {
            if (saveRecord) {
                _events.emit(TurnEvent.ManuallySaved(user, elapsed))
            }
            // Rotate to next user
            rotateToNextUser()
        }
    }

    // Cancel / Discard current Turn
    fun cancelTurn() {
        cancelTimer()
        _state.value = TurnState.IDLE
        _elapsedSeconds.value = 0L
        _remainingSeconds.value = 0L
        isWarningTriggered = false
        scope.launch {
            _events.emit(TurnEvent.Canceled)
        }
    }

    // Move to Next User manually
    fun rotateToNextUser() {
        if (activeUsers.isEmpty()) return
        currentUserIndex = (currentUserIndex + 1) % activeUsers.size
        val nextUser = activeUsers[currentUserIndex]
        _currentUser.value = nextUser
        _state.value = TurnState.IDLE
        _elapsedSeconds.value = 0L
        _remainingSeconds.value = 0L
        isWarningTriggered = false

        scope.launch {
            _events.emit(TurnEvent.UserChanged(nextUser))
        }
    }

    // Directly select a user
    fun selectUser(userId: Int) {
        if (activeUsers.isEmpty()) return
        val index = activeUsers.indexOfFirst { it.id == userId }
        if (index != -1) {
            cancelTimer()
            currentUserIndex = index
            val selected = activeUsers[index]
            _currentUser.value = selected
            _state.value = TurnState.IDLE
            _elapsedSeconds.value = 0L
            _remainingSeconds.value = 0L
            isWarningTriggered = false

            scope.launch {
                _events.emit(TurnEvent.UserChanged(selected))
            }
        }
    }

    private fun startTimerTicker(isOpenMode: Boolean) {
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _elapsedSeconds.value += 1

                if (isOpenMode) {
                    _remainingSeconds.value = 0L
                } else {
                    if (_remainingSeconds.value > 0) {
                        _remainingSeconds.value -= 1

                        // Warning trigger when 10 seconds or less remain
                        if (_remainingSeconds.value <= 10 && _remainingSeconds.value > 0 && !isWarningTriggered) {
                            _events.emit(TurnEvent.WarningTick)
                        }

                        if (_remainingSeconds.value == 0L) {
                            // Turn finished automatically (Time expired)!
                            isWarningTriggered = false
                            val user = _currentUser.value
                            val elapsed = _elapsedSeconds.value
                            _state.value = TurnState.FINISHED
                            if (user != null) {
                                _events.emit(TurnEvent.TimeExpired(user, elapsed))
                            }
                            rotateToNextUser()
                            break
                        }
                    }
                }
            }
        }
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    // Recovery mechanism - Restore engine state
    fun restoreState(
        session: SessionEntity,
        users: List<UserEntity>,
        activeUserId: Int,
        savedState: TurnState,
        elapsed: Long,
        remaining: Long,
        totalDuration: Long
    ) {
        currentSession = session
        activeUsers = users
        val index = users.indexOfFirst { it.id == activeUserId }
        currentUserIndex = if (index != -1) index else 0
        if (users.isNotEmpty()) {
            _currentUser.value = users[currentUserIndex]
        }
        _state.value = savedState
        _elapsedSeconds.value = elapsed
        _remainingSeconds.value = remaining
        _totalDurationSeconds.value = totalDuration

        if (savedState == TurnState.RUNNING || savedState == TurnState.OPEN_MODE) {
            startTimerTicker(savedState == TurnState.OPEN_MODE)
        }
    }
}
