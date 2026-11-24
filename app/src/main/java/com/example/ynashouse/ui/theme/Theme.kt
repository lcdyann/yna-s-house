package com.example.ynashouse.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/*
 * THEME.KT
 * Tema app: sentiasa terang & pastel (Yna's House tiada dark mode).
 */
private val YnaColors = lightColorScheme(
    primary = Pink,
    onPrimary = Cream,
    secondary = BubbleBlue,
    background = Cream,
    surface = Cream,
    onBackground = Ink,
    onSurface = Ink
)

@Composable
fun YnasHouseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = YnaColors,
        typography = YnaTypography,
        content = content
    )
}
