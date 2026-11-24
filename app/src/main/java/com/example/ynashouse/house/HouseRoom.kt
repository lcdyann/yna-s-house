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
    val nightFloor: Color = floorColor
) {
    OUTSIDE(
        "Outside", R.drawable.house_outside, R.drawable.house_outside_night, null,
        0.5f, 0.92f, 0.38f, Color(0xFFC9E5F1), Color(0xFFCECBC0),
        Color(0xFF45547E), Color(0xFF6A7289)
    ),
    KITCHEN(
        "Kitchen", R.drawable.room_kitchen, R.drawable.room_kitchen, Mood.EATING,
        0.36f, 0.92f, 0.44f, Color(0xFFFBF4E6), Color(0xFFE2D1C0)
    ),
    GACHA(
        "Gacha room", R.drawable.room_gacha, R.drawable.room_gacha, Mood.STRESS_EATING,
        0.55f, 0.92f, 0.44f, Color(0xFFFBF4E7), Color(0xFFE1CFBF)
    ),
    PLAYROOM(
        "Playroom", R.drawable.room_playroom, R.drawable.room_playroom, Mood.GAMING,
        0.5f, 0.86f, 0.44f, Color(0xFFFBF4E7), Color(0xFFDFCDBE)
    ),
    THEATER(
        "Theater", R.drawable.room_theater, R.drawable.room_theater, Mood.WATCHING,
        0.5f, 0.97f, 0.42f, Color(0xFFF9F2E7), Color(0xFFDDCBBD)
    ),
    RADIO(
        "Music room", R.drawable.room_radio, R.drawable.room_radio, null,
        0.5f, 0.92f, 0.42f, Color(0xFFF8F1E6), Color(0xFFDBC9BA)
    ),
    DESK(
        "Computer desk", R.drawable.room_desk, R.drawable.room_desk, Mood.CODING,
        0.5f, 0.92f, 0.44f, Color(0xFFF9F1E5), Color(0xFFDECDBC)
    ),
    BEDROOM(
        "Bedroom", R.drawable.room_bedroom, R.drawable.room_bedroom, Mood.SLEEPING,
        0.47f, 0.80f, 0.50f, Color(0xFFF7F0E4), Color(0xFFDCCABA)
    ),
    BALCONY(
        "Balcony", R.drawable.room_balcony, R.drawable.room_balcony_night, null,
        0.5f, 0.92f, 0.42f, Color(0xFFF6EFE4), Color(0xFFF0ECE9),
        Color(0xFF6A6E7F), Color(0xFF606C86)
    );

    companion object {
        /* bilik untuk sesuatu mood */
        fun forMood(mood: Mood): HouseRoom = entries.first { it.mood == mood }
    }
}
