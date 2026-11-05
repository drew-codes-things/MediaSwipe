package com.mediaswipe.app.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

const val IMAGE_MIME_TYPE = "image/*"
const val VIDEO_MIME_TYPE = "video/*"

fun pickPhotosIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT)
    .addCategory(Intent.CATEGORY_OPENABLE)
    .setType(IMAGE_MIME_TYPE)
    .putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(IMAGE_MIME_TYPE, VIDEO_MIME_TYPE))
    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

fun installedPhotoSource(packageManager: PackageManager): String? {
    val galleries = packageManager.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY),
        0,
    ).map { resolved ->
        val app = resolved.activityInfo.applicationInfo
        GalleryApp(app.packageName, isSystemApp = app.flags and ApplicationInfo.FLAG_SYSTEM != 0)
    }
    val pickers = packageManager.queryIntentActivities(pickPhotosIntent(), 0)
        .map { it.activityInfo.packageName }
        .toSet()
    return photoSourcePackage(galleries, pickers)
}
