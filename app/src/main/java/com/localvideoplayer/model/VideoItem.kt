package com.localvideoplayer.model

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val title: String,
    val duration: Long,        // milliseconds
    val size: Long,            // bytes
    val dateAdded: Long,       // epoch seconds
    val dateModified: Long,    // epoch seconds
    val width: Int,
    val height: Int,
    val mimeType: String,
    val bucketName: String,    // folder name
    val bucketId: Long,
    val data: String           // file path
) {
    val durationFormatted: String get() {
        val totalSeconds = duration / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }

    val sizeFormatted: String get() {
        return when {
            size >= 1024 * 1024 * 1024 -> "%.2f GB".format(size / (1024.0 * 1024 * 1024))
            size >= 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024))
            else -> "%.1f KB".format(size / 1024.0)
        }
    }

    val resolutionLabel: String get() = when {
        height >= 2160 -> "4K"
        height >= 1440 -> "1440p"
        height >= 1080 -> "1080p"
        height >= 720  -> "720p"
        height >= 480  -> "480p"
        else           -> "${height}p"
    }
}

data class FolderItem(
    val bucketId: Long,
    val bucketName: String,
    val count: Int,
    val firstVideoUri: Uri?
)

enum class SortOrder {
    NAME_ASC, NAME_DESC,
    DATE_ADDED_DESC, DATE_ADDED_ASC,
    SIZE_DESC, SIZE_ASC,
    DURATION_DESC, DURATION_ASC
}
