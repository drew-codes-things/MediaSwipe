package com.mediaswipe.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryAppsTest {

    private val realmeGallery = GalleryApp("com.coloros.gallery3d", isSystemApp = true)
    private val googlePhotos = GalleryApp(GOOGLE_PHOTOS_PACKAGE, isSystemApp = true)
    private val downloadedGallery = GalleryApp("org.example.gallery", isSystemApp = false)

    @Test
    fun phonesOwnGalleryWinsOverGooglePhotos() {
        val source = photoSourcePackage(
            listOf(googlePhotos, realmeGallery),
            setOf(GOOGLE_PHOTOS_PACKAGE, "com.coloros.gallery3d"),
        )
        assertEquals("com.coloros.gallery3d", source)
    }

    @Test
    fun builtInGalleryWinsOverADownloadedOne() {
        val source = photoSourcePackage(
            listOf(downloadedGallery, realmeGallery),
            setOf("org.example.gallery", "com.coloros.gallery3d"),
        )
        assertEquals("com.coloros.gallery3d", source)
    }

    @Test
    fun googlePhotosIsUsedWhenThereIsNoOtherGallery() {
        val source = photoSourcePackage(listOf(googlePhotos), setOf(GOOGLE_PHOTOS_PACKAGE))
        assertEquals(GOOGLE_PHOTOS_PACKAGE, source)
    }

    @Test
    fun aGalleryThatCannotPickPhotosIsSkipped() {
        val source = photoSourcePackage(listOf(realmeGallery, googlePhotos), setOf(GOOGLE_PHOTOS_PACKAGE))
        assertEquals(GOOGLE_PHOTOS_PACKAGE, source)
    }

    @Test
    fun noSourceWhenNeitherCanPickPhotos() {
        assertNull(photoSourcePackage(listOf(realmeGallery, googlePhotos), emptySet()))
    }
}
