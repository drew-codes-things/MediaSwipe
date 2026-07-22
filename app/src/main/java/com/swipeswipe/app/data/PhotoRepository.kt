package com.swipeswipe.app.data

interface PhotoRepository {
    suspend fun loadPhotos(): List<Photo>
}
