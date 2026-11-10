package com.mediaswipe.app.data

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

fun pickPhotosIntent(): Intent = Intent(Intent.ACTION_GET_CONTENT)
    .addCategory(Intent.CATEGORY_OPENABLE)
    .setType(GALLERY_REQUEST_TYPE)
    .putExtra(Intent.EXTRA_MIME_TYPES, GALLERY_MIME_TYPES.toTypedArray())
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
