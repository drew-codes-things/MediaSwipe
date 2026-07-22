package com.swipeswipe.app.data

interface PendingDeletionStore {
    fun pendingIds(): Set<Long>
    fun stage(id: Long)
    fun unstage(id: Long)
}
