package com.koreageo.quiz.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Seed = Color(0xFF2E6E63)

private val LightColors = lightColorScheme(
    primary = Seed,
    secondary = Color(0xFF4C6B5E),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD6C6),
    secondary = Color(0xFFB2CCBF),
)

@Composable
fun KoreaGeoQuizTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, content = content)
}
