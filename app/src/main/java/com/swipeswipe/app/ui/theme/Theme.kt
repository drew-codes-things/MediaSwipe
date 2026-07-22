package com.swipeswipe.app.ui.theme

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

/**
 * drew-gnr.xyz has no light-mode variant, so this app doesn't offer one
 * either - always the dark red-on-black palette, regardless of system theme.
 */
@Composable
fun SwipeSwipeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content,
    )
}
