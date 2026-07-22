package com.swipeswipe.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.swipeswipe.app.ui.CompletionReason
import com.swipeswipe.app.ui.formatBytes

@Composable
fun CompletionScreen(
    reason: CompletionReason,
    successCount: Int,
    failureCount: Int,
    bytesFreed: Long,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "All done!", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        if (successCount > 0) {
            when (reason) {
                CompletionReason.MOVED_TO_RECENTLY_DELETED ->
                    Text("$successCount photo(s) moved to Recently Deleted.")
                CompletionReason.PERMANENTLY_DELETED ->
                    Text("Freed ${formatBytes(bytesFreed)} across $successCount photos.")
            }
        } else {
            Text("No photos needed cleanup this round.")
        }
        if (failureCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$failureCount photo(s) couldn't be removed.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Continue") }
    }
}
