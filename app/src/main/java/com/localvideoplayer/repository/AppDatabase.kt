package com.localvideoplayer.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.localvideoplayer.model.WatchProgress

@Database(entities = [WatchProgress::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun watchProgressDao(): WatchProgressDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_player.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
        }
    }
}
