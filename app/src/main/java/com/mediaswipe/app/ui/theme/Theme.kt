package com.mediaswipe.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
fun MediaSwipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
