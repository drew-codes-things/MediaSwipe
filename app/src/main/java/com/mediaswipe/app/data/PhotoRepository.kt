package com.mediaswipe.app.data

import android.net.Uri

interface PhotoRepository {
    suspend fun loadPhotos(): List<Photo>

    suspend fun loadPhotos(uris: List<Uri>): List<Photo>
}
