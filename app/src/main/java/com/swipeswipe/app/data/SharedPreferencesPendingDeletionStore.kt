package com.swipeswipe.app.data

import android.content.Context
import androidx.core.content.edit

class SharedPreferencesPendingDeletionStore(context: Context) : PendingDeletionStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun pendingIds(): Set<Long> =
        prefs.getStringSet(KEY_PENDING_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    override fun stage(id: Long) {
        val current = prefs.getStringSet(KEY_PENDING_IDS, emptySet()) ?: emptySet()
        prefs.edit { putStringSet(KEY_PENDING_IDS, current + id.toString()) }
    }

    override fun unstage(id: Long) {
        val current = prefs.getStringSet(KEY_PENDING_IDS, emptySet()) ?: emptySet()
        prefs.edit { putStringSet(KEY_PENDING_IDS, current - id.toString()) }
    }

    private companion object {
        const val PREFS_NAME = "pending_deletion"
        const val KEY_PENDING_IDS = "pending_ids"
    }
}
