package com.mediaswipe.app.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStorePhotoRepository(private val context: Context) : PhotoRepository {

    override suspend fun loadPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false) +
            queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true)
        photos.sortedByDescending { it.dateTakenMillis }
    }

    override suspend fun loadPhotos(uris: List<Uri>): List<Photo> = withContext(Dispatchers.IO) {
        uris.mapNotNull { picked -> resolvePickedItem(picked) }.distinctBy { it.id }
    }

    private fun resolvePickedItem(picked: Uri): Photo? {
        if (picked.authority != MediaStore.AUTHORITY) {
            val mediaUri = runCatching { MediaStore.getMediaUri(context, picked) }.getOrNull()
            return mediaUri?.let(::resolvePickedItem) ?: findByNameAndSize(picked)
        }
        val segments = picked.pathSegments
        if (segments.firstOrNull() != "picker") {
            return querySingle(picked, isVideo = segments.contains("video"))
        }
        if (LOCAL_PICKER_PROVIDER !in segments) return null
        val id = picked.lastPathSegment?.toLongOrNull() ?: return null
        val (name, size) = openableNameAndSize(picked) ?: return null
        for ((contentUri, isVideo) in listOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI to false,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI to true,
        )) {
            val candidate = querySingle(ContentUris.withAppendedId(contentUri, id), isVideo) ?: continue
            if (candidate.displayName == name && candidate.sizeBytes == size) return candidate
        }
        return null
    }

    private fun findByNameAndSize(picked: Uri): Photo? {
        val (name, size) = openableNameAndSize(picked) ?: return null
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ? AND ${MediaStore.MediaColumns.SIZE} = ?"
        val arguments = arrayOf(name, size.toString())
        val candidates = queryMedia(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, isVideo = false, selection, arguments) +
            queryMedia(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, isVideo = true, selection, arguments)
        return candidates.singleOrNull()
    }

    private fun openableNameAndSize(uri: Uri): Pair<String, Long>? {
        val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) ?: ""
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE))
                name to size
            }
        }.getOrNull()
    }

    private fun querySingle(itemUri: Uri, isVideo: Boolean): Photo? = runCatching {
        val contentUri = if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val id = ContentUris.parseId(itemUri)
        queryMedia(contentUri, isVideo, selection = "${MediaStore.MediaColumns._ID} = ?", selectionArgs = arrayOf(id.toString()))
            .firstOrNull()
    }.getOrNull()

    private fun queryMedia(
        contentUri: Uri,
        isVideo: Boolean,
        selection: String? = null,
        selectionArgs: Array<String>? = null,
    ): List<Photo> {
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

        context.contentResolver.query(contentUri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
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

    private fun resolveDateTakenMillis(cursor: Cursor, dateTakenColumn: Int, dateAddedColumn: Int): Long {
        val dateTaken = cursor.getLong(dateTakenColumn)
        if (dateTaken > 0) return dateTaken
        return cursor.getLong(dateAddedColumn) * 1000
    }

    private companion object {
        const val LOCAL_PICKER_PROVIDER = "com.android.providers.media.photopicker"
    }
}
