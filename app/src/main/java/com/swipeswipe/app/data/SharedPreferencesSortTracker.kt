package com.swipeswipe.app.data

import android.content.Context
import androidx.core.content.edit

class SharedPreferencesSortTracker(context: Context) : SortTracker {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun sortedIds(): Set<Long> =
        prefs.getStringSet(KEY_SORTED_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

    override fun markSorted(id: Long) {
        val current = prefs.getStringSet(KEY_SORTED_IDS, emptySet()) ?: emptySet()
        prefs.edit { putStringSet(KEY_SORTED_IDS, current + id.toString()) }
    }

    override fun markUnsorted(id: Long) {
        val current = prefs.getStringSet(KEY_SORTED_IDS, emptySet()) ?: emptySet()
        prefs.edit { putStringSet(KEY_SORTED_IDS, current - id.toString()) }
    }

    override fun resetAll() {
        prefs.edit { remove(KEY_SORTED_IDS) }
    }

    private companion object {
        const val PREFS_NAME = "sorted_photos"
        const val KEY_SORTED_IDS = "sorted_ids"
    }
}
