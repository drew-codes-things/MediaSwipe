package com.swipeswipe.app.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.swipeswipe.app.data.DeleteOutcome
import com.swipeswipe.app.data.FavoriteOutcome
import com.swipeswipe.app.data.Photo
import com.swipeswipe.app.data.PendingDeletionStore
import com.swipeswipe.app.data.PhotoDeleter
import com.swipeswipe.app.data.PhotoFavoriter
import com.swipeswipe.app.data.PhotoRepository
import com.swipeswipe.app.data.SortTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PhotoCleanerViewModel(
    private val photoRepository: PhotoRepository,
    private val photoDeleter: PhotoDeleter,
    private val sortTracker: SortTracker,
    private val photoFavoriter: PhotoFavoriter,
    private val pendingDeletionStore: PendingDeletionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PhotoCleanerUiState())
    val uiState: StateFlow<PhotoCleanerUiState> = _uiState.asStateFlow()

    private data class SwipeRecord(val photo: Photo, val staged: Boolean)

    private val swipeHistory = ArrayDeque<SwipeRecord>()

    fun onPermissionGranted() {
        _uiState.update { it.copy(permissionDenied = false) }
        // Guard against MainActivity re-invoking this on every rotation
        // (ViewModel survives config changes, but onCreate re-checks the
        // permission every time) — only auto-navigate on first grant.
        if (_uiState.value.screen != Screen.Permission) return
        if (_uiState.value.allPhotos.isNotEmpty()) {
            _uiState.update { it.copy(screen = Screen.SessionSetup) }
            return
        }
        loadPhotos()
    }

    fun onPermissionDenied() {
        _uiState.update { it.copy(permissionDenied = true) }
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingPhotos = true) }
            val photos = photoRepository.loadPhotos()
            val sortedIds = sortTracker.sortedIds()
            val pendingIds = pendingDeletionStore.pendingIds()
            _uiState.update {
                it.copy(
                    allPhotos = photos,
                    sortedPhotoIds = sortedIds,
                    pendingDeletionIds = pendingIds,
                    isLoadingPhotos = false,
                    screen = Screen.SessionSetup,
                )
            }
        }
    }

    fun onSessionSizeChosen(size: Int?) {
        _uiState.update { state ->
            val pool = state.unsortedPhotos
            val session = if (size == null) pool else pool.take(size)
            state.copy(
                sessionPhotos = session,
                currentIndex = 0,
                screen = Screen.Deck,
            )
        }
    }

    fun onResetSortingProgress() {
        sortTracker.resetAll()
        _uiState.update { it.copy(sortedPhotoIds = emptySet()) }
    }

    fun onSwipeRight(photo: Photo) = advance(photo, stage = false)

    fun onSwipeLeft(photo: Photo) = advance(photo, stage = true)

    private fun advance(photo: Photo, stage: Boolean) {
        swipeHistory.addLast(SwipeRecord(photo, stage))
        sortTracker.markSorted(photo.id)
        _uiState.update { state ->
            val staged = if (stage) state.stagedForDeletion + photo else state.stagedForDeletion
            val nextIndex = state.currentIndex + 1
            val deckExhausted = nextIndex >= state.sessionPhotos.size
            state.copy(
                stagedForDeletion = staged,
                sortedPhotoIds = state.sortedPhotoIds + photo.id,
                currentIndex = nextIndex,
                screen = if (deckExhausted) resolveAfterLeavingDeck(Screen.Completion, staged) else state.screen,
            )
        }
    }

    fun onUndo() {
        val last = swipeHistory.removeLastOrNull() ?: return
        sortTracker.markUnsorted(last.photo.id)
        _uiState.update { state ->
            val staged = if (last.staged) state.stagedForDeletion - last.photo else state.stagedForDeletion
            state.copy(
                stagedForDeletion = staged,
                sortedPhotoIds = state.sortedPhotoIds - last.photo.id,
                currentIndex = (state.currentIndex - 1).coerceAtLeast(0),
                screen = Screen.Deck,
            )
        }
    }

    /**
     * Called when the user backs out of the deck early (system back-press),
     * as opposed to `advance()` reaching the natural end of the deck. Shares
     * the "anything staged?" check with `advance()` but resolves the empty
     * case differently — bailing early with nothing staged should return to
     * the session picker, not claim the session is "done".
     */
    fun onBackFromDeck() {
        _uiState.update { state ->
            state.copy(screen = resolveAfterLeavingDeck(Screen.SessionSetup, state.stagedForDeletion))
        }
    }

    private fun resolveAfterLeavingDeck(emptyDestination: Screen, staged: Set<Photo>): Screen =
        if (staged.isEmpty()) emptyDestination else Screen.Review

    fun onUnstageFromReview(photo: Photo) {
        _uiState.update { state ->
            val staged = state.stagedForDeletion - photo
            state.copy(
                stagedForDeletion = staged,
                screen = if (staged.isEmpty()) Screen.Completion else Screen.Review,
            )
        }
    }

    /**
     * Review's confirm action no longer touches MediaStore at all — it just
     * moves the staged photos into our own Recently Deleted staging area.
     * Nothing is actually removed from the device until "Delete Forever" is
     * used from that screen, so no system consent dialog is needed here.
     */
    fun onMoveToRecentlyDeleted() {
        val staged = _uiState.value.stagedForDeletion
        if (staged.isEmpty()) return
        staged.forEach { pendingDeletionStore.stage(it.id) }
        _uiState.update { state ->
            state.copy(
                stagedForDeletion = emptySet(),
                pendingDeletionIds = state.pendingDeletionIds + staged.map { it.id },
                completionReason = CompletionReason.MOVED_TO_RECENTLY_DELETED,
                completionSuccessCount = staged.size,
                completionFailureCount = 0,
                completionBytesFreed = 0,
                screen = Screen.Completion,
            )
        }
    }

    fun onOpenRecentlyDeleted() {
        _uiState.update { it.copy(screen = Screen.RecentlyDeleted) }
    }

    fun onBackFromRecentlyDeleted() {
        _uiState.update { it.copy(screen = Screen.SessionSetup) }
    }

    fun onRestoreFromRecentlyDeleted(photo: Photo) {
        pendingDeletionStore.unstage(photo.id)
        _uiState.update { it.copy(pendingDeletionIds = it.pendingDeletionIds - photo.id) }
    }

    fun onDeleteForever() {
        val pending = _uiState.value.pendingDeletionPhotos
        if (pending.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            handleDeleteOutcome(photoDeleter.requestDelete(pending.map { it.uri }))
        }
    }

    fun onDeleteConfirmationResult(resultOk: Boolean) {
        val pending = _uiState.value.pendingDeleteConfirmation ?: return
        _uiState.update { it.copy(pendingDeleteConfirmation = null, isDeleting = true) }
        viewModelScope.launch {
            handleDeleteOutcome(
                photoDeleter.onConfirmationResult(
                    resultOk = resultOk,
                    remaining = pending.remaining,
                    successSoFar = pending.successSoFar,
                    failureSoFar = pending.failureSoFar,
                ),
            )
        }
    }

    fun onToggleFavorite() {
        val photo = _uiState.value.currentPhoto ?: return
        if (_uiState.value.pendingFavoriteConfirmation != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFavoriting = true) }
            handleFavoriteOutcome(photoFavoriter.requestFavorite(photo.uri, favorite = !photo.isFavorite))
        }
    }

    fun onFavoriteConfirmationResult(resultOk: Boolean) {
        val pending = _uiState.value.pendingFavoriteConfirmation ?: return
        _uiState.update { it.copy(pendingFavoriteConfirmation = null, isFavoriting = true) }
        viewModelScope.launch {
            handleFavoriteOutcome(
                photoFavoriter.onConfirmationResult(
                    resultOk = resultOk,
                    uri = pending.uri,
                    favorite = pending.favorite,
                ),
            )
        }
    }

    private fun handleFavoriteOutcome(outcome: FavoriteOutcome) {
        when (outcome) {
            is FavoriteOutcome.NeedsConfirmation -> _uiState.update {
                it.copy(isFavoriting = false, pendingFavoriteConfirmation = outcome)
            }
            is FavoriteOutcome.Done -> _uiState.update {
                it.copy(
                    isFavoriting = false,
                    pendingFavoriteConfirmation = null,
                    allPhotos = it.allPhotos.withFavoriteUpdated(outcome.uri, outcome.favorite),
                    sessionPhotos = it.sessionPhotos.withFavoriteUpdated(outcome.uri, outcome.favorite),
                )
            }
            is FavoriteOutcome.Cancelled, FavoriteOutcome.Unsupported, is FavoriteOutcome.Error ->
                _uiState.update { it.copy(isFavoriting = false, pendingFavoriteConfirmation = null) }
        }
    }

    private fun List<Photo>.withFavoriteUpdated(uri: Uri, favorite: Boolean): List<Photo> =
        map { if (it.uri == uri) it.copy(isFavorite = favorite) else it }

    fun onCompletionAcknowledged() {
        swipeHistory.clear()
        _uiState.update {
            it.copy(
                sessionPhotos = emptyList(),
                currentIndex = 0,
                stagedForDeletion = emptySet(),
                completionSuccessCount = 0,
                completionFailureCount = 0,
                completionBytesFreed = 0,
            )
        }
        loadPhotos()
    }

    private fun handleDeleteOutcome(outcome: DeleteOutcome) {
        when (outcome) {
            is DeleteOutcome.NeedsConfirmation -> _uiState.update {
                it.copy(isDeleting = false, pendingDeleteConfirmation = outcome)
            }
            is DeleteOutcome.Done -> finishDelete(outcome.successCount, outcome.failureCount)
            is DeleteOutcome.Cancelled -> finishDelete(outcome.successSoFar, outcome.failureSoFar, cancelled = true)
            is DeleteOutcome.Error -> _uiState.update { it.copy(isDeleting = false, pendingDeleteConfirmation = null) }
        }
    }

    private fun finishDelete(successCount: Int, failureCount: Int, cancelled: Boolean = false) {
        if (cancelled) {
            // Nothing was actually deleted — stay in Recently Deleted, not
            // Review (Review never touches the deleter anymore, so routing
            // there would be a dead end).
            _uiState.update {
                it.copy(isDeleting = false, pendingDeleteConfirmation = null, screen = Screen.RecentlyDeleted)
            }
            return
        }
        // Bytes-freed is an estimate: it assumes successes land at the head of
        // the pending list, which only matters for this summary stat, not
        // correctness of what actually got deleted.
        val deletedPhotos = _uiState.value.pendingDeletionPhotos.take(successCount)
        deletedPhotos.forEach { pendingDeletionStore.unstage(it.id) }
        val deletedIds = deletedPhotos.map { it.id }.toSet()
        _uiState.update { state ->
            state.copy(
                isDeleting = false,
                pendingDeleteConfirmation = null,
                pendingDeletionIds = state.pendingDeletionIds - deletedIds,
                completionReason = CompletionReason.PERMANENTLY_DELETED,
                completionSuccessCount = successCount,
                completionFailureCount = failureCount,
                completionBytesFreed = deletedPhotos.sumOf { it.sizeBytes },
                screen = Screen.Completion,
            )
        }
    }
}
