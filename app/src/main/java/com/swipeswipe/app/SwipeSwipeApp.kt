package com.swipeswipe.app

import android.content.IntentSender
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.swipeswipe.app.data.Photo
import com.swipeswipe.app.ui.PhotoCleanerViewModel
import com.swipeswipe.app.ui.Screen
import com.swipeswipe.app.ui.screens.CompletionScreen
import com.swipeswipe.app.ui.screens.PermissionScreen
import com.swipeswipe.app.ui.screens.RecentlyDeletedScreen
import com.swipeswipe.app.ui.screens.ReviewScreen
import com.swipeswipe.app.ui.screens.SessionSetupScreen
import com.swipeswipe.app.ui.screens.SwipeDeckScreen

@Composable
fun SwipeSwipeApp(
    viewModel: PhotoCleanerViewModel,
    onRequestPermission: () -> Unit,
    onLaunchDeleteConfirmation: (IntentSender) -> Unit,
    onLaunchFavoriteConfirmation: (IntentSender) -> Unit,
    onShare: (Photo) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // These two are assumed mutually exclusive: delete-confirm only fires
    // from the Recently Deleted screen's "Delete Forever" action,
    // favorite-confirm only from Deck, so at most one system consent
    // dialog is ever in flight at a time.
    LaunchedEffect(uiState.pendingDeleteConfirmation) {
        uiState.pendingDeleteConfirmation?.let { onLaunchDeleteConfirmation(it.intentSender) }
    }
    LaunchedEffect(uiState.pendingFavoriteConfirmation) {
        uiState.pendingFavoriteConfirmation?.let { onLaunchFavoriteConfirmation(it.intentSender) }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (uiState.screen) {
            Screen.Permission -> PermissionScreen(
                onRequestPermission = onRequestPermission,
                permissionDenied = uiState.permissionDenied,
            )
            Screen.SessionSetup -> SessionSetupScreen(
                uiState = uiState,
                onChooseSize = viewModel::onSessionSizeChosen,
                onResetProgress = viewModel::onResetSortingProgress,
                onOpenRecentlyDeleted = viewModel::onOpenRecentlyDeleted,
            )
            Screen.Deck -> SwipeDeckScreen(
                uiState = uiState,
                onSwipeLeft = viewModel::onSwipeLeft,
                onSwipeRight = viewModel::onSwipeRight,
                onBack = viewModel::onUndo,
                onBackPressed = viewModel::onBackFromDeck,
                onToggleFavorite = viewModel::onToggleFavorite,
                onShare = onShare,
            )
            Screen.Review -> ReviewScreen(
                uiState = uiState,
                onUnstage = viewModel::onUnstageFromReview,
                onMoveToRecentlyDeleted = viewModel::onMoveToRecentlyDeleted,
            )
            Screen.RecentlyDeleted -> RecentlyDeletedScreen(
                uiState = uiState,
                onRestore = viewModel::onRestoreFromRecentlyDeleted,
                onDeleteForever = viewModel::onDeleteForever,
                onBackPressed = viewModel::onBackFromRecentlyDeleted,
            )
            Screen.Completion -> CompletionScreen(
                reason = uiState.completionReason,
                successCount = uiState.completionSuccessCount,
                failureCount = uiState.completionFailureCount,
                bytesFreed = uiState.completionBytesFreed,
                onDone = viewModel::onCompletionAcknowledged,
            )
        }
    }
}
