package com.swipeswipe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swipeswipe.app.ui.PhotoCleanerUiState
import com.swipeswipe.app.ui.formatBytes

private val SESSION_SIZE_OPTIONS = listOf(10, 25, 50, 100)

@Composable
fun SessionSetupScreen(
    uiState: PhotoCleanerUiState,
    onChooseSize: (Int?) -> Unit,
    onResetProgress: () -> Unit,
    onOpenRecentlyDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "DrewPhotoSwipe", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.isLoadingPhotos) {
            CircularProgressIndicator()
            return@Column
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Library", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${uiState.totalCount} photos total • ${formatBytes(uiState.totalBytes)}")
                Text("${uiState.sortedCount} sorted • ${formatBytes(uiState.sortedBytes)}")
                Text("${uiState.unsortedCount} left to sort • ${formatBytes(uiState.unsortedBytes)}")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState.unsortedCount == 0) {
            Text(
                "All caught up! Every photo has been sorted.",
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Text(
                "How many photos do you want to go through?",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SESSION_SIZE_OPTIONS.filter { it <= uiState.unsortedCount }.forEach { size ->
                    SuggestionChip(onClick = { onChooseSize(size) }, label = { Text("$size") })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onChooseSize(null) }) {
                Text("All ${uiState.unsortedCount}")
            }
        }

        if (uiState.pendingDeletionCount > 0) {
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onOpenRecentlyDeleted) {
                Text("Recently Deleted (${uiState.pendingDeletionCount})")
            }
        }

        if (uiState.sortedCount > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = { showResetConfirm = true }) {
                Text("Reset sorting progress")
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset sorting progress?") },
            text = {
                Text(
                    "This marks all ${uiState.sortedCount} sorted photos as unsorted again, so " +
                        "they'll reappear next time you go through your library. Anything already " +
                        "in Recently Deleted is not affected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetConfirm = false
                        onResetProgress()
                    },
                ) { Text("Reset") }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
            },
        )
    }
}
