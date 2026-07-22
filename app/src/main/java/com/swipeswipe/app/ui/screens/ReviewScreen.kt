package com.swipeswipe.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swipeswipe.app.data.Photo
import com.swipeswipe.app.ui.PhotoCleanerUiState
import com.swipeswipe.app.ui.components.PhotoCard
import com.swipeswipe.app.ui.formatBytes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    uiState: PhotoCleanerUiState,
    onUnstage: (Photo) -> Unit,
    onMoveToRecentlyDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Review (${uiState.countToFree})") })

        Text(
            text = "${uiState.countToFree} photos staged • ${formatBytes(uiState.bytesToFree)}",
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
            items(uiState.stagedForDeletion.toList(), key = { it.id }) { photo ->
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f),
                ) {
                    PhotoCard(photo = photo, modifier = Modifier.fillMaxSize())
                    IconButton(
                        onClick = { onUnstage(photo) },
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = "Keep this photo instead")
                    }
                }
            }
        }

        Button(
            onClick = onMoveToRecentlyDeleted,
            enabled = uiState.countToFree > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text("Move ${uiState.countToFree} photos to Recently Deleted")
        }
    }
}
