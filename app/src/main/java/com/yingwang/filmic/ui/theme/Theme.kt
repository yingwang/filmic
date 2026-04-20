package com.yingwang.filmic.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Graphite,
    onPrimary = Paper,
    secondary = Accent,
    onSecondary = Paper,
    background = Ivory,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Hairline,
    onSurfaceVariant = Ash,
    outline = Hairline,
)

private val DarkColors = darkColorScheme(
    primary = Paper,
    onPrimary = InkDark,
    secondary = Accent,
    onSecondary = Paper,
    background = InkDark,
    onBackground = InkDarkOn,
    surface = InkDarkSurface,
    onSurface = InkDarkOn,
    surfaceVariant = Ink,
    onSurfaceVariant = Ash,
    outline = Ink,
)

@Composable
fun FilmicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FilmicTypography,
        content = content,
    )
}
