package com.mediaswipe.app.data

const val GOOGLE_PHOTOS_PACKAGE = "com.google.android.apps.photos"

data class GalleryApp(val packageName: String, val isSystemApp: Boolean)

fun phoneGalleryPackage(galleryApps: List<GalleryApp>, photoPickerPackages: Set<String>): String? =
    galleryApps
        .filter { it.packageName != GOOGLE_PHOTOS_PACKAGE && it.packageName in photoPickerPackages }
        .sortedByDescending { it.isSystemApp }
        .firstOrNull()
        ?.packageName

fun photoSourcePackage(galleryApps: List<GalleryApp>, photoPickerPackages: Set<String>): String? =
    phoneGalleryPackage(galleryApps, photoPickerPackages)
        ?: GOOGLE_PHOTOS_PACKAGE.takeIf { it in photoPickerPackages }
