package com.mediaswipe.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun aGalleryThatOnlyReadsTheRequestTypeStillShowsVideosAndPhotos() {
        assertTrue("videos hidden by request type $GALLERY_REQUEST_TYPE", typeAllows(GALLERY_REQUEST_TYPE, "video/mp4"))
        assertTrue("photos hidden by request type $GALLERY_REQUEST_TYPE", typeAllows(GALLERY_REQUEST_TYPE, "image/jpeg"))
    }

    @Test
    fun aGalleryThatReadsTheMimeListIsLimitedToPhotosAndVideos() {
        assertEquals(listOf("image/*", "video/*"), GALLERY_MIME_TYPES)
    }

    private fun typeAllows(pattern: String, mimeType: String): Boolean {
        val (wantedKind, wantedSubtype) = pattern.split('/')
        val (kind, subtype) = mimeType.split('/')
        return (wantedKind == "*" || wantedKind == kind) && (wantedSubtype == "*" || wantedSubtype == subtype)
    }
}
