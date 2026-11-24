package com.example.ynashouse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * TYPE.KT
 * Saiz & tebal tulisan.
 * Font comel: letak fail bernama "fredoka.ttf" dalam app/src/main/res/font/
 * (buat folder font kalau tiada). Theme.kt akan jumpa sendiri — tak perlu ubah kod.
 * Muat turun Fredoka percuma: fonts.google.com/specimen/Fredoka
 */
fun ynaTypography(font: FontFamily) = Typography(
    headlineMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, color = Ink),
    titleLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink),
    titleMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink),
    bodyMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink),
    bodySmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = InkSoft),
    labelLarge = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink),
    labelMedium = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Ink),
    labelSmall = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = InkSoft)
)

/* font biasa (sebelum fredoka.ttf dimasukkan) */
val YnaTypography = ynaTypography(FontFamily.Default)
