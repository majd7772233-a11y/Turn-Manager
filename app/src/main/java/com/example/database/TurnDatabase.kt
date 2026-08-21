package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val colorHex: String,
    val avatarEmoji: String,
    val soundPack: String = "Default",
    val turnsCount: Int = 0,
    val totalDurationSeconds: Long = 0L,
    val averageDurationSeconds: Long = 0L,
    val customSoundUri: String? = null
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // STUDY, PLAY, WORK, MEETING, POMODORO
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val userId: Int,
    val userName: String,
    val userColorHex: String,
    val actionType: String, // START, PAUSE, RESUME, FINISH, CANCEL
    val timestamp: Long = System.currentTimeMillis(),
    val elapsedSeconds: Long = 0L
)

@Entity(tableName = "achievements", primaryKeys = ["userId", "id"])
data class AchievementEntity(
    val userId: Int,
    val id: String, // e.g. "first_turn", "turns_10", "turns_100", "hours_50", "pause_master", "open_time_beast"
    val title: String,
    val description: String,
    val isUnlocked: Boolean = false,
    val progress: Int = 0,
    val maxProgress: Int = 1
)

@Entity(tableName = "preferences")
data class PreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "deleted_history")
data class DeletedHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val originalId: Int = 0,
    val sessionId: Int,
    val userId: Int,
    val userName: String,
    val userColorHex: String,
    val actionType: String,
    val originalTimestamp: Long,
    val elapsedSeconds: Long,
    val deletedAt: Long = System.currentTimeMillis(),
    val deletionType: String, // "SINGLE" or "BULK"
    val bulkBatchId: String? = null, // e.g. "bulk_1720000000"
    val sessionName: String? = null
)

// --- DAO ---

@Dao
interface TurnDao {
    // Users
    @Query("SELECT * FROM users ORDER BY id ASC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users ORDER BY id ASC")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Int): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    // Sessions
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessionsFlow(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Int)

    @Update
    suspend fun updateSession(session: SessionEntity)

    // History
    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    fun getAllHistoryFlow(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE sessionId = :sessionId ORDER BY timestamp DESC")
    fun getHistoryBySessionFlow(sessionId: Int): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history ORDER BY timestamp DESC")
    suspend fun getAllHistory(): List<HistoryEntity>

    @Query("SELECT * FROM history WHERE id = :id")
    suspend fun getHistoryById(id: Int): HistoryEntity?

    @Query("SELECT * FROM history WHERE sessionId = :sessionId")
    suspend fun getHistoryBySession(sessionId: Int): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: HistoryEntity): Long

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun deleteHistoryById(id: Int)

    @Query("DELETE FROM history WHERE sessionId = :sessionId")
    suspend fun deleteHistoryBySession(sessionId: Int)

    // Deleted History (Trash / Secret Archive)
    @Query("SELECT * FROM deleted_history ORDER BY deletedAt DESC")
    fun getAllDeletedHistoryFlow(): Flow<List<DeletedHistoryEntity>>

    @Query("SELECT * FROM deleted_history WHERE deletionType = :type ORDER BY deletedAt DESC")
    fun getDeletedHistoryByTypeFlow(type: String): Flow<List<DeletedHistoryEntity>>

    @Query("SELECT * FROM deleted_history ORDER BY deletedAt DESC")
    suspend fun getAllDeletedHistory(): List<DeletedHistoryEntity>

    @Query("SELECT * FROM deleted_history WHERE id = :id")
    suspend fun getDeletedHistoryById(id: Int): DeletedHistoryEntity?

    @Query("SELECT * FROM deleted_history WHERE bulkBatchId = :batchId")
    suspend fun getDeletedHistoryByBatchId(batchId: String): List<DeletedHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedHistory(item: DeletedHistoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedHistoryList(items: List<DeletedHistoryEntity>)

    @Query("DELETE FROM deleted_history WHERE id = :id")
    suspend fun deleteDeletedHistoryById(id: Int)

    @Query("DELETE FROM deleted_history WHERE bulkBatchId = :batchId")
    suspend fun deleteDeletedHistoryByBatchId(batchId: String)

    @Query("DELETE FROM deleted_history")
    suspend fun deleteAllDeletedHistory()

    // Achievements
    @Query("SELECT * FROM achievements")
    fun getAllAchievementsFlow(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements")
    suspend fun getAllAchievements(): List<AchievementEntity>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    fun getAchievementsByUserFlow(userId: Int): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE userId = :userId")
    suspend fun getAchievementsByUser(userId: Int): List<AchievementEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    // Preferences
    @Query("SELECT * FROM preferences")
    fun getAllPreferencesFlow(): Flow<List<PreferenceEntity>>

    @Query("SELECT * FROM preferences WHERE `key` = :key")
    suspend fun getPreference(key: String): PreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreference(preference: PreferenceEntity)
}

// --- DATABASE ---

@Database(
    entities = [
        UserEntity::class,
        SessionEntity::class,
        HistoryEntity::class,
        DeletedHistoryEntity::class,
        AchievementEntity::class,
        PreferenceEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TurnDatabase : RoomDatabase() {
    abstract fun turnDao(): TurnDao
}
