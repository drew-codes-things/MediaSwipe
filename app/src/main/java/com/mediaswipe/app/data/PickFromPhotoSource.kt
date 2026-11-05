package com.mediaswipe.app.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract

class PickFromPhotoSource : ActivityResultContract<String, List<Uri>>() {
    override fun createIntent(context: Context, input: String): Intent = pickPhotosIntent().setPackage(input)

    override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        if (resultCode != Activity.RESULT_OK || intent == null) return emptyList()
        val clip = intent.clipData ?: return listOfNotNull(intent.data)
        return (0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri }
    }
}
