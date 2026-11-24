package com.example.ynashouse.house

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ynashouse.gacha.GachaScreen
import com.example.ynashouse.games.GamesScreen
import com.example.ynashouse.state.Care
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.HudPill
import com.example.ynashouse.ui.bubble.RoundButton
import com.example.ynashouse.ui.theme.BubbleBlue
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubbleLilac
import com.example.ynashouse.ui.theme.BubblePeach
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.HappyColor
import com.example.ynashouse.ui.theme.Pink
import com.example.ynashouse.ui.theme.RestColor
import com.example.ynashouse.ui.theme.TummyColor
import com.example.ynashouse.watch.WatchListSheet
import com.example.ynashouse.watch.WatchListStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableIntStateOf
import androidx.core.content.ContextCompat
import com.example.ynashouse.sync.HouseSync
import com.example.ynashouse.weather.Weather
import com.example.ynashouse.weather.WeatherNow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubblePinkDeep
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.example.ynashouse.state.LocalVisitorMode
import com.example.ynashouse.ui.bubble.YnaIcon
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import com.example.ynashouse.R
import com.example.ynashouse.music.MusicSheet
import com.example.ynashouse.music.NowPlaying
import com.example.ynashouse.music.Song
import com.example.ynashouse.ui.bubble.Ikon
import com.example.ynashouse.ui.bubble.LiquidMeter
import com.example.ynashouse.ui.bubble.RoundButtonBase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * HOUSESCREEN.KT
 * Skrin utama: rumah nonagon, gaya "cozy game HUD".
 *
 *  ┌────────────────────────────────┐
 *  │ (🕹️ Playroom)  [❤️][🍟][🌙](My)│  ← HUD atas: nama bilik + meter cecair + butang My (Diyana sahaja)
 *  │ (🐻 Yna is in the Theater →)   │  ← chip info (ikut bilik)
 *  │                                │
 *  │        (gambar bilik)          │  ← swipe kiri/kanan = bilik sebelah (pusing satu round)
 *  │            Yna 🐻              │
 *  │                                │
 *  │  ╭ (🐾) (💬) (🍟) (🌙) (🎮) ╮   │  ← dok bawah: butang bulat ikut bilik
 *  │            (⬡)                 │  ← peta nonagon: tekan = Hallway (9 gambar bilik)
 *  └────────────────────────────────┘
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
    var showWatchList by remember { mutableStateOf(false) }
    var showGacha by remember { mutableStateOf(false) }
    var showGames by remember { mutableStateOf(false) }
    var showMusic by remember { mutableStateOf(false) }
    var showHallway by remember { mutableStateOf(false) }
    var showMy by remember { mutableStateOf(false) }               // butang "My" (Diyana sahaja)
    val visitor = LocalVisitorMode.current                           // app pelawat nanti: tiada butang My

    // tinggi HUD atas & dok bawah (diukur bila dilukis) — bilik di-zoom muat di tengah-tengah.
    // Simpan nilai paling tinggi supaya gambar tak melompat bila swipe ke bilik lain.
    val density = LocalDensity.current
    var topPad by remember { mutableStateOf(0.dp) }
    var bottomPad by remember { mutableStateOf(0.dp) }

    // senarai tontonan (Theater) & link Spotify (Music room)
    val context = LocalContext.current
    val watchList = remember {
        WatchListStore(context.getSharedPreferences("yna_house", Context.MODE_PRIVATE)).apply { load() }
    }
    val links = LocalUriHandler.current

    /* buka benda dalam bilik (dari objek dalam gambar atau butang dok) */
    fun doAction(action: RoomAction) {
        when (action) {
            RoomAction.GAMES -> showGames = true
            RoomAction.GACHA -> showGacha = true
            RoomAction.WATCH_LIST -> showWatchList = true
            RoomAction.SPOTIFY -> showMusic = true
        }
    }

    // jam animasi: berubah setiap 100ms (untuk frame Yna)
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(100)
            value = System.currentTimeMillis()
        }
    }
    // lagu yang Diyana sedang dengar (semak setiap 4 saat, perlukan "notification access")
    val song by produceState<Song?>(initialValue = null) {
        while (true) {
            value = withContext(Dispatchers.IO) { NowPlaying.read(context) }
            delay(4_000)
        }
    }

    // ---------- cuaca sebenar (setiap 30 minit) ----------
    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    var weatherTry by remember { mutableIntStateOf(0) }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { weatherTry++ }
    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) askLocation.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }
    LaunchedEffect(weatherTry) {
        while (true) {
            Weather.fetch(context)?.let { weather = it }
            delay(30 * 60_000L)
        }
    }

    // ---------- sync ke website (rumah live untuk pelawat) ----------
    suspend fun syncNow() {
        val boost = HouseSync.sync(context, state, watchList.shows.toList(), song, weather) ?: return
        val h = boost["happiness"] ?: 0
        val t = boost["tummy"] ?: 0
        val r = boost["rest"] ?: 0
        if (h + t + r > 0) {
            state.addVisitorCare(h, t, r)
            Toast.makeText(context, "💕 Visitors cared for Yna!", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            syncNow()
            delay(60_000L)
        }
    }
    // perubahan penting dihantar cepat (2 saat lepas berubah)
    LaunchedEffect(state.mood, state.foodMood, song?.title, song?.playing, weather) {
        delay(2_000L)
        syncNow()
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
                now = now,
                watching = watchList.nowWatching(),
                song = song,
                rainy = weather?.isRainy == true,
                padTop = topPad,
                padBottom = bottomPad,
                onAction = { doAction(it) }
            )
        }

        // ---------- HUD ATAS ----------
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onSizeChanged { val h = with(density) { it.height.toDp() }; if (h > topPad) topPad = h }
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // nama bilik (boleh mengecil kalau skrin sempit, meter tetap penuh)
                HudPill(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        "${viewing.emoji} ${viewing.label}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // 3 meter (tekan = set meter ikut mood sebenar)
                // 3 meter cecair macam Pou (tekan = set meter ikut mood sebenar)
                Row(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!visitor) showMy = true
                        },
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidMeter(Ikon.HEART, state.happiness, HappyColor, boxSize = 40.dp)
                    LiquidMeter(Ikon.FRIES, state.tummy, TummyColor, boxSize = 40.dp)
                    LiquidMeter(Ikon.MOON, state.rest, RestColor, boxSize = 40.dp)
                    // butang "My": mood, level, muzik, games (Diyana sahaja)
                    if (!visitor) MyButton { showMy = true }
                }
            }

            // chip info (satu baris, boleh scroll ke tepi)
            val showCraving = (viewing == HouseRoom.GACHA || viewing == HouseRoom.KITCHEN) && state.foodMood.isNotEmpty()
            val w = weather
            if (w != null || showCraving || viewing != ynaRoom) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // cuaca sebenar
                    if (w != null) {
                        HudPill {
                            Text("${w.emoji} ${w.temp}°", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    // food mood Diyana (pelawat nampak di Kitchen & Gacha room)
                    if (showCraving) {
                        HudPill(onClick = { showGacha = true }) {
                            YnaIcon(Ikon.FRIES, size = 20.dp)
                            Text(
                                "Yna's craving: ${state.foodMood}" +
                                    if (state.foodMoodBefore.isNotEmpty()) "  ·  before: ${state.foodMoodBefore}" else "",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                    if (viewing != ynaRoom) {
                        HudPill(onClick = { goTo(ynaRoom) }) {
                            Text("🐻 Yna is in the ${ynaRoom.label} →", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        // ---------- DOK BAWAH ----------
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { val h = with(density) { it.height.toDp() }; if (h > bottomPad) bottomPad = h }
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(32.dp))
                    .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(32.dp))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Top
            ) {
                // butang benda dalam bilik (main game / gacha / senarai tontonan / Spotify)
                roomActionButton(viewing)?.let { (icon, label, action) ->
                    RoundButton(icon, label, onClick = { doAction(action) }, color = BubbleLilac)
                }

                when {
                    // bilik Yna: jaga Yna (butang yang Yna perlukan goyang & jadi pink)
                    viewing == ynaRoom -> {
                        listOf(Care.PAT, Care.LISTEN, Care.SNACK, Care.NAP).forEach { care ->
                            RoundButton(
                                icon = careIcon(care),
                                label = care.label,
                                onClick = { state.doCare(care) },
                                color = BubbleYellow,
                                highlight = state.need?.fixedBy == care
                            )
                        }
                    }
                    // bilik biasa: cari Yna
                    else -> {
                        FindYnaButton { goTo(ynaRoom) }
                        if (viewing == HouseRoom.OUTSIDE) {
                            RoundButton(Ikon.ENVELOPE, "Mail", onClick = {
                                Toast.makeText(context, "Mailbox opens when visitors arrive 💌", Toast.LENGTH_SHORT).show()
                            }, color = BubblePeach)
                        }
                    }
                }
            }

            // peta nonagon: tekan = buka Hallway (9 gambar bilik)
            MiniMap(viewing = viewing, ynaRoom = ynaRoom, onClick = { showHallway = true })
        }

        // ---------- butang My (Diyana sahaja) ----------
        if (showMy) {
            MySheet(
                state = state,
                song = song,
                onMoodPicked = { goTo(it) },
                onPlayGames = { showMy = false; showGames = true },
                onClose = { showMy = false }
            )
        }
        // ---------- permainan (Playroom) ----------
        if (showGames) {
            GamesScreen(onClose = { showGames = false })
        }
        // ---------- gacha makanan ----------
        if (showGacha) {
            GachaScreen(state = state, onClose = { showGacha = false })
        }
        // ---------- muzik (TV Music room) ----------
        if (showMusic) {
            MusicSheet(song = song, onClose = { showMusic = false })
        }
        // ---------- lorong 9 bilik ----------
        if (showHallway) {
            HallwaySheet(
                viewing = viewing,
                ynaRoom = ynaRoom,
                isNight = isNight,
                onPick = { goTo(it); showHallway = false },
                onClose = { showHallway = false }
            )
        }
        // ---------- senarai tontonan ----------
        if (showWatchList) {
            WatchListSheet(store = watchList, canEdit = !visitor, onClose = { showWatchList = false })
        }
    }
}

/* butang dok untuk benda dalam bilik: (ikon, label, action) atau null */
private fun roomActionButton(room: HouseRoom): Triple<Ikon, String, RoomAction>? = when (room) {
    HouseRoom.PLAYROOM -> Triple(Ikon.GAMEPAD, "Play", RoomAction.GAMES)
    HouseRoom.GACHA -> Triple(Ikon.GACHA, "Gacha", RoomAction.GACHA)
    HouseRoom.THEATER -> Triple(Ikon.TV, "Shows", RoomAction.WATCH_LIST)
    HouseRoom.RADIO -> Triple(Ikon.MUSIC, "Music", RoomAction.SPOTIFY)
    else -> null
}

/* ikon untuk setiap cara jaga Yna */
private fun careIcon(care: Care): Ikon = when (care) {
    Care.PAT -> Ikon.PAW
    Care.LISTEN -> Ikon.CHAT
    Care.SNACK -> Ikon.FRIES
    Care.NAP -> Ikon.MOON
    Care.WELCOME -> Ikon.HEART
}

/* butang "Find Yna": muka Yna dalam bulatan */
@Composable
private fun FindYnaButton(onClick: () -> Unit) {
    RoundButtonBase(label = "Find Yna", onClick = onClick, color = BubbleBlue) {
        Image(
            painter = painterResource(R.drawable.yna_happy),
            contentDescription = "Yna",
            modifier = Modifier.size(44.dp)
        )
    }
}

/* butang bulat "My" (pink, mahkota kecil) */
@Composable
private fun MyButton(onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .shadow(5.dp, CircleShape, ambientColor = Pink, spotColor = Pink)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(BubblePink, BubblePinkDeep)))
            .border(2.5.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        YnaIcon(Ikon.CROWN, size = 18.dp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 1.dp))
        Text("My", style = MaterialTheme.typography.labelLarge, color = Color.White, modifier = Modifier.padding(top = 12.dp))
    }
}
