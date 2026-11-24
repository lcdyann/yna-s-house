package com.example.ynashouse.house

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.ynashouse.gacha.GachaScreen
import com.example.ynashouse.links.MyLinks
import com.example.ynashouse.state.Care
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.bubble.MeterBar
import com.example.ynashouse.ui.theme.BubbleBlue
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubbleLilac
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.HappyColor
import com.example.ynashouse.ui.theme.RestColor
import com.example.ynashouse.ui.theme.TummyColor
import com.example.ynashouse.watch.WatchListSheet
import com.example.ynashouse.watch.WatchListStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

/*
 * HOUSESCREEN.KT
 * Skrin utama: rumah nonagon.
 *
 *  ┌──────────────────────────┐
 *  │ [nama bilik]      [✎]    │  ← ATAS: nama bilik + butang set meter
 *  │ ❤️ ▓▓▓▓░  🍟 ▓▓▓░  😴 ▓▓  │  ← 3 meter
 *  │                          │
 *  │      (gambar bilik)      │  ← swipe kiri/kanan = bilik sebelah (pusing satu round)
 *  │         Yna 🐻           │
 *  │                          │
 *  │ [🤗][👂][🍟][💤]          │  ← BAWAH: butang ikut bilik
 *  │        (peta ⬡)          │  ← peta kecil nonagon
 *  └──────────────────────────┘
 */
@Composable
fun HouseScreen(state: YnaHouseState) {
    val rooms = HouseRoom.entries
    val count = rooms.size
    val ynaRoom = HouseRoom.forMood(state.mood)

    // pager "tak berpenghujung": mula di tengah supaya boleh swipe ke kiri & kanan selamanya
    val pager = rememberPagerState(
        initialPage = count * 500 + ynaRoom.ordinal,
        pageCount = { count * 1000 }
    )
    val scope = rememberCoroutineScope()
    var showMeters by remember { mutableStateOf(false) }
    var showWatchList by remember { mutableStateOf(false) }
    var showGacha by remember { mutableStateOf(false) }

    // senarai tontonan (Theater) & link Spotify (Music room)
    val context = LocalContext.current
    val watchList = remember {
        WatchListStore(context.getSharedPreferences("yna_house", Context.MODE_PRIVATE)).apply { load() }
    }
    val links = LocalUriHandler.current

    // jam animasi: berubah setiap 100ms (untuk frame Yna)
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(100)
            value = System.currentTimeMillis()
        }
    }
    // setiap 30 saat: meter turun + mungkin need baru
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            state.tick()
        }
    }

    // malam: 7pm - 7am (bilik luar & balkoni guna gambar malam)
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isNight = hour >= 19 || hour < 7

    val viewing = rooms[pager.currentPage % count]

    /* pusing ke bilik tertentu ikut jalan paling dekat */
    fun goTo(room: HouseRoom) {
        scope.launch {
            val current = pager.currentPage
            var steps = (room.ordinal - current % count + count) % count
            if (steps > count / 2) steps -= count
            pager.animateScrollToPage(current + steps)
        }
    }

    Box(Modifier.fillMaxSize()) {

        // ---------- BILIK-BILIK ----------
        HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
            val room = rooms[page % count]
            RoomPage(
                room = room,
                isNight = isNight,
                showYna = room == ynaRoom,
                state = state,
                now = now
            )
        }

        // ---------- ATAS: nama bilik + meter ----------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleCard(modifier = Modifier.weight(1f)) {
                    Text(viewing.label, style = MaterialTheme.typography.titleMedium)
                }
                BubbleButton(
                    text = "✎",
                    onClick = { showMeters = true },
                    color = BubblePink,
                    modifier = Modifier.width(58.dp)
                )
            }
            BubbleCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MeterBar("❤️", state.happiness, HappyColor, Modifier.fillMaxWidth())
                    MeterBar("🍟", state.tummy, TummyColor, Modifier.fillMaxWidth())
                    MeterBar("😴", state.rest, RestColor, Modifier.fillMaxWidth())
                }
            }

            // butang khas bilik (Theater: watch list, Music room: Spotify)
            when (viewing) {
                HouseRoom.THEATER -> {
                    val watching = watchList.nowWatching()
                    BubbleButton(
                        text = if (watching != null) "📺 Now watching: ${watching.title}" else "📺 My watch list",
                        onClick = { showWatchList = true },
                        color = BubbleLilac,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HouseRoom.GACHA -> {
                    BubbleButton(
                        text = if (state.foodMood.isNotEmpty()) "🎰 Spin the gacha · last: ${state.foodMood}" else "🎰 Spin the food gacha",
                        onClick = { showGacha = true },
                        color = BubbleYellow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                HouseRoom.RADIO -> {
                    BubbleButton(
                        text = "🎵 Open my Spotify playlist",
                        onClick = { links.openUri(MyLinks.SPOTIFY_PLAYLIST) },
                        color = BubbleGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                else -> Unit
            }
        }

        // ---------- BAWAH: butang ikut bilik + peta ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val roomMood = viewing.mood
            when {
                // bilik Yna: butang jaga Yna
                viewing == ynaRoom -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(Care.PAT, Care.LISTEN, Care.SNACK, Care.NAP).forEach { care ->
                            // butang yang selesaikan need sekarang jadi pink
                            val wanted = state.need?.fixedBy == care
                            BubbleButton(
                                text = "${care.emoji}\n${care.label}",
                                onClick = { state.doCare(care) },
                                color = if (wanted) BubblePink else BubbleYellow,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                // bilik mood lain: pindah ke sini
                roomMood != null -> {
                    BubbleButton(
                        text = "${roomMood.emoji} ${roomMood.nowText}",
                        onClick = { state.changeMood(roomMood) },
                        color = BubbleGreen,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // bilik biasa: tunjuk jalan ke Yna
                else -> {
                    BubbleButton(
                        text = "Yna is in the ${ynaRoom.label} →",
                        onClick = { goTo(ynaRoom) },
                        color = BubbleBlue,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            MiniMap(viewing = viewing, ynaRoom = ynaRoom)
        }

        // ---------- kotak set meter ----------
        if (showMeters) {
            MeterSheet(state = state, onClose = { showMeters = false })
        }
        // ---------- gacha makanan ----------
        if (showGacha) {
            GachaScreen(state = state, onClose = { showGacha = false })
        }
        // ---------- senarai tontonan ----------
        if (showWatchList) {
            WatchListSheet(store = watchList, canEdit = true, onClose = { showWatchList = false })
        }
    }
}
