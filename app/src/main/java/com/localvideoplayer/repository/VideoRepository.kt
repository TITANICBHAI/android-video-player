package com.localvideoplayer.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.localvideoplayer.model.FolderItem
import com.localvideoplayer.model.SortOrder
import com.localvideoplayer.model.VideoItem
import com.localvideoplayer.model.WatchProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class VideoRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val dao = db.watchProgressDao()

    suspend fun getAllVideos(
        sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC,
        query: String = ""
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DATE_MODIFIED,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.DATA
        )

        val orderBy = when (sortOrder) {
            SortOrder.NAME_ASC -> "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
            SortOrder.NAME_DESC -> "${MediaStore.Video.Media.DISPLAY_NAME} DESC"
            SortOrder.DATE_ADDED_DESC -> "${MediaStore.Video.Media.DATE_ADDED} DESC"
            SortOrder.DATE_ADDED_ASC -> "${MediaStore.Video.Media.DATE_ADDED} ASC"
            SortOrder.SIZE_DESC -> "${MediaStore.Video.Media.SIZE} DESC"
            SortOrder.SIZE_ASC -> "${MediaStore.Video.Media.SIZE} ASC"
            SortOrder.DURATION_DESC -> "${MediaStore.Video.Media.DURATION} DESC"
            SortOrder.DURATION_ASC -> "${MediaStore.Video.Media.DURATION} ASC"
        }

        val selection = if (query.isNotBlank()) {
            "${MediaStore.Video.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Video.Media.DURATION} > 0"
        } else {
            "${MediaStore.Video.Media.DURATION} > 0"
        }

        val selectionArgs = if (query.isNotBlank()) arrayOf("%$query%") else null

        val videos = mutableListOf<VideoItem>()

        context.contentResolver.query(collection, projection, selection, selectionArgs, orderBy)
            ?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val bucketIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    videos.add(
                        VideoItem(
                            id = id,
                            uri = uri,
                            displayName = cursor.getString(nameCol) ?: "",
                            title = cursor.getString(titleCol) ?: "",
                            duration = cursor.getLong(durationCol),
                            size = cursor.getLong(sizeCol),
                            dateAdded = cursor.getLong(dateAddedCol),
                            dateModified = cursor.getLong(dateModCol),
                            width = cursor.getInt(widthCol),
                            height = cursor.getInt(heightCol),
                            mimeType = cursor.getString(mimeCol) ?: "",
                            bucketName = cursor.getString(bucketNameCol) ?: "Unknown",
                            bucketId = cursor.getLong(bucketIdCol),
                            data = cursor.getString(dataCol) ?: ""
                        )
                    )
                }
            }

        videos
    }

    suspend fun getVideosByFolder(bucketId: Long, sortOrder: SortOrder = SortOrder.DATE_ADDED_DESC): List<VideoItem> =
        withContext(Dispatchers.IO) {
            getAllVideos(sortOrder).filter { it.bucketId == bucketId }
        }

    suspend fun getFolders(): List<FolderItem> = withContext(Dispatchers.IO) {
        val all = getAllVideos()
        all.groupBy { it.bucketId }.map { (bucketId, videos) ->
            FolderItem(
                bucketId = bucketId,
                bucketName = videos.first().bucketName,
                count = videos.size,
                firstVideoUri = videos.firstOrNull()?.uri
            )
        }.sortedBy { it.bucketName }
    }

    suspend fun getWatchProgress(videoId: Long): WatchProgress? = dao.getProgress(videoId)

    fun getAllWatchProgress(): Flow<List<WatchProgress>> = dao.getAllProgress()

    suspend fun saveWatchProgress(videoId: Long, position: Long, duration: Long) {
        dao.upsert(
            WatchProgress(
                videoId = videoId,
                position = position,
                duration = duration,
                lastWatched = System.currentTimeMillis(),
                completed = duration > 0 && position >= duration * 0.95
            )
        )
    }

    suspend fun clearProgress(videoId: Long) = dao.delete(videoId)

    suspend fun clearAllProgress() = dao.deleteAll()
}
