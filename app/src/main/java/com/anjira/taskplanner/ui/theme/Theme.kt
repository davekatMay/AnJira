package com.anjira.taskplanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF4766F2),
    secondary = Color(0xFF06D6A0),
    tertiary = Color(0xFFAF40FF),
    error = Color(0xFFB00020),
    background = Color(0xFFFFFBFE),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFFFFFFFF),
    onError = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF000000)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8CB5FF),
    secondary = Color(0xFF70F2B9),
    tertiary = Color(0xFFD0BCFF),
    error = Color(0xFFCF6679),
    background = Color(0xFF121212),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),
    onError = Color(0xFFFFFFFF),
    onBackground = Color(0xFFE1E1E1),
    onSurface = Color(0xFFE1E1E1),
    surface = Color(0xFF121212)
)

@Composable
fun TaskPlannerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        DarkColors
    } else {
        LightColors
    }

    androidx.compose.material3.MaterialTheme(
        colorScheme = colors,
        content = content
    )
}