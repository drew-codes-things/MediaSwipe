package com.swipeswipe.app.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swipeswipe.app.data.Photo
import com.swipeswipe.app.ui.PhotoCleanerUiState
import com.swipeswipe.app.ui.components.PhotoCard
import com.swipeswipe.app.ui.components.SwipeableCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeDeckScreen(
    uiState: PhotoCleanerUiState,
    onSwipeLeft: (Photo) -> Unit,
    onSwipeRight: (Photo) -> Unit,
    onBack: () -> Unit,
    onBackPressed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: (Photo) -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)

    var showFavoriteConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "${(uiState.currentIndex + 1).coerceAtMost(uiState.sessionPhotos.size)} / ${uiState.sessionPhotos.size}",
                )
            },
        )

        Text(
            text = "Swipe left to delete, right to keep",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isLoadingPhotos -> CircularProgressIndicator()
                uiState.allPhotos.isEmpty() -> Text(
                    "No photos found on this device.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                uiState.sessionPhotos.isEmpty() -> Text(
                    "Nothing to sort right now.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                else -> {
                    val nextPhoto = uiState.sessionPhotos.getOrNull(uiState.currentIndex + 1)
                    val currentPhoto = uiState.currentPhoto

                    nextPhoto?.let { photo ->
                        PhotoCard(photo = photo, modifier = Modifier.fillMaxSize())
                    }
                    currentPhoto?.let { photo ->
                        SwipeableCard(
                            key = photo.id,
                            photo = photo,
                            onSwipedLeft = { onSwipeLeft(photo) },
                            onSwipedRight = { onSwipeRight(photo) },
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            val currentPhoto = uiState.currentPhoto
            val favoritesSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

            ActionButton(
                label = "Back",
                onClick = onBack,
                enabled = uiState.currentIndex > 0,
                primary = false,
            ) {
                Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Back")
            }

            ActionButton(
                label = "Delete",
                onClick = { currentPhoto?.let(onSwipeLeft) },
                enabled = currentPhoto != null,
                primary = true,
            ) {
                Icon(Icons.Default.Close, contentDescription = "Delete")
            }

            if (favoritesSupported) {
                ActionButton(
                    label = "Favorite",
                    onClick = { showFavoriteConfirm = true },
                    enabled = currentPhoto != null &&
                        !uiState.isFavoriting &&
                        uiState.pendingFavoriteConfirmation == null,
                    primary = false,
                ) {
                    if (currentPhoto?.isFavorite == true) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite")
                    } else {
                        Icon(Icons.Outlined.StarBorder, contentDescription = "Favorite")
                    }
                }
            }

            ActionButton(
                label = "Keep",
                onClick = { currentPhoto?.let(onSwipeRight) },
                enabled = currentPhoto != null,
                primary = true,
            ) {
                Icon(Icons.Default.Favorite, contentDescription = "Keep")
            }

            ActionButton(
                label = "Share",
                onClick = { currentPhoto?.let(onShare) },
                enabled = currentPhoto != null,
                primary = false,
            ) {
                Icon(Icons.Default.Share, contentDescription = "Share")
            }
        }
    }

    if (showFavoriteConfirm) {
        val currentPhoto = uiState.currentPhoto
        val alreadyFavorite = currentPhoto?.isFavorite == true
        AlertDialog(
            onDismissRequest = { showFavoriteConfirm = false },
            title = { Text(if (alreadyFavorite) "Remove favorite?" else "Add to favorites?") },
            text = {
                Text(
                    if (alreadyFavorite) {
                        "Are you sure you want to remove this photo from your favorites?"
                    } else {
                        "Are you sure you want to favorite this photo?"
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showFavoriteConfirm = false
                        onToggleFavorite()
                    },
                ) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { showFavoriteConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
    primary: Boolean,
    icon: @Composable () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (primary) {
            FilledIconButton(onClick = onClick, enabled = enabled, content = icon)
        } else {
            FilledTonalIconButton(onClick = onClick, enabled = enabled, content = icon)
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
