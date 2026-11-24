package com.example.ynashouse.house

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.SpeechBubble
import com.example.ynashouse.yna.YnaView

/*
 * ROOMPAGE.KT
 * Satu bilik dalam pusingan rumah.
 * - Gambar bilik penuh lebar skrin, di tengah. Atas & bawah diwarnakan ikut dinding / lantai bilik.
 * - Kalau Yna tinggal di bilik ni (ikut mood), Yna dilukis di "spot" bilik.
 */

/* saiz asal gambar bilik (928 x 1152) */
private const val ART_RATIO = 1152f / 928f

@Composable
fun RoomPage(
    room: HouseRoom,
    isNight: Boolean,
    showYna: Boolean,
    state: YnaHouseState,
    now: Long
) {
    val wall = if (isNight) room.nightWall else room.wallColor
    val floor = if (isNight) room.nightFloor else room.floorColor

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // latar: separuh atas warna dinding, separuh bawah warna lantai
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).background(wall))
            Box(Modifier.fillMaxWidth().weight(1f).background(floor))
        }

        // gambar bilik (penuh lebar, di tengah)
        val artW = maxWidth
        val artH = maxWidth * ART_RATIO
        val artTop = (maxHeight - artH) / 2
        Image(
            painter = painterResource(if (isNight) room.nightArt else room.dayArt),
            contentDescription = room.label,
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .fillMaxWidth()
                .height(artH)
                .offset(y = artTop)
        )

        // Yna di spot bilik
        if (showYna) {
            val ynaW = artW * room.ynaSize
            val ynaX = artW * room.spotX - ynaW / 2
            val ynaY = artTop + artH * room.spotY - ynaW      // spotY = kaki Yna
            YnaView(
                state = state,
                now = now,
                modifier = Modifier
                    .offset(x = ynaX, y = ynaY)
                    .size(ynaW)
            )

            // bubble cakap / need di atas kepala Yna
            val caring = state.care != null && now < state.careUntil
            val bubbleText = if (caring) state.speech else state.need?.bubble
            if (bubbleText != null) {
                SpeechBubble(
                    text = bubbleText,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = ynaY - 70.dp)
                )
            }
        }
    }
}
