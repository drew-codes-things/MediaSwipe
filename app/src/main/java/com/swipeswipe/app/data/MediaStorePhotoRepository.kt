package com.swipeswipe.app.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePhotoRepository(private val context: Context) : PhotoRepository {

    override suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false) +
            queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        photos.sortedByDescending { it.dateTakenMillis }
    }

    private fun queryMedia(contentUri: Uri, isVideo: Boolean): List<Photo> {
        val photos = mutableListOf<Photo>()
        val includeFavoriteColumn = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
        val projection = buildList {
            add(MediaStore.MediaColumns._ID)
            add(MediaStore.MediaColumns.DISPLAY_NAME)
            add(MediaStore.MediaColumns.SIZE)
            add(MediaStore.MediaColumns.DATE_TAKEN)
            add(MediaStore.MediaColumns.DATE_ADDED)
            if (includeFavoriteColumn) add(MediaStore.MediaColumns.IS_FAVORITE)
        }.toTypedArray()
        val sortOrder = "${MediaStore.MediaColumns.DATE_TAKEN} DESC"

        context.contentResolver.query(contentUri, projection, null, null, sortOrder)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val favoriteColumn = if (includeFavoriteColumn) {
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.IS_FAVORITE)
            } else {
                null
            }

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                photos += Photo(
                    id = id,
                    uri = ContentUris.withAppendedId(contentUri, id),
                    displayName = cursor.getString(nameColumn) ?: "",
                    sizeBytes = cursor.getLong(sizeColumn),
                    dateTakenMillis = resolveDateTakenMillis(cursor, dateTakenColumn, dateAddedColumn),
                    isFavorite = favoriteColumn?.let { cursor.getInt(it) != 0 } ?: false,
                    isVideo = isVideo,
                )
            }
        }
        return photos
    }

    /**
     * DATE_TAKEN is frequently 0/missing for video files (no EXIF-equivalent
     * field), which would otherwise dump every such video to the tail of a
     * date-descending sort regardless of actual recency. DATE_ADDED is in
     * seconds, not milliseconds, unlike DATE_TAKEN — must convert.
     */
    private fun resolveDateTakenMillis(cursor: Cursor, dateTakenColumn: Int, dateAddedColumn: Int): Long {
        val dateTaken = cursor.getLong(dateTakenColumn)
        if (dateTaken > 0) return dateTaken
        return cursor.getLong(dateAddedColumn) * 1000
    }
}
