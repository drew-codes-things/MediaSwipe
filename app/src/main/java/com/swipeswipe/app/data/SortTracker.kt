package com.swipeswipe.app.data

interface SortTracker {
    fun sortedIds(): Set<Long>
    fun markSorted(id: Long)
    fun markUnsorted(id: Long)
    fun resetAll()
}
