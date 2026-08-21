package com.example.database

import android.content.Context
import androidx.room.Room
import com.example.repository.TurnRepository

object DatabaseModule {
    @Volatile
    private var database: TurnDatabase? = null
    
    @Volatile
    private var repository: TurnRepository? = null

    fun getDatabase(context: Context): TurnDatabase {
        return database ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                TurnDatabase::class.java,
                "turn_manager_db"
            )
            .fallbackToDestructiveMigration() // Simple for development and updates
            .build()
            database = instance
            instance
        }
    }

    fun getRepository(context: Context): TurnRepository {
        return repository ?: synchronized(this) {
            if (repository == null) {
                val dao = getDatabase(context).turnDao()
                repository = TurnRepository(dao)
            }
            repository!!
        }
    }
}
