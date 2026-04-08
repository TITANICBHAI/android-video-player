package com.localvideoplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localvideoplayer.model.WatchProgress
import com.localvideoplayer.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VideoRepository(app)
    private var saveJob: Job? = null

    var currentVideoId: Long = -1L
    var savedProgress: WatchProgress? = null

    suspend fun loadProgress(videoId: Long): WatchProgress? {
        currentVideoId = videoId
        savedProgress = repo.getWatchProgress(videoId)
        return savedProgress
    }

    fun startPeriodicSave(getPosition: () -> Long, getDuration: () -> Long) {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            while (isActive) {
                delay(5_000)
                val pos = getPosition()
                val dur = getDuration()
                if (currentVideoId >= 0 && dur > 0) {
                    repo.saveWatchProgress(currentVideoId, pos, dur)
                }
            }
        }
    }

    fun stopPeriodicSave() {
        saveJob?.cancel()
    }

    fun saveProgressNow(position: Long, duration: Long) {
        viewModelScope.launch {
            if (currentVideoId >= 0 && duration > 0) {
                repo.saveWatchProgress(currentVideoId, position, duration)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveJob?.cancel()
    }
}
