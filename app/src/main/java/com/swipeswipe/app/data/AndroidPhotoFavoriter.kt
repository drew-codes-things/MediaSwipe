package com.swipeswipe.app.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AndroidPhotoFavoriter"

/**
 * Both MediaStore.createFavoriteRequest and the IS_FAVORITE column were
 * added in API 30 (one level above the trash APIs) - there is no fallback
 * column to write pre-30, so favouriting is simply unsupported there.
 */
class AndroidPhotoFavoriter(private val context: Context) : PhotoFavoriter {

    private val resolver get() = context.contentResolver

    override suspend fun requestFavorite(uri: Uri, favorite: Boolean): FavoriteOutcome =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                requestFavoriteApi30Plus(uri, favorite)
            } else {
                FavoriteOutcome.Unsupported
            }
        }

    override suspend fun onConfirmationResult(
        resultOk: Boolean,
        uri: Uri,
        favorite: Boolean,
    ): FavoriteOutcome = withContext(Dispatchers.IO) {
        if (!resultOk) {
            FavoriteOutcome.Cancelled(uri)
        } else {
            FavoriteOutcome.Done(uri, favorite)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun requestFavoriteApi30Plus(uri: Uri, favorite: Boolean): FavoriteOutcome = try {
        val pendingIntent = MediaStore.createFavoriteRequest(resolver, listOf(uri), favorite)
        FavoriteOutcome.NeedsConfirmation(
            intentSender = pendingIntent.intentSender,
            uri = uri,
            favorite = favorite,
        )
    } catch (t: Exception) {
        Log.e(TAG, "createFavoriteRequest failed for $uri", t)
        FavoriteOutcome.Error(t)
    }
}
