package com.mediaswipe.app

import android.content.IntentSender
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaswipe.app.data.Photo
import com.mediaswipe.app.ui.PhotoCleanerViewModel
import com.mediaswipe.app.ui.Screen
import com.mediaswipe.app.ui.screens.CompletionScreen
import com.mediaswipe.app.ui.screens.PermissionScreen
import com.mediaswipe.app.ui.screens.RecentlyDeletedScreen
import com.mediaswipe.app.ui.screens.ReviewScreen
import com.mediaswipe.app.ui.screens.SessionSetupScreen
import com.mediaswipe.app.ui.screens.SwipeDeckScreen

@Composable
fun MediaSwipeApp(
    viewModel: PhotoCleanerViewModel,
    onRequestPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onPickPhotos: () -> Unit,
    onLaunchDeleteConfirmation: (IntentSender) -> Unit,
    onLaunchFavoriteConfirmation: (IntentSender) -> Unit,
    onShare: (Photo) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.pendingDeleteConfirmation) {
        uiState.pendingDeleteConfirmation?.let { onLaunchDeleteConfirmation(it.intentSender) }
    }
    LaunchedEffect(uiState.pendingFavoriteConfirmation) {
        uiState.pendingFavoriteConfirmation?.let { onLaunchFavoriteConfirmation(it.intentSender) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        when (uiState.screen) {
            Screen.Permission -> PermissionScreen(
                onRequestPermission = onRequestPermission,
                onOpenAppSettings = onOpenAppSettings,
                permissionDenied = uiState.permissionDenied,
            )
            Screen.SessionSetup -> SessionSetupScreen(
                uiState = uiState,
                onChooseSize = viewModel::onSessionSizeChosen,
                onResetProgress = viewModel::onResetSortingProgress,
                onOpenRecentlyDeleted = viewModel::onOpenRecentlyDeleted,
                onPickPhotos = onPickPhotos,
            )
            Screen.Deck -> SwipeDeckScreen(
                uiState = uiState,
                onSwipeLeft = viewModel::onSwipeLeft,
                onSwipeRight = viewModel::onSwipeRight,
                onUndo = viewModel::onUndo,
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
                onMoveToPhoneBin = viewModel::onMoveToPhoneBin,
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
