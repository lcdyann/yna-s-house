package com.example.ynashouse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/*
 * TYPE.KT
 * Saiz & tebal tulisan.
 * Nak guna font comel (contoh Fredoka): letak fail .ttf dalam res/font,
 * kemudian tukar FontFamily.Default kepada FontFamily(Font(R.font.fredoka)).
 */
val YnaFont = FontFamily.Default

val YnaTypography = Typography(
    titleLarge = TextStyle(fontFamily = YnaFont, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Ink),
    titleMedium = TextStyle(fontFamily = YnaFont, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink),
    bodyMedium = TextStyle(fontFamily = YnaFont, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Ink),
    labelLarge = TextStyle(fontFamily = YnaFont, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Ink),
    labelSmall = TextStyle(fontFamily = YnaFont, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = InkSoft)
)
