package com.swipeswipe.app.data

import android.net.Uri

data class Photo(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val dateTakenMillis: Long,
    val isFavorite: Boolean = false,
    val isVideo: Boolean = false,
)
