package com.mediaswipe.app.update

import android.content.Context
import androidx.core.content.edit

const val LATEST_RELEASE_API = "https://api.github.com/repos/drew-codes-things/MediaSwipe/releases/latest"
const val RELEASES_PAGE = "https://github.com/drew-codes-things/MediaSwipe/releases/latest"

class UpdatePreferences(context: Context) : UpdateNoticeStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var checksEnabled: Boolean
        get() = prefs.getBoolean(KEY_CHECKS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_CHECKS_ENABLED, value) }

    override var dismissedVersion: String
        get() = prefs.getString(KEY_DISMISSED_VERSION, "").orEmpty()
        set(value) = prefs.edit { putString(KEY_DISMISSED_VERSION, value) }

    private companion object {
        const val PREFS_NAME = "updates"
        const val KEY_CHECKS_ENABLED = "checks_enabled"
        const val KEY_DISMISSED_VERSION = "dismissed_version"
    }
}
