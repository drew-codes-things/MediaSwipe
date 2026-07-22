package com.swipeswipe.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.swipeswipe.app.ui.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentlyDeletedScreen(
    uiState: PhotoCleanerUiState,
    onRestore: (Photo) -> Unit,
    onDeleteForever: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)

    var showDeleteForeverConfirm by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Recently Deleted") },
            navigationIcon = {
                IconButton(onClick = onBackPressed) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Text(
            text = "${uiState.pendingDeletionCount} photos • ${formatBytes(uiState.pendingDeletionBytes)}",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.bodyMedium,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            contentPadding = PaddingValues(8.dp),
        ) {
            items(uiState.pendingDeletionPhotos, key = { it.id }) { photo ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f),
                ) {
                    PhotoCard(photo = photo, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { onRestore(photo) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Restore")
                    }
                }
            }
        }

        Button(
            onClick = { showDeleteForeverConfirm = true },
            enabled = uiState.pendingDeletionCount > 0 && !uiState.isDeleting,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            if (uiState.isDeleting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Delete ${uiState.pendingDeletionCount} photos forever")
            }
        }
    }

    if (showDeleteForeverConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteForeverConfirm = false },
            title = { Text("Delete forever?") },
            text = {
                Text(
                    "This permanently deletes ${uiState.pendingDeletionCount} photo(s). " +
                        "This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteForeverConfirm = false
                        onDeleteForever()
                    },
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteForeverConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
