package com.calorietracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Rose500,
    onPrimary = Neutral900,
    primaryContainer = Rose500.copy(alpha = 0.2f),
    onPrimaryContainer = Rose300,
    secondary = Orange500,
    onSecondary = Neutral900,
    background = Neutral900,
    onBackground = androidx.compose.ui.graphics.Color.White,
    surface = Neutral800,
    onSurface = androidx.compose.ui.graphics.Color.White,
    surfaceVariant = Neutral700,
    onSurfaceVariant = Neutral400,
    outline = Neutral700,
    error = Red500,
    onError = Neutral900
)

@Composable
fun CalorieTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
