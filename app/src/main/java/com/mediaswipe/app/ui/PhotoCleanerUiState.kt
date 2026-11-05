package com.mediaswipe.app.ui

import com.mediaswipe.app.data.DeleteOutcome
import com.mediaswipe.app.data.FavoriteOutcome
import com.mediaswipe.app.data.Photo

enum class Screen { Permission, SessionSetup, Deck, Review, RecentlyDeleted, Completion }

enum class CompletionReason { MOVED_TO_RECENTLY_DELETED, MOVED_TO_PHONE_BIN }

data class PhotoCleanerUiState(
    val screen: Screen = Screen.Permission,
    val permissionDenied: Boolean = false,
    val isLoadingPhotos: Boolean = false,
    val allPhotos: List<Photo> = emptyList(),
    val sortedPhotoIds: Set<Long> = emptySet(),
    val pendingDeletionIds: Set<Long> = emptySet(),
    val sessionPhotos: List<Photo> = emptyList(),
    val currentIndex: Int = 0,
    val stagedForDeletion: Set<Photo> = linkedSetOf(),
    val isDeleting: Boolean = false,
    val pendingDeleteConfirmation: DeleteOutcome.NeedsConfirmation? = null,
    val isFavoriting: Boolean = false,
    val pendingFavoriteConfirmation: FavoriteOutcome.NeedsConfirmation? = null,
    val completionReason: CompletionReason = CompletionReason.MOVED_TO_PHONE_BIN,
    val completionSuccessCount: Int = 0,
    val completionFailureCount: Int = 0,
    val completionBytesFreed: Long = 0,
    val pickerNotice: String? = null,
) {
    val currentPhoto: Photo? get() = sessionPhotos.getOrNull(currentIndex)
    val bytesToFree: Long get() = stagedForDeletion.sumOf { it.sizeBytes }
    val countToFree: Int get() = stagedForDeletion.size

    val unsortedPhotos: List<Photo> get() = allPhotos.filter { it.id !in sortedPhotoIds }
    val sortedPhotos: List<Photo> get() = allPhotos.filter { it.id in sortedPhotoIds }
    val totalCount: Int get() = allPhotos.size
    val totalBytes: Long get() = allPhotos.sumOf { it.sizeBytes }
    val sortedCount: Int get() = sortedPhotos.size
    val sortedBytes: Long get() = sortedPhotos.sumOf { it.sizeBytes }
    val unsortedCount: Int get() = unsortedPhotos.size
    val unsortedBytes: Long get() = unsortedPhotos.sumOf { it.sizeBytes }

    val pendingDeletionPhotos: List<Photo> get() = allPhotos.filter { it.id in pendingDeletionIds }
    val pendingDeletionCount: Int get() = pendingDeletionPhotos.size
    val pendingDeletionBytes: Long get() = pendingDeletionPhotos.sumOf { it.sizeBytes }
}
