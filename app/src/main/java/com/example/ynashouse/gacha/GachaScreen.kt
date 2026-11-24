package com.example.ynashouse.gacha

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import com.example.ynashouse.state.LocalVisitorMode
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.ui.bubble.BubbleButton
import com.example.ynashouse.ui.bubble.BubbleCard
import com.example.ynashouse.ui.theme.BubbleBlue
import com.example.ynashouse.ui.theme.BubbleGreen
import com.example.ynashouse.ui.theme.BubblePink
import com.example.ynashouse.ui.theme.BubbleYellow
import com.example.ynashouse.ui.theme.Cream
import com.example.ynashouse.ui.theme.Ink
import com.example.ynashouse.ui.theme.deepOf
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import com.example.ynashouse.ui.theme.InkSoft
import com.example.ynashouse.ui.theme.Pink
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
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
    val visitor = LocalVisitorMode.current      // pelawat: gacha untuk diri sendiri, tak tukar food mood Diyana

    var step by remember { mutableStateOf(Step.PICK) }
    val picked = remember { mutableStateListOf<FoodMood>() }
    var km by remember { mutableFloatStateOf(2f) }
    var token by remember { mutableStateOf<List<FoodMood>>(emptyList()) }

    var places by remember { mutableStateOf<List<Place>>(emptyList()) }
    var usingBackup by remember { mutableStateOf(false) }
    val seen = remember { mutableStateListOf<String>() }
    var chosen by remember { mutableStateOf<Place?>(null) }
    var problem by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Ooh spinning… 🎰") }   // langkah carian sekarang
    var searchJob by remember { mutableStateOf<Job?>(null) }        // carian yang sedang berjalan (untuk Cancel)

    // animasi mesin MAMAM♥TIME (token masuk, mesin pusing, kapsul keluar)
    val machine = rememberMachineAnim()
    var tokenRolling by remember { mutableStateOf(false) }          // true = "Clink! Token in…"
    val scroll = rememberScrollState()

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

    /* tunjuk kad kedai & skrol ke bawah supaya kad nampak */
    suspend fun showResult() {
        step = Step.RESULT
        delay(150)
        scroll.animateScrollTo(scroll.maxValue)
    }

    /* PULL: cari kedai. Kapsul terus berguling sampai carian siap (paling lama 40 saat) */
    fun pull() {
        if (picked.isNotEmpty() || token.isEmpty()) {
            token = picked.toList().ifEmpty { listOf(FOOD_MOODS.random()) }   // tiada mood: Yna pilih
        }
        if (!visitor) state.saveFoodMood(token.joinToString(" + ") { it.phrase })
        step = Step.SPIN
        status = "Ooh spinning… 🎰"
        seen.clear()
        searchJob = scope.launch {
            val search = async {
                runCatching { withTimeout(40_000L) { findPlaces(context, token, km) { status = it } } }
            }
            // token bergolek masuk, kemudian mesin berpusing selagi masih mencari
            machine.reset()
            scroll.animateScrollTo(0)
            tokenRolling = true
            machine.insertToken()
            tokenRolling = false
            machine.spinUntil { search.isCompleted }

            val result = search.await()
            result.onSuccess { found ->
                usingBackup = found.list.isEmpty() && found.backup.isNotEmpty()
                places = if (found.list.isNotEmpty()) found.list else found.backup
                if (places.isEmpty()) {
                    problem = "No food places found within ${km.toInt()} km 😢"
                    step = Step.PROBLEM
                } else {
                    chosen = pickPlace()
                    machine.dropCapsule()               // kapsul keluar & terbuka bila jumpa kedai
                    machine.openCapsule()
                    showResult()
                }
            }.onFailure { error ->
                problem = when (error) {
                    is NoLocationException ->
                        "I can't find where you are 🥺 Turn on Location (GPS) in your phone settings and try again."
                    is TimeoutCancellationException ->
                        "The food map is taking too long to answer 🥺 Please try again in a bit."
                    is NoPlacesException ->
                        if (error.total > 0) "Found ${error.total} places within ${km.toInt()} km, but none are open now 😢"
                        else "No food places on the map within ${km.toInt()} km 😢 Try a bigger distance."
                    else ->
                        "Can't reach the food map right now 🥺 Check your internet and try again.\n(${error.message})"
                }
                step = Step.PROBLEM
            }
        }
    }

    /* Nope: mesin pusing lagi, kapsul baru (token sama) */
    fun nope() {
        step = Step.SPIN
        status = "Hmm, something else then…"
        scope.launch {
            scroll.animateScrollTo(0)
            machine.spinUntil { true }
            chosen = pickPlace()
            machine.dropCapsule()
            machine.openCapsule()
            showResult()
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
                .background(Brush.verticalGradient(listOf(Color(0xFFFFE1F0), Color(0xFFFFF4E0))))
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scroll)
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
                    Text("🎰 MAMAM♥TIME", style = MaterialTheme.typography.titleMedium, color = Pink)
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
                Step.SPIN -> if (tokenRolling) "Clink! Token in… ♥" else status
                Step.RESULT -> if (usingBackup) "Couldn't find exactly that… but how about this?" else "The gacha picked! 😍"
                Step.GO -> "Let's makan! 💖"
                Step.PROBLEM -> "Oh no…"
            }
            val atMachine = step == Step.SPIN || step == Step.RESULT
            if (atMachine) {
                // bubble sahaja (Yna duduk sebelah mesin di bawah)
                BubbleCard(color = Color.White) {
                    Text(ynaLine, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Image(painterResource(ynaPic), contentDescription = "Yna", modifier = Modifier.size(110.dp))
                    BubbleCard(modifier = Modifier.weight(1f), color = Color.White) {
                        Text(ynaLine, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            when (step) {
                // ---------- PICK: mood token ----------
                Step.PICK -> {
                    // food mood Diyana (1-2 terkini) — pelawat boleh pilih sama
                    if (state.foodMood.isNotEmpty()) {
                        BubbleCard(modifier = Modifier.fillMaxWidth(), color = Color.White) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Yna's craving", style = MaterialTheme.typography.labelSmall)
                                Text(state.foodMood, style = MaterialTheme.typography.titleMedium, color = Pink)
                                if (state.foodMoodBefore.isNotEmpty()) {
                                    Text("before: ${state.foodMoodBefore}", style = MaterialTheme.typography.bodySmall)
                                }
                                val herMoods = FOOD_MOODS.filter { it.phrase in state.foodMood.split(" + ") }.take(2)
                                if (herMoods.isNotEmpty()) {
                                    BubbleButton(
                                        text = if (visitor) "Crave the same as Yna" else "Same as last time",
                                        onClick = { picked.clear(); picked.addAll(herMoods) },
                                        color = BubblePink,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
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
                                colors = SliderDefaults.colors(thumbColor = Pink, activeTrackColor = Pink)
                            )
                            Text("Only places open now ♥", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    // butang gacha comel: pusing tombol = tarik (token baru setiap kali)
                    GachaKnob(
                        label = if (picked.isEmpty()) "Too hangry · just pick!" else "PULL! Let's eat",
                        onPull = { token = emptyList(); startPull() }
                    )
                }

                // ---------- SPIN / RESULT: mesin MAMAM♥TIME ----------
                Step.SPIN, Step.RESULT -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        // Yna tengok mesin (melantun sikit)
                        Image(
                            painterResource(ynaPic),
                            contentDescription = "Yna",
                            modifier = Modifier
                                .padding(top = 150.dp)
                                .size(100.dp)
                        )
                        MakanponMachine(
                            anim = machine,
                            tokenText = token.joinToString(" ") { it.emoji },
                            ballColor = token.firstOrNull()?.color ?: BubbleYellow
                        )
                    }
                    // masa mencari: boleh batal
                    if (step == Step.SPIN && searchJob?.isActive == true) {
                        BubbleButton(
                            "Cancel",
                            onClick = { searchJob?.cancel(); step = Step.PICK },
                            color = Cream,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
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
                    BubbleButton("↺ Try again", onClick = { startPull() }, color = BubbleYellow, modifier = Modifier.fillMaxWidth())
                    BubbleButton("← New token", onClick = { step = Step.PICK }, color = Cream, modifier = Modifier.fillMaxWidth())
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
            .height(50.dp)
            .scale(if (selected) 1.03f else 1f)
            .shadow(if (selected) 8.dp else 3.dp, RoundedCornerShape(50), ambientColor = deepOf(mood.color), spotColor = deepOf(mood.color))
            .clip(RoundedCornerShape(50))
            .background(Brush.verticalGradient(listOf(mood.color, deepOf(mood.color))))
            .border(if (selected) 3.dp else 2.dp, if (selected) Pink else Color.White, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(mood.emoji)
        Text(mood.phrase, style = MaterialTheme.typography.labelLarge)
    }
}

/* ---------- kad kedai ---------- */
@Composable
private fun PlaceCard(place: Place, tokenText: String) {
    BubbleCard(modifier = Modifier.fillMaxWidth(), color = Color.White) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                tokenText,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
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
