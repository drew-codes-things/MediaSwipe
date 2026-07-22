package com.swipeswipe.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.swipeswipe.app.data.Photo
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sign

private const val SWIPE_THRESHOLD_FRACTION = 0.35f
private const val ROTATION_DIVISOR = 20f
private const val MAX_ROTATION_DEG = 16f
private const val OFF_SCREEN_MULTIPLIER = 1.5f
private const val STAMP_ALPHA_MULTIPLIER = 3f

@Composable
fun SwipeableCard(
    key: Any,
    photo: Photo,
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val offsetX = remember(key) { Animatable(0f) }
        val scope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = offsetX.value
                    rotationZ = (offsetX.value / ROTATION_DIVISOR).coerceIn(-MAX_ROTATION_DEG, MAX_ROTATION_DEG)
                }
                .pointerInput(key) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch { offsetX.snapTo(offsetX.value + dragAmount.x) }
                        },
                        onDragEnd = {
                            val threshold = screenWidthPx * SWIPE_THRESHOLD_FRACTION
                            scope.launch {
                                if (abs(offsetX.value) > threshold) {
                                    val target = sign(offsetX.value) * screenWidthPx * OFF_SCREEN_MULTIPLIER
                                    offsetX.animateTo(target, tween(250))
                                    if (offsetX.value > 0) onSwipedRight() else onSwipedLeft()
                                } else {
                                    offsetX.animateTo(0f, spring())
                                }
                            }
                        },
                    )
                },
        ) {
            PhotoCard(photo = photo, modifier = Modifier.fillMaxSize())

            SwipeStamp(
                text = "DELETE",
                alignment = Alignment.TopStart,
                color = MaterialTheme.colorScheme.error,
                alphaProvider = { (-offsetX.value / screenWidthPx * STAMP_ALPHA_MULTIPLIER).coerceIn(0f, 1f) },
            )
            SwipeStamp(
                text = "KEEP",
                alignment = Alignment.TopEnd,
                color = MaterialTheme.colorScheme.primary,
                alphaProvider = { (offsetX.value / screenWidthPx * STAMP_ALPHA_MULTIPLIER).coerceIn(0f, 1f) },
            )
        }
    }
}

@Composable
private fun BoxScope.SwipeStamp(
    text: String,
    alignment: Alignment,
    color: Color,
    alphaProvider: () -> Float,
) {
    Text(
        text = text,
        color = color,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier
            .align(alignment)
            .graphicsLayer { alpha = alphaProvider() }
            .padding(16.dp)
            .border(BorderStroke(2.dp, color), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
