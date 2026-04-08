package com.localvideoplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "watch_progress")
data class WatchProgress(
    @PrimaryKey val videoId: Long,
    val position: Long,         // milliseconds
    val duration: Long,         // milliseconds
    val lastWatched: Long,      // epoch milliseconds
    val completed: Boolean = false
)
