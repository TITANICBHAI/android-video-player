package com.localvideoplayer.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.localvideoplayer.model.FolderItem
import com.localvideoplayer.model.SortOrder
import com.localvideoplayer.model.VideoItem
import com.localvideoplayer.model.WatchProgress
import com.localvideoplayer.repository.VideoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ViewMode { ALL_VIDEOS, FOLDERS, FOLDER_DETAIL, RECENT }

class LibraryViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = VideoRepository(app)

    private val _videos = MutableLiveData<List<VideoItem>>(emptyList())
    val videos: LiveData<List<VideoItem>> = _videos

    private val _folders = MutableLiveData<List<FolderItem>>(emptyList())
    val folders: LiveData<List<FolderItem>> = _folders

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _viewMode = MutableLiveData(ViewMode.ALL_VIDEOS)
    val viewMode: LiveData<ViewMode> = _viewMode

    private val _selectedBucketId = MutableLiveData<Long?>(null)
    val selectedBucketId: LiveData<Long?> = _selectedBucketId

    private val _selectedBucketName = MutableLiveData<String?>()
    val selectedBucketName: LiveData<String?> = _selectedBucketName

    private val _sortOrder = MutableLiveData(SortOrder.DATE_ADDED_DESC)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    private val _searchQuery = MutableLiveData("")
    val searchQuery: LiveData<String> = _searchQuery

    val watchProgress = repo.getAllWatchProgress()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadAllVideos()
    }

    fun loadAllVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            _videos.value = repo.getAllVideos(
                sortOrder = _sortOrder.value ?: SortOrder.DATE_ADDED_DESC,
                query = _searchQuery.value ?: ""
            )
            _isLoading.value = false
        }
    }

    fun loadFolders() {
        viewModelScope.launch {
            _isLoading.value = true
            _folders.value = repo.getFolders()
            _isLoading.value = false
        }
    }

    fun loadFolderVideos(bucketId: Long, bucketName: String) {
        _selectedBucketId.value = bucketId
        _selectedBucketName.value = bucketName
        viewModelScope.launch {
            _isLoading.value = true
            _videos.value = repo.getVideosByFolder(
                bucketId, _sortOrder.value ?: SortOrder.DATE_ADDED_DESC
            )
            _isLoading.value = false
        }
    }

    fun setViewMode(mode: ViewMode) {
        _viewMode.value = mode
        when (mode) {
            ViewMode.ALL_VIDEOS -> loadAllVideos()
            ViewMode.FOLDERS -> loadFolders()
            ViewMode.RECENT -> loadAllVideos()
            ViewMode.FOLDER_DETAIL -> {}
        }
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        val bucketId = _selectedBucketId.value
        if (bucketId != null) {
            loadFolderVideos(bucketId, _selectedBucketName.value ?: "")
        } else {
            loadAllVideos()
        }
    }

    fun setSearchQuery(q: String) {
        _searchQuery.value = q
        loadAllVideos()
    }

    fun getProgressFor(videoId: Long, progresses: List<WatchProgress>): WatchProgress? =
        progresses.find { it.videoId == videoId }
}
