package com.example.ynashouse.gacha

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ynashouse.R
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.theme.BubbleBlue
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.InkSoft
import com.example.ynashouse.ui.theme.Pink
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder

/*
 * GACHASCREEN.KT
 * Mesin gacha makanan (buka dari Gacha room).
 *
 * Langkah:
 *  PICK    → pilih 1-2 mood token + jarak, tekan PULL (tiada mood = Yna pilih random)
 *  SPIN    → kapsul bergoyang & jatuh sambil cari kedai
 *  RESULT  → kapsul terbuka, kad kedai: Nope (cari lain) / Let's eat
 *  GO      → direction Google Maps / Waze + TikTok
 *  PROBLEM → tiada lokasi / internet / kedai
 */
private enum class Step { PICK, SPIN, RESULT, GO, PROBLEM }

@Composable
fun GachaScreen(state: YnaHouseState, onClose: () -> Unit) {
    val context = LocalContext.current
    val links = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(Step.PICK) }
    val picked = remember { mutableStateListOf<FoodMood>() }
    var km by remember { mutableFloatStateOf(2f) }
    var token by remember { mutableStateOf<List<FoodMood>>(emptyList()) }

    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var usingBackup by remember { mutableStateOf(false) }
    val seen = remember { mutableStateListOf<String>() }
    var chosen by remember { mutableStateOf<Place?>(null) }
    var problem by remember { mutableStateOf("") }

    // animasi kapsul
    val drop = remember { Animatable(-260f) }
    val wobble = remember { Animatable(0f) }
    val open = remember { Animatable(0f) }

    /* pilih kedai random: yang belum keluar, yang confirm buka didahulukan */
    fun pickPlace(): Place {
        var fresh = places.filter { it.id !in seen }
        if (fresh.isEmpty()) {
            seen.clear()
            fresh = places
        }
        val openOnes = fresh.filter { it.openNow == true }
        val place = (openOnes.ifEmpty { fresh }).random()
        seen += place.id
        return place
    }

    /* animasi kapsul jatuh & terbuka, lepas tu tunjuk kad */
    suspend fun capsuleAnimation() {
        open.snapTo(0f)
        drop.snapTo(-260f)
        repeat(3) {
            wobble.animateTo(10f, tween(90))
            wobble.animateTo(-10f, tween(90))
        }
        wobble.animateTo(0f, tween(90))
        drop.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        delay(250)
        open.animateTo(1f, tween(450))
    }

    /* PULL: cari kedai sambil animasi */
    fun pull() {
        token = picked.toList().ifEmpty { listOf(FOOD_MOODS.random()) }   // tiada mood: Yna pilih
        state.saveFoodMood(token.joinToString(" + ") { it.phrase })
        step = Step.SPIN
        seen.clear()
        scope.launch {
            val search = async {
                runCatching { findPlaces(context, token, km) }
            }
            capsuleAnimation()
            val result = search.await()
            result.onSuccess { found ->
                usingBackup = found.list.isEmpty() && found.backup.isNotEmpty()
                places = if (found.list.isNotEmpty()) found.list else found.backup
                if (places.isEmpty()) {
                    problem = "No food places found within ${km.toInt()} km 😢"
                    step = Step.PROBLEM
                } else {
                    chosen = pickPlace()
                    step = Step.RESULT
                }
            }.onFailure { error ->
                problem = if (error is NoLocationException) {
                    "I need your location to find food near you 🥺 Please turn on location and try again."
                } else {
                    "Can't reach the food map right now. Check your internet and try again."
                }
                step = Step.PROBLEM
            }
        }
    }

    /* Nope: kapsul baru */
    fun nope() {
        step = Step.SPIN
        scope.launch {
            capsuleAnimation()
            chosen = pickPlace()
            step = Step.RESULT
        }
    }

    // minta kebenaran lokasi, kemudian PULL
    val askLocation = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) {
            pull()
        } else {
            problem = "I need your location to find food near you 🥺"
            step = Step.PROBLEM
        }
    }
    fun startPull() {
        askLocation.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFBE3F3))
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---------- atas: tajuk + tutup ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BubbleCard(modifier = Modifier.weight(1f)) {
                    Text("🎰 MAKAN♥PON", style = MaterialTheme.typography.titleMedium, color = Pink)
                }
                BubbleButton(text = "✕", onClick = onClose, color = BubblePink, modifier = Modifier.width(58.dp))
            }

            // ---------- Yna + bubble ----------
            val ynaPic = when (step) {
                Step.PICK -> R.drawable.yna_hmm
                Step.SPIN -> R.drawable.yna_excited
                Step.RESULT -> if (usingBackup) R.drawable.yna_hmm else R.drawable.yna_wow
                Step.GO -> R.drawable.yna_happy
                Step.PROBLEM -> R.drawable.yna_teary
            }
            val ynaLine = when (step) {
                Step.PICK -> "What are we craving? Pick 1 or 2 🥺"
                Step.SPIN -> "Ooh spinning… 🎰"
                Step.RESULT -> if (usingBackup) "Couldn't find exactly that… but how about this?" else "The gacha picked! 😍"
                Step.GO -> "Let's makan! 💖"
                Step.PROBLEM -> "Oh no…"
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(painterResource(ynaPic), contentDescription = "Yna", modifier = Modifier.size(110.dp))
                BubbleCard(modifier = Modifier.weight(1f), color = Color.White) {
                    Text(ynaLine, style = MaterialTheme.typography.bodyMedium)
                }
            }

            when (step) {
                // ---------- PICK: mood token ----------
                Step.PICK -> {
                    FOOD_MOODS.chunked(2).forEach { pair ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            pair.forEach { mood ->
                                MoodChip(
                                    mood = mood,
                                    selected = mood in picked,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        if (mood in picked) {
                                            picked.remove(mood)
                                        } else {
                                            picked.add(mood)
                                            if (picked.size > 2) picked.removeAt(0)   // ganti yang paling lama
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (picked.isNotEmpty()) {
                        Text(
                            picked.joinToString("  ·  ") { it.menu },
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center
                        )
                    }
                    BubbleCard(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("📍 How far can you go?  ${km.toInt()} km", style = MaterialTheme.typography.bodyMedium)
                            Slider(
                                value = km,
                                onValueChange = { km = it },
                                valueRange = 1f..10f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = Ink, activeTrackColor = Pink)
                            )
                            Text("Only places open now ♥", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    BubbleButton(
                        text = if (picked.isEmpty()) "⚡ TOO HANGRY · JUST PICK" else "PULL! LET'S EAT ▶",
                        onClick = { startPull() },
                        color = if (picked.isEmpty()) BubblePink else BubbleYellow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ---------- SPIN / RESULT: kapsul ----------
                Step.SPIN, Step.RESULT -> {
                    Capsule(
                        color = token.firstOrNull()?.color ?: BubbleYellow,
                        drop = drop.value,
                        wobble = wobble.value,
                        open = open.value
                    )
                    val place = chosen
                    if (step == Step.RESULT && place != null) {
                        PlaceCard(place = place, tokenText = token.joinToString(" + ") { it.phrase })
                        BubbleButton(
                            text = "▶ See it on TikTok",
                            onClick = { links.openUri(tiktokSearch(place.name)) },
                            color = Cream,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BubbleButton("✕ Nope", onClick = { nope() }, color = BubblePink, modifier = Modifier.weight(1f))
                            BubbleButton("Let's eat ✓", onClick = { step = Step.GO }, color = BubbleGreen, modifier = Modifier.weight(1f))
                        }
                    }
                }

                // ---------- GO: direction ----------
                Step.GO -> {
                    val place = chosen
                    if (place != null) {
                        PlaceCard(place = place, tokenText = token.joinToString(" + ") { it.phrase })
                        val spot = "${place.lat},${place.lng}"
                        BubbleButton(
                            "Directions · Google Maps",
                            onClick = { links.openUri("https://www.google.com/maps/dir/?api=1&destination=$spot") },
                            color = BubbleBlue, modifier = Modifier.fillMaxWidth()
                        )
                        BubbleButton(
                            "Directions · Waze",
                            onClick = { links.openUri("https://waze.com/ul?ll=$spot&navigate=yes") },
                            color = BubbleBlue, modifier = Modifier.fillMaxWidth()
                        )
                        BubbleButton(
                            "▶ See it on TikTok",
                            onClick = { links.openUri(tiktokSearch(place.name)) },
                            color = Cream, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BubbleButton("↺ Spin again", onClick = { nope() }, color = Cream, modifier = Modifier.weight(1f))
                        BubbleButton("New token", onClick = { picked.clear(); step = Step.PICK }, modifier = Modifier.weight(1f))
                    }
                }

                // ---------- PROBLEM ----------
                Step.PROBLEM -> {
                    BubbleCard(modifier = Modifier.fillMaxWidth()) {
                        Text(problem, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    }
                    BubbleButton(
                        "🔎 Find hidden spots on TikTok",
                        onClick = { links.openUri(tiktokSearch("hidden gem " + token.joinToString(" ") { it.tiktok })) },
                        color = Cream, modifier = Modifier.fillMaxWidth()
                    )
                    if (km < 10f) {
                        BubbleButton(
                            "Search ${(km + 2f).coerceAtMost(10f).toInt()} km instead",
                            onClick = { km = (km + 2f).coerceAtMost(10f); startPull() },
                            color = BubbleBlue, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    BubbleButton("← New token", onClick = { step = Step.PICK }, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

}

/* ---------- butang mood token ---------- */
@Composable
private fun MoodChip(mood: FoodMood, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(mood.color)
            .border(if (selected) 4.dp else 2.dp, if (selected) Pink else Ink, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(mood.emoji)
        Text(mood.phrase, style = MaterialTheme.typography.labelLarge)
    }
}

/* ---------- kapsul gacha (atas warna mood, bawah krim) ---------- */
@Composable
private fun Capsule(color: Color, drop: Float, wobble: Float, open: Float) {
    Box(
        modifier = Modifier
            .size(140.dp)
            .offset(y = drop.dp)
            .graphicsLayer { rotationZ = wobble },
        contentAlignment = Alignment.Center
    ) {
        // separuh atas: terbang ke atas & senget bila terbuka
        Box(
            Modifier
                .size(width = 110.dp, height = 55.dp)
                .offset(y = (-27.5).dp)
                .graphicsLayer {
                    translationY = -60f * open
                    rotationZ = -25f * open
                }
                .clip(RoundedCornerShape(topStart = 55.dp, topEnd = 55.dp))
                .background(color)
                .border(4.dp, Ink, RoundedCornerShape(topStart = 55.dp, topEnd = 55.dp))
        )
        // separuh bawah
        Box(
            Modifier
                .size(width = 110.dp, height = 55.dp)
                .offset(y = 27.5.dp)
                .graphicsLayer {
                    translationY = 25f * open
                    rotationZ = 12f * open
                }
                .clip(RoundedCornerShape(bottomStart = 55.dp, bottomEnd = 55.dp))
                .background(Cream)
                .border(4.dp, Ink, RoundedCornerShape(bottomStart = 55.dp, bottomEnd = 55.dp))
        )
    }
}

/* ---------- kad kedai ---------- */
@Composable
private fun PlaceCard(place: Place, tokenText: String) {
    BubbleCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                tokenText,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BubbleYellow)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            Text(place.name, style = MaterialTheme.typography.titleLarge)
            Text(place.info, style = MaterialTheme.typography.bodyMedium)
            if (place.food.isNotEmpty()) {
                Text(place.food, style = MaterialTheme.typography.labelSmall, color = InkSoft)
            }
        }
    }
}

/* link carian TikTok */
private fun tiktokSearch(words: String): String =
    "https://www.tiktok.com/search?q=" + URLEncoder.encode(words.trim(), "UTF-8")
