package com.example.ynashouse.house

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.ynashouse.R
import com.example.ynashouse.mood.Mood

/*
 * HOUSEROOM.KT
 * 9 bilik rumah nonagon, ikut susunan bulatan (swipe kiri/kanan pusing satu round).
 * Susunan: Outside → Kitchen → Gacha → Playroom → Theater → Music → Desk → Bedroom → Balcony
 *
 * Setiap bilik:
 * - dayArt / nightArt : gambar latar (nightArt sama dengan dayArt kalau tiada versi malam)
 * - mood              : mood yang tinggal di bilik ni (null = bilik biasa, Yna tak duduk di sini)
 * - spotX, spotY      : tempat kaki Yna dalam gambar (0.0 - 1.0, dari kiri / dari atas)
 * - ynaSize           : saiz Yna berbanding lebar gambar (0.4 = 40% lebar)
 * - wallColor         : warna tepi atas (sambung gambar dengan skrin)
 * - floorColor        : warna tepi bawah
 * - nightWall / nightFloor : warna tepi waktu malam (kalau ada gambar malam)
 * - focusX            : tengah bilik yang penting (0.5 = tengah gambar)
 */
enum class HouseRoom(
    val label: String,
    @DrawableRes val dayArt: Int,
    @DrawableRes val nightArt: Int,
    val mood: Mood?,
    val spotX: Float,
    val spotY: Float,
    val ynaSize: Float,
    val wallColor: Color,
    val floorColor: Color,
    val nightWall: Color = wallColor,
    val nightFloor: Color = floorColor,
    val focusX: Float = 0.5f,         // bahagian gambar yang jadi tengah skrin bila gambar di-zoom
    @DrawableRes val rainArt: Int? = null   // gambar masa hujan (Outside & Balcony sahaja)
) {
    OUTSIDE(
        "Outside", R.drawable.house_outside, R.drawable.house_outside_night, Mood.OUT,
        0.5f, 0.88f, 0.27f,                          // Yna atas laluan batu depan pintu
        Color(0xFFC9E5F1), Color(0xFFCECBC0),
        Color(0xFF45547E), Color(0xFF6A7289),
        rainArt = R.drawable.house_outside_rain
    ),
    KITCHEN(
        "Kitchen", R.drawable.room_kitchen, R.drawable.room_kitchen, Mood.EATING,
        0.36f, 0.9f, 0.32f, Color(0xFFFBF4E6), Color(0xFFE2D1C0), focusX = 0.55f
    ),
    GACHA(
        "Gacha room", R.drawable.room_gacha, R.drawable.room_gacha, Mood.STRESS_EATING,
        0.6f, 0.88f, 0.31f, Color(0xFFFBF4E7), Color(0xFFE1CFBF), focusX = 0.46f
    ),
    PLAYROOM(
        "Playroom", R.drawable.room_playroom, R.drawable.room_playroom, Mood.GAMING,
        0.58f, 0.88f, 0.34f, Color(0xFFFBF4E7), Color(0xFFDFCDBE), focusX = 0.45f
    ),
    THEATER(
        "Theater", R.drawable.room_theater, R.drawable.room_theater, Mood.WATCHING,
        0.5f, 0.79f, 0.28f, Color(0xFFF9F2E7), Color(0xFFDDCBBD)   // Yna duduk atas sofa
    ),
    RADIO(
        "Music room", R.drawable.room_radio, R.drawable.room_radio, Mood.MUSIC,
        0.78f, 0.9f, 0.34f, Color(0xFFF8F1E6), Color(0xFFDBC9BA), focusX = 0.55f   // Yna menari tepi kerusi
    ),
    DESK(
        "Computer desk", R.drawable.room_desk, R.drawable.room_desk, Mood.CODING,
        0.75f, 0.755f, 0.3f, Color(0xFFF9F1E5), Color(0xFFDECDBC), focusX = 0.57f   // Yna dalam beanbag
    ),
    BEDROOM(
        "Bedroom", R.drawable.room_bedroom, R.drawable.room_bedroom, Mood.SLEEPING,
        0.49f, 0.745f, 0.34f, Color(0xFFF7F0E4), Color(0xFFDCCABA)   // Yna baring atas katil
    ),
    BALCONY(
        "Balcony", R.drawable.room_balcony, R.drawable.room_balcony_night, Mood.CHILLING,
        0.49f, 0.69f, 0.22f,                         // Yna duduk atas bangku
        Color(0xFFF6EFE4), Color(0xFFF0ECE9),
        Color(0xFF6A6E7F), Color(0xFF606C86),
        rainArt = R.drawable.room_balcony_rain
    );

    /* emoji bilik (untuk HUD nama bilik) */
    val emoji: String
        get() = when (this) {
            OUTSIDE -> "🏡"
            KITCHEN -> "🍳"
            GACHA -> "🎰"
            PLAYROOM -> "🕹️"
            THEATER -> "🎬"
            RADIO -> "🎵"
            DESK -> "💻"
            BEDROOM -> "🌙"
            BALCONY -> "🌿"
        }

    companion object {
        /* bilik untuk sesuatu mood */
        fun forMood(mood: Mood): HouseRoom = entries.first { it.mood == mood }
    }
}
