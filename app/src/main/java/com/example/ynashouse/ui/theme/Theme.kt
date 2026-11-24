package com.example.ynashouse.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/*
 * THEME.KT
 * Tema app: sentiasa terang & pastel (Yna's House tiada dark mode).
 * Kalau ada res/font/fredoka.ttf, semua tulisan guna font tu.
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

@SuppressLint("DiscouragedApi")
@Composable
fun YnasHouseTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    // cari font "fredoka" dalam res/font (0 = tiada)
    val typography = remember {
        val id = context.resources.getIdentifier("fredoka", "font", context.packageName)
        if (id != 0) ynaTypography(FontFamily(Font(id))) else YnaTypography
    }
    MaterialTheme(
        colorScheme = YnaColors,
        typography = typography,
        content = content
    )
}
