package com.mediaswipe.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private const val GLOW_RADIUS = 1400f

private val DarkColors = darkColorScheme(
    primary = SwipeRed,
    onPrimary = Color.White,
    secondary = SwipeRedDark,
    onSecondary = Color.White,
    background = SwipeBackground,
    onBackground = SwipeOnDark,
    surface = SwipeSurface,
    onSurface = SwipeOnDark,
    error = SwipeRedDeep,
    onError = Color.White,
)

@Composable
fun GradientBackground(content: @Composable () -> Unit) {
    val base = Brush.linearGradient(
        colors = listOf(GradientTop, GradientMiddle, GradientBottom),
        start = Offset.Zero,
        end = Offset(0f, Float.POSITIVE_INFINITY),
    )
    val glow = Brush.radialGradient(
        colors = listOf(GradientGlow, Color.Transparent),
        center = Offset.Zero,
        radius = GLOW_RADIUS,
    )
    Box(Modifier.fillMaxSize().background(base).background(glow)) { content() }
}

@Composable
fun MediaSwipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
    ) { GradientBackground(content) }
}
