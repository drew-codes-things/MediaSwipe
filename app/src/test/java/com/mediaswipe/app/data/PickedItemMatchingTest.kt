package com.mediaswipe.app.data

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

private const val GALLERY_AUTHORITY = "org.example.gallery"
private const val SHARED_NAME = "IMG_0001.jpg"
private const val SHARED_SIZE = 4_096L
private val PICKED_FROM_GALLERY: Uri = Uri.parse("content://$GALLERY_AUTHORITY/item/7")

data class FakeMediaRow(val id: Long, val name: String, val size: Long)

class FakeMediaProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val columns = projection ?: arrayOf(MediaStore.MediaColumns._ID)
        val cursor = MatrixCursor(columns)
        val rows = if (uri.pathSegments.contains("images")) images else emptyList()
        rows.filter { selectionArgs == null || (it.name == selectionArgs[0] && it.size.toString() == selectionArgs[1]) }
            .forEach { row -> cursor.addRow(columns.map { valueFor(row, it) }) }
        return cursor
    }

    private fun valueFor(row: FakeMediaRow, column: String): Any = when (column) {
        MediaStore.MediaColumns._ID -> row.id
        MediaStore.MediaColumns.DISPLAY_NAME -> row.name
        MediaStore.MediaColumns.SIZE -> row.size
        MediaStore.MediaColumns.DATE_TAKEN -> row.id
        else -> 0
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        var images: List<FakeMediaRow> = emptyList()
    }
}

class FakeGalleryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor =
        MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)).apply { addRow(arrayOf<Any>(SHARED_NAME, SHARED_SIZE)) }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}

@RunWith(RobolectricTestRunner::class)
class PickedItemMatchingTest {

    @Before
    fun setUp() {
        Robolectric.setupContentProvider(FakeMediaProvider::class.java, MediaStore.AUTHORITY)
        Robolectric.setupContentProvider(FakeGalleryProvider::class.java, GALLERY_AUTHORITY)
    }

    private fun repository() = MediaStorePhotoRepository(ApplicationProvider.getApplicationContext())

    @Test
    fun aPickedItemMatchingTwoFilesByNameAndSizeIsSkippedRatherThanGuessed() = runTest {
        FakeMediaProvider.images = listOf(FakeMediaRow(10, SHARED_NAME, SHARED_SIZE), FakeMediaRow(11, SHARED_NAME, SHARED_SIZE))
        assertEquals(emptyList<Photo>(), repository().loadPhotos(listOf(PICKED_FROM_GALLERY)))
    }

    @Test
    fun aPickedItemMatchingExactlyOneFileIsUsed() = runTest {
        FakeMediaProvider.images = listOf(FakeMediaRow(10, SHARED_NAME, SHARED_SIZE), FakeMediaRow(11, "other.jpg", SHARED_SIZE))
        assertEquals(listOf(10L), repository().loadPhotos(listOf(PICKED_FROM_GALLERY)).map { it.id })
    }
}
