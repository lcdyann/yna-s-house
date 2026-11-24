package com.example.ynashouse.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * COLOR.KT
 * Warna Yna's House ("cozy game HUD"). Tukar warna di sini, semua skrin ikut.
 * Setiap warna butang ada versi "Deep" (lebih gelap) untuk gradient & bayang.
 */
val Ink = Color(0xFF3B2B3F)          // tulisan utama (coklat-ungu gelap)
val InkSoft = Color(0xFF7A6380)      // tulisan kecil
val Cream = Color(0xFFFFF7FD)        // latar terang
val Pink = Color(0xFFE0569F)         // tulisan penting / warna utama
val Glass = Color(0xEEFFFFFF)        // kotak HUD putih separa lut sinar

/* warna butang (terang → deep) */
val BubblePink = Color(0xFFFFC2DE)
val BubblePinkDeep = Color(0xFFF27BB4)
val BubbleYellow = Color(0xFFFFE597)
val BubbleYellowDeep = Color(0xFFFFBE3D)
val BubbleBlue = Color(0xFFB4E6FA)
val BubbleBlueDeep = Color(0xFF6DBDEB)
val BubbleGreen = Color(0xFFC2F2CF)
val BubbleGreenDeep = Color(0xFF6CD39A)
val BubbleLilac = Color(0xFFDCCBFF)
val BubbleLilacDeep = Color(0xFFA98BF2)
val BubblePeach = Color(0xFFFFD0AE)
val BubblePeachDeep = Color(0xFFFF9A62)

/* warna meter */
val HappyColor = Color(0xFFFF7FB0)
val TummyColor = Color(0xFFFFB547)
val RestColor = Color(0xFF8EA9FF)

/* cari versi deep untuk warna butang (warna lain: gelapkan sikit) */
fun deepOf(c: Color): Color = when (c) {
    BubblePink -> BubblePinkDeep
    BubbleYellow -> BubbleYellowDeep
    BubbleBlue -> BubbleBlueDeep
    BubbleGreen -> BubbleGreenDeep
    BubbleLilac -> BubbleLilacDeep
    BubblePeach -> BubblePeachDeep
    else -> Color(
        red = c.red * 0.85f,
        green = c.green * 0.85f,
        blue = c.blue * 0.85f,
        alpha = c.alpha
    )
}
