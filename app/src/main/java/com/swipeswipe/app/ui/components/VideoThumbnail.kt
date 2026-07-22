package com.swipeswipe.app.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.os.CancellationSignal
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val THUMBNAIL_SIZE = Size(720, 720)

@Composable
fun VideoThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Bitmap?> { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    val result = context.contentResolver.loadThumbnail(uri, THUMBNAIL_SIZE, signal)
                    if (continuation.isActive) continuation.resumeWith(Result.success(result))
                } catch (t: Exception) {
                    if (continuation.isActive) continuation.resumeWith(Result.success(null))
                }
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Video",
            tint = Color.White,
            modifier = Modifier
                .size(48.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape),
        )
    }
}
