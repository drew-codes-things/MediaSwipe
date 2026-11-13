package com.mediaswipe.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.mediaswipe.app.data.DeleteOutcome
import com.mediaswipe.app.data.FavoriteOutcome
import com.mediaswipe.app.data.PendingDeletionStore
import com.mediaswipe.app.data.Photo
import com.mediaswipe.app.data.PhotoDeleter
import com.mediaswipe.app.data.PhotoFavoriter
import com.mediaswipe.app.data.PhotoRepository
import com.mediaswipe.app.data.SortTracker
import com.mediaswipe.app.ui.CompletionReason
import com.mediaswipe.app.ui.PhotoCleanerViewModel
import com.mediaswipe.app.ui.Screen
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
    override suspend fun loadPhotos(uris: List<Uri>): List<Photo> =
        uris.mapNotNull { picked -> photos.firstOrNull { it.uri == picked } }
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
        deletedSoFar: List<Uri>,
        failureSoFar: Int,
    ): DeleteOutcome = if (resultOk) {
        DeleteOutcome.Done(deleted = deletedSoFar + remaining, failureCount = failureSoFar)
    } else {
        DeleteOutcome.Cancelled(deletedSoFar, failureSoFar + remaining.size)
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
        deleter: PhotoDeleter = FakePhotoDeleter(DeleteOutcome.Done(emptyList(), 0)),
        sortTracker: SortTracker = FakeSortTracker(),
        photoFavoriter: PhotoFavoriter = FakePhotoFavoriter(),
        pendingDeletionStore: PendingDeletionStore = FakePendingDeletionStore(),
        awaitingReviewStore: PendingDeletionStore = FakePendingDeletionStore(),
    ) = PhotoCleanerViewModel(FakePhotoRepository(photos), deleter, sortTracker, photoFavoriter, pendingDeletionStore, awaitingReviewStore)

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
    fun `repeated undo steps back through several swipes in order`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2), fakePhoto(3))
        val vm = viewModel(photos)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeRight(photos[1])
        vm.onUndo()

        assertEquals(1, vm.uiState.value.currentIndex)
        assertTrue(vm.uiState.value.stagedForDeletion.contains(photos[0]))

        vm.onUndo()
        assertEquals(0, vm.uiState.value.currentIndex)
        assertTrue(vm.uiState.value.stagedForDeletion.isEmpty())

        vm.onUndo()
        assertEquals(0, vm.uiState.value.currentIndex)
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
        val deleter = FakePhotoDeleter(DeleteOutcome.Done(listOf(photos[0].uri), 0))
        val vm = viewModel(photos, deleter)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeRight(photos[1])
        vm.onMoveToRecentlyDeleted()

        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertTrue(vm.uiState.value.stagedForDeletion.isEmpty())
        assertEquals(Screen.Completion, vm.uiState.value.screen)
        assertEquals(CompletionReason.MOVED_TO_RECENTLY_DELETED, vm.uiState.value.completionReason)

        vm.onMoveToPhoneBin()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(listOf(photos[0].uri), deleter.lastRequestedUris)
        assertTrue(vm.uiState.value.pendingDeletionIds.isEmpty())
        assertEquals(Screen.Completion, vm.uiState.value.screen)
        assertEquals(CompletionReason.MOVED_TO_PHONE_BIN, vm.uiState.value.completionReason)
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
            deletedSoFar = emptyList(),
            failureSoFar = 0,
        )
        val deleter = FakePhotoDeleter(pendingOutcome)
        val vm = viewModel(photos, deleter)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onMoveToRecentlyDeleted()
        vm.onMoveToPhoneBin()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value.pendingDeleteConfirmation != null)

        vm.onDeleteConfirmationResult(resultOk = false)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Screen.RecentlyDeleted, vm.uiState.value.screen)
        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertNull(vm.uiState.value.pendingDeleteConfirmation)
    }

    @Test
    fun `partial delete keeps the photos that failed in recently deleted`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val deleter = FakePhotoDeleter(DeleteOutcome.Done(listOf(photos[1].uri), failureCount = 1))
        val vm = viewModel(photos, deleter)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeLeft(photos[1])
        vm.onMoveToRecentlyDeleted()
        vm.onMoveToPhoneBin()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertEquals(1, vm.uiState.value.completionSuccessCount)
        assertEquals(1, vm.uiState.value.completionFailureCount)
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
    fun `picked photos start a session of exactly those items and report cloud-only ones`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2), fakePhoto(3))
        val vm = viewModel(photos)
        vm.onPermissionGranted()
        dispatcher.scheduler.advanceUntilIdle()

        val cloudOnly = Uri.parse("content://media/picker/0/com.google.android.apps.photos.cloudpicker/media/99")
        vm.onPhotosPicked(listOf(photos[2].uri, photos[0].uri, cloudOnly))
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(Screen.Deck, vm.uiState.value.screen)
        assertEquals(listOf(3L, 1L), vm.uiState.value.sessionPhotos.map { it.id })
        assertTrue(vm.uiState.value.pickerNotice!!.startsWith("1 picked item"))

        vm.onPhotosPicked(listOf(cloudOnly))
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value.pickerNotice!!.startsWith("None of the picked"))
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

    @Test
    fun `delete swipes survive a restart before the review step`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2), fakePhoto(3))
        val sortTracker = FakeSortTracker()
        val awaitingReview = FakePendingDeletionStore()

        val firstRun = viewModel(photos, sortTracker = sortTracker, awaitingReviewStore = awaitingReview)
        readyDeck(firstRun)
        firstRun.onSwipeLeft(photos[0])
        firstRun.onSwipeRight(photos[1])

        val secondRun = viewModel(photos, sortTracker = sortTracker, awaitingReviewStore = awaitingReview)
        secondRun.onPermissionGranted()
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(setOf(photos[0]), secondRun.uiState.value.stagedForDeletion)
        assertEquals(Screen.Review, secondRun.uiState.value.screen)
    }

    @Test
    fun `undone and unstaged delete swipes are not brought back after a restart`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2), fakePhoto(3))
        val awaitingReview = FakePendingDeletionStore()

        val firstRun = viewModel(photos, awaitingReviewStore = awaitingReview)
        readyDeck(firstRun)
        firstRun.onSwipeLeft(photos[0])
        firstRun.onUndo()
        firstRun.onSwipeLeft(photos[0])
        firstRun.onSwipeLeft(photos[1])
        firstRun.onBackFromDeck()
        firstRun.onUnstageFromReview(photos[0])

        assertEquals(setOf(2L), awaitingReview.pendingIds())
    }

    @Test
    fun `moving to recently deleted empties the saved review list`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val pending = FakePendingDeletionStore()
        val awaitingReview = FakePendingDeletionStore()
        val vm = viewModel(photos, pendingDeletionStore = pending, awaitingReviewStore = awaitingReview)
        readyDeck(vm)

        vm.onSwipeLeft(photos[0])
        vm.onSwipeLeft(photos[1])
        vm.onMoveToRecentlyDeleted()

        assertTrue(awaitingReview.pendingIds().isEmpty())
        assertEquals(setOf(1L, 2L), pending.pendingIds())
    }

    @Test
    fun `a keep swipe takes a photo out of the delete queue`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val pending = FakePendingDeletionStore(mutableSetOf(1L))
        val deleter = FakePhotoDeleter(DeleteOutcome.Done(emptyList(), 0))
        val vm = viewModel(photos, deleter = deleter, sortTracker = FakeSortTracker(mutableSetOf(1L)), pendingDeletionStore = pending)
        vm.onPermissionGranted()
        dispatcher.scheduler.advanceUntilIdle()
        vm.onResetSortingProgress()
        vm.onSessionSizeChosen(null)

        vm.onSwipeRight(photos[0])

        assertFalse(vm.uiState.value.pendingDeletionIds.contains(1L))
        assertTrue(pending.pendingIds().isEmpty())
        vm.onOpenRecentlyDeleted()
        vm.onMoveToPhoneBin()
        dispatcher.scheduler.advanceUntilIdle()
        assertNull(deleter.lastRequestedUris)
    }

    @Test
    fun `undoing that keep swipe puts the photo back in the delete queue`() = runTest(dispatcher) {
        val photos = listOf(fakePhoto(1), fakePhoto(2))
        val pending = FakePendingDeletionStore(mutableSetOf(1L))
        val vm = viewModel(photos, pendingDeletionStore = pending)
        readyDeck(vm)

        vm.onSwipeRight(photos[0])
        vm.onUndo()

        assertEquals(setOf(1L), vm.uiState.value.pendingDeletionIds)
        assertEquals(setOf(1L), pending.pendingIds())
    }

    @Test
    fun `app data is left out of device backups because photo ids differ between phones`() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertEquals(0, context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
