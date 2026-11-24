package com.example.ynashouse.house

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubblePinkDeep
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.ui.layout.layout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import com.example.ynashouse.music.AlbumArt
import com.example.ynashouse.music.MusicBars
import com.example.ynashouse.music.Song
import com.example.ynashouse.posters.PosterImage
import com.example.ynashouse.ui.bubble.YnaIcon
import androidx.compose.ui.unit.Constraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.example.ynashouse.watch.Show
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.SpeechBubble
import com.example.ynashouse.yna.YnaView

/*
 * ROOMPAGE.KT
 * Satu bilik dalam pusingan rumah.
 * - Gambar bilik di-zoom supaya penuh ruang antara kad atas & butang bawah (padTop / padBottom).
 *   Siling kosong di atas gambar dipotong (ART_TOP_CUT), tepi kiri/kanan boleh terpotong sikit
 *   (sekurang-kurangnya MIN_VISIBLE lebar gambar mesti nampak). room.focusX = bahagian yang jadi tengah.
 * - Atas & bawah diwarnakan ikut dinding / lantai bilik.
 * - Kalau Yna tinggal di bilik ni (ikut mood), Yna dilukis di "spot" bilik.
 * - Theater: poster + tajuk cerita yang sedang ditonton keluar atas skrin projektor.
 * - Music room: lagu yang Diyana sedang dengar keluar atas TV.
 * - Benda boleh tekan (RoomHotspot.kt): ada label kecil melantun di atasnya.
 */

/* kedudukan skrin projektor dalam gambar Theater (0.0 - 1.0) */
private const val SCREEN_LEFT = 0.375f
private const val SCREEN_RIGHT = 0.745f
private const val SCREEN_TOP = 0.345f
private const val SCREEN_BOTTOM = 0.515f

/* kedudukan skrin TV dalam gambar Music room (0.0 - 1.0) */
private const val TV_LEFT = 0.378f
private const val TV_RIGHT = 0.622f
private const val TV_TOP = 0.405f
private const val TV_BOTTOM = 0.52f

/* saiz asal gambar bilik (928 x 1152) */
private const val ART_RATIO = 1152f / 928f

/* bahagian atas gambar (siling kosong) yang boleh disorok: 0.2 = 20% atas */
private const val ART_TOP_CUT = 0.2f

/* paling kurang 75% lebar gambar mesti nampak (jangan zoom terlalu banyak) */
private const val MIN_VISIBLE = 0.75f

@Composable
fun RoomPage(
    room: HouseRoom,
    isNight: Boolean,
    showYna: Boolean,
    state: YnaHouseState,
    now: Long,
    watching: Show? = null,
    song: Song? = null,
    rainy: Boolean = false,   // hujan sebenar → Outside & Balcony guna gambar hujan
    padTop: Dp = 0.dp,        // tinggi kad atas (nama bilik + meter + butang)
    padBottom: Dp = 0.dp,     // tinggi butang bawah + peta
    onAction: (RoomAction) -> Unit = {}
) {
    // label melantun naik turun
    val bounce = rememberInfiniteTransition(label = "bounce")
    val lift by bounce.animateFloat(
        initialValue = 0f,
        targetValue = -10f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "lift"
    )

    val wall = if (isNight) room.nightWall else room.wallColor
    val floor = if (isNight) room.nightFloor else room.floorColor

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // latar: separuh atas warna dinding, separuh bawah warna lantai
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().weight(1f).background(wall))
            Box(Modifier.fillMaxWidth().weight(1f).background(floor))
        }

        // ---- kira saiz & kedudukan gambar bilik ----
        val freeH = (maxHeight - padTop - padBottom).coerceAtLeast(100.dp)   // ruang kosong di tengah
        // lebar gambar: cukup besar supaya bahagian penting (bawah siling) penuh ruang kosong,
        // tapi tak kurang dari lebar skrin & tak zoom lebih dari MIN_VISIBLE
        val needW = freeH / ((1f - ART_TOP_CUT) * ART_RATIO)
        val artW = needW.coerceIn(maxWidth, maxWidth / MIN_VISIBLE)
        val artH = artW * ART_RATIO
        val bandH = artH * (1f - ART_TOP_CUT)                                 // bahagian penting gambar
        val artTop = padTop + (freeH - bandH) / 2 - artH * ART_TOP_CUT      // bahagian penting di tengah ruang kosong
        // kiri: room.focusX jadi tengah skrin, tapi jangan tunjuk ruang kosong di tepi
        val artLeft = (maxWidth / 2 - artW * room.focusX).coerceIn(maxWidth - artW, 0.dp)

        Image(
            painter = painterResource(
                when {
                    rainy && room.rainArt != null -> room.rainArt
                    isNight -> room.nightArt
                    else -> room.dayArt
                }
            ),
            contentDescription = room.label,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .offset(x = artLeft, y = artTop)
                .layout { measurable, constraints ->
                    // lukis gambar saiz penuh (boleh lebih besar dari skrin) tanpa dipusatkan semula,
                    // supaya kedudukan objek boleh tekan & Yna tepat dengan gambar
                    val w = artW.roundToPx()
                    val h = artH.roundToPx()
                    val pic = measurable.measure(Constraints.fixed(w, h))
                    layout(constraints.maxWidth, constraints.maxHeight) { pic.place(0, 0) }
                }
        )

        // Theater: poster + tajuk cerita yang sedang ditonton, atas skrin projektor
        if (room == HouseRoom.THEATER) {
            ScreenBox(
                x = artLeft + artW * SCREEN_LEFT,
                y = artTop + artH * SCREEN_TOP,
                w = artW * (SCREEN_RIGHT - SCREEN_LEFT),
                h = artH * (SCREEN_BOTTOM - SCREEN_TOP)
            ) {
                if (watching != null) {
                    Row(
                        Modifier.fillMaxSize().padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (watching.poster != null) {
                            PosterImage(
                                watching.poster,
                                Modifier
                                    .fillMaxHeight()
                                    .aspectRatio(0.68f)
                                    .clip(RoundedCornerShape(6.dp))
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("NOW SHOWING", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC2DE))
                            Text(
                                watching.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(watching.reaction.emoji, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                } else {
                    Text(
                        "Nothing on yet…\ntap to add a show",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // Music room: lagu yang Diyana sedang dengar, atas TV
        if (room == HouseRoom.RADIO) {
            ScreenBox(
                x = artLeft + artW * TV_LEFT,
                y = artTop + artH * TV_TOP,
                w = artW * (TV_RIGHT - TV_LEFT),
                h = artH * (TV_BOTTOM - TV_TOP)
            ) {
                if (song != null) {
                    Row(
                        Modifier.fillMaxSize().padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AlbumArt(song, 44.dp)
                        Column(Modifier.weight(1f)) {
                            Text(song.title, style = MaterialTheme.typography.labelMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(song.artist, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.75f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            MusicBars(playing = song.playing, color = Color(0xFF7FE0A8), height = 12.dp)
                        }
                    }
                } else {
                    Text(
                        "♪ tap to see\nYna's music",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }

        // benda boleh tekan + label melantun
        hotspotsFor(room).forEach { spot ->
            val spotX = artLeft + artW * spot.left
            val spotY = artTop + artH * spot.top
            Box(
                modifier = Modifier
                    .offset(x = spotX, y = spotY)
                    .size(width = artW * (spot.right - spot.left), height = artH * (spot.bottom - spot.top))
                    .clickable { onAction(spot.action) }
            )
            // label di tengah, betul-betul atas objek
            Box(
                modifier = Modifier
                    .offset(x = spotX, y = spotY - 32.dp)
                    .width(artW * (spot.right - spot.left)),
                contentAlignment = Alignment.TopCenter
            ) {
                // label satu baris je (ikon + tulisan), boleh lebih lebar dari objek
                Row(
                    modifier = Modifier
                        .wrapContentWidth(unbounded = true)
                        .graphicsLayer { translationY = lift }
                        .shadow(6.dp, CircleShape, ambientColor = BubblePinkDeep, spotColor = BubblePinkDeep)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color.White, BubblePink)))
                        .border(2.dp, Color.White, CircleShape)
                        .clickable { onAction(spot.action) }
                        .padding(start = 6.dp, end = 12.dp, top = 3.dp, bottom = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    YnaIcon(spot.icon, size = 22.dp)
                    Text(spot.label, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }
        }

        // Yna di spot bilik
        if (showYna) {
            val ynaW = artW * room.ynaSize
            val ynaX = artLeft + artW * room.spotX - ynaW / 2
            val ynaY = artTop + artH * room.spotY - ynaW      // spotY = kaki Yna
            YnaView(
                state = state,
                now = now,
                showReaction = if (room == HouseRoom.THEATER) watching?.reaction else null,
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
                        .offset(x = ynaX + ynaW / 2 - maxWidth / 2, y = ynaY - 70.dp)   // atas kepala Yna
                )
            }
        }
    }
}

/* skrin gelap (projektor / TV) di atas gambar bilik, kandungan di dalam */
@Composable
private fun ScreenBox(x: Dp, y: Dp, w: Dp, h: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(width = w, height = h)
            .clip(RoundedCornerShape(4.dp))
            .background(Brush.verticalGradient(listOf(Color(0xEE2B2140), Color(0xEE3E2F5C)))),
        content = content
    )
}
