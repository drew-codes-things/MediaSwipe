package com.swipeswipe.app.data

import android.content.IntentSender
import android.net.Uri

interface PhotoFavoriter {

    suspend fun requestFavorite(uri: Uri, favorite: Boolean): FavoriteOutcome

    suspend fun onConfirmationResult(resultOk: Boolean, uri: Uri, favorite: Boolean): FavoriteOutcome
}

sealed interface FavoriteOutcome {

    data class NeedsConfirmation(val intentSender: IntentSender, val uri: Uri, val favorite: Boolean) : FavoriteOutcome

    data class Done(val uri: Uri, val favorite: Boolean) : FavoriteOutcome

    data class Cancelled(val uri: Uri) : FavoriteOutcome

    /** No fallback exists below API 30 - no MediaStore column to write. */
    data object Unsupported : FavoriteOutcome

    data class Error(val cause: Throwable) : FavoriteOutcome
}
