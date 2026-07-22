package com.swipeswipe.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.swipeswipe.app.data.DeleteOutcome
import com.swipeswipe.app.data.FavoriteOutcome
import com.swipeswipe.app.data.PendingDeletionStore
import com.swipeswipe.app.data.Photo
import com.swipeswipe.app.data.PhotoDeleter
import com.swipeswipe.app.data.PhotoFavoriter
import com.swipeswipe.app.data.PhotoRepository
import com.swipeswipe.app.data.SortTracker
import com.swipeswipe.app.ui.CompletionReason
import com.swipeswipe.app.ui.PhotoCleanerViewModel
import com.swipeswipe.app.ui.Screen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

private fun fakePhoto(id: Long, sizeBytes: Long = 1_000L): Photo = Photo(
    id = id,
    uri = Uri.parse("content://media/external/images/media/$id"),
    displayName = "photo_$id.jpg",
    sizeBytes = sizeBytes,
    dateTakenMillis = id,
)

private class FakePhotoRepository(private val photos: List<Photo>) : PhotoRepository {
    override suspend fun loadPhotos(): List<Photo> = photos
}

private class FakePhotoDeleter(private val outcome: DeleteOutcome) : PhotoDeleter {
    var lastRequestedUris: List<Uri>? = null

    override suspend fun requestDelete(uris: List<Uri>): DeleteOutcome {
        lastRequestedUris = uris
        return outcome
    }

    override suspend fun onConfirmationResult(
        resultOk: Boolean,
        remaining: List<Uri>,
        successSoFar: Int,
        failureSoFar: Int,
    ): DeleteOutcome = if (resultOk) {
        DeleteOutcome.Done(successCount = successSoFar + remaining.size, failureCount = failureSoFar)
    } else {
        DeleteOutcome.Cancelled(successSoFar, failureSoFar + remaining.size)
    }
}

private class FakeSortTracker(private val sorted: MutableSet<Long> = mutableSetOf()) : SortTracker {
    override fun sortedIds(): Set<Long> = sorted.toSet()
    override fun markSorted(id: Long) { sorted += id }
    override fun markUnsorted(id: Long) { sorted -= id }
    override fun resetAll() { sorted.clear() }
}

private class FakePhotoFavoriter(private val outcome: FavoriteOutcome? = null) : PhotoFavoriter {
    var lastRequestedUri: Uri? = null
    var lastRequestedFavorite: Boolean? = null

    override suspend fun requestFavorite(uri: Uri, favorite: Boolean): FavoriteOutcome {
        lastRequestedUri = uri
        lastRequestedFavorite = favorite
        return outcome ?: FavoriteOutcome.Done(uri, favorite)
    }

    override suspend fun onConfirmationResult(resultOk: Boolean, uri: Uri, favorite: Boolean): FavoriteOutcome =
        if (resultOk) FavoriteOutcome.Done(uri, favorite) else FavoriteOutcome.Cancelled(uri)
}

private class FakePendingDeletionStore(private val pending: MutableSet<Long> = mutableSetOf()) : PendingDeletionStore {
    override fun pendingIds(): Set<Long> = pending.toSet()
    override fun stage(id: Long) { pending += id }
    override fun unstage(id: Long) { pending -= id }
}

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PhotoCleanerViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        photos: List<Photo>,
        deleter: PhotoDeleter = FakePhotoDeleter(DeleteOutcome.Done(0, 0)),
        sortTracker: SortTracker = FakeSortTracker(),
        photoFavoriter: PhotoFavoriter = FakePhotoFavoriter(),
        pendingDeletionStore: PendingDeletionStore = FakePendingDeletionStore(),
    ) = PhotoCleanerViewModel(FakePhotoRepository(photos), deleter, sortTracker, photoFavoriter, pendingDeletionStore)

    /** Gets a freshly-loaded ViewModel all the way to the swipe deck, all-unsorted session. */
    private fun readyDeck(vm: PhotoCleanerViewModel) {
        vm.onPermissionGranted()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onSessionSizeChosen(null)
    }

    @Test
    fun `swipe left stages photo, swipe right does not`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        assertTrue(vm.uiState.value.stagedForDeletion.contains(photos[0]))
        assertEquals(1, vm.uiState.value.currentIndex)

        vm.onSwipeRight(photos[1])
        assertTrue(!vm.uiState.value.stagedForDeletion.contains(photos[1]))
    }

    @Test
    fun `undo restores index and unstages`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onUndo()

        assertEquals(0, vm.uiState.value.currentIndex)
        assertTrue(!vm.uiState.value.stagedForDeletion.contains(photos[0]))
        assertEquals(Screen.Deck, vm.uiState.value.screen)
    }

    @Test
    fun `unstage from review only touches staged set`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeLeft(photos[1])
        assertEquals(Screen.Review, vm.uiState.value.screen)

        vm.onUnstageFromReview(photos[0])
        assertEquals(setOf(photos[1]), vm.uiState.value.stagedForDeletion)
        assertEquals(2, vm.uiState.value.currentIndex)
    }

    @Test
    fun `moving to recently deleted stages ids, delete forever passes exactly those uris`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val deleter = FakePhotoDeleter(DeleteOutcome.Done(1, 0))
        val vm = viewModel(photos, deleter)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeRight(photos[1])
        vm.onMoveToRecentlyDeleted()

        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertTrue(vm.uiState.value.stagedForDeletion.isEmpty())
        assertEquals(Screen.Completion, vm.uiState.value.screen)
        assertEquals(CompletionReason.MOVED_TO_RECENTLY_DELETED, vm.uiState.value.completionReason)

        vm.onDeleteForever()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(photos[0].uri), deleter.lastRequestedUris)
        assertTrue(vm.uiState.value.pendingDeletionIds.isEmpty())
        assertEquals(Screen.Completion, vm.uiState.value.screen)
        assertEquals(CompletionReason.PERMANENTLY_DELETED, vm.uiState.value.completionReason)
    }

    @Test
    fun `cancelled delete confirmation returns to recently deleted and preserves pending ids`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1))
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val fakeIntentSender = PendingIntent.getActivity(
            context,
            0,
            Intent(),
            PendingIntent.FLAG_IMMUTABLE,
        ).intentSender
        val pendingOutcome = DeleteOutcome.NeedsConfirmation(
            intentSender = fakeIntentSender,
            remaining = listOf(photos[0].uri),
            successSoFar = 0,
            failureSoFar = 0,
        )
        val deleter = FakePhotoDeleter(pendingOutcome)
        val vm = viewModel(photos, deleter)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onMoveToRecentlyDeleted()
        vm.onDeleteForever()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.pendingDeleteConfirmation != null)

        vm.onDeleteConfirmationResult(resultOk = false)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Screen.RecentlyDeleted, vm.uiState.value.screen)
        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertNull(vm.uiState.value.pendingDeleteConfirmation)
    }

    @Test
    fun `restoring from recently deleted removes it from pending but keeps it sorted`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onMoveToRecentlyDeleted()
        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)

        vm.onRestoreFromRecentlyDeleted(photos[0])
        assertTrue(vm.uiState.value.pendingDeletionIds.isEmpty())
        assertTrue(vm.uiState.value.sortedPhotoIds.contains(1L))
    }

    @Test
    fun `back from recently deleted returns to session setup`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onOpenRecentlyDeleted()
        assertEquals(Screen.RecentlyDeleted, vm.uiState.value.screen)

        vm.onBackFromRecentlyDeleted()
        assertEquals(Screen.SessionSetup, vm.uiState.value.screen)
    }

    @Test
    fun `swiping marks a photo sorted, undo unsorts it`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val tracker = FakeSortTracker()
        val vm = viewModel(photos, sortTracker = tracker)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        assertEquals(setOf(1L), vm.uiState.value.sortedPhotoIds)
        assertFalse(vm.uiState.value.unsortedPhotos.contains(photos[0]))
        assertEquals(setOf(1L), tracker.sortedIds())

        vm.onUndo()
        assertTrue(vm.uiState.value.sortedPhotoIds.isEmpty())
        assertTrue(vm.uiState.value.unsortedPhotos.contains(photos[0]))
        assertTrue(tracker.sortedIds().isEmpty())
    }

    @Test
    fun `sorted status persists across a simulated app restart`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val sharedTracker = FakeSortTracker()

        val firstRun = viewModel(photos, sortTracker = sharedTracker)
        readyDeck(firstRun)
        firstRun.onSwipeRight(photos[0])

        // Simulate a restart: a brand new ViewModel instance backed by the same tracker.
        val secondRun = viewModel(photos, sortTracker = sharedTracker)
        secondRun.onPermissionGranted()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, secondRun.uiState.value.sortedCount)
        assertEquals(1, secondRun.uiState.value.unsortedCount)
        assertFalse(secondRun.uiState.value.unsortedPhotos.contains(photos[0]))
    }

    @Test
    fun `toggling favorite updates state and requests the right uri`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1))
        val favoriter = FakePhotoFavoriter()
        val vm = viewModel(photos, photoFavoriter = favoriter)
        readyDeck(vm)

        vm.onToggleFavorite()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(photos[0].uri, favoriter.lastRequestedUri)
        assertEquals(true, favoriter.lastRequestedFavorite)
        assertTrue(vm.uiState.value.currentPhoto?.isFavorite == true)
    }

    @Test
    fun `back from deck goes to session setup when nothing staged, review when something is`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onBackFromDeck()
        assertEquals(Screen.SessionSetup, vm.uiState.value.screen)

        vm.onSessionSizeChosen(null)
        vm.onSwipeLeft(photos[0])
        vm.onBackFromDeck()
        assertEquals(Screen.Review, vm.uiState.value.screen)
    }
}
