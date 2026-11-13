package com.mediaswipe.app.data

import android.content.Context
import androidx.core.content.edit

class SharedPreferencesPendingDeletionStore(
    context: Context,
    prefsName: String = PENDING_DELETION_PREFS,
) : PendingDeletionStore {

    private val prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

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

    companion object {
        const val PENDING_DELETION_PREFS = "pending_deletion"
        const val AWAITING_REVIEW_PREFS = "awaiting_review"
        private const val KEY_PENDING_IDS = "pending_ids"
    }
}
