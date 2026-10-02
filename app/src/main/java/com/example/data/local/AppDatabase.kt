package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.DownloadRecordDao
import com.example.data.local.dao.SavedPinDao
import com.example.data.local.dao.SearchHistoryDao
import com.example.data.local.entity.DownloadRecordEntity
import com.example.data.local.entity.SavedPinEntity
import com.example.data.local.entity.SearchHistoryEntity

@Database(
    entities = [
        SavedPinEntity::class,
        DownloadRecordEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun savedPinDao(): SavedPinDao
    abstract fun downloadRecordDao(): DownloadRecordDao
    abstract fun searchHistoryDao(): SearchHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shreepins.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
