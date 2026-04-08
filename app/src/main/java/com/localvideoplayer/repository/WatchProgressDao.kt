package com.localvideoplayer.repository

import androidx.room.*
import com.localvideoplayer.model.WatchProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchProgressDao {
    @Query("SELECT * FROM watch_progress WHERE videoId = :videoId")
    suspend fun getProgress(videoId: Long): WatchProgress?

    @Query("SELECT * FROM watch_progress ORDER BY lastWatched DESC LIMIT 50")
    fun getAllProgress(): Flow<List<WatchProgress>>

    @Upsert
    suspend fun upsert(progress: WatchProgress)

    @Query("DELETE FROM watch_progress WHERE videoId = :videoId")
    suspend fun delete(videoId: Long)

    @Query("DELETE FROM watch_progress")
    suspend fun deleteAll()
}
