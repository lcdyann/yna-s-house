package com.example.ynashouse.state

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.example.ynashouse.mood.Mood
import com.example.ynashouse.yna.YnaLines
import kotlin.random.Random

/*
 * YNAHOUSESTATE.KT
 * Semua "otak" Yna di satu tempat:
 * 1. Mood Diyana (bilik mana Yna duduk)
 * 2. Tiga meter: happiness, tummy, rest (0 - 100), turun perlahan ikut masa
 * 3. Need Yna bila idle (sulk / nudge / yawn)
 * 4. Care (pat, listen, snack, nap) yang naikkan meter
 * Semua disimpan dalam phone (SharedPreferences) supaya kekal bila app ditutup.
 */

/* Need: Yna minta perhatian sendiri. fixedBy = care yang selesaikan need tu */
enum class Need(val bubble: String, val fixedBy: Care) {
    SULK("🥺 pat me…", Care.PAT),
    NUDGE("💬 listen to me!", Care.LISTEN),
    YAWN("💤 so sleepy…", Care.NAP)
}

/* Care: apa yang boleh dibuat pada Yna. ms = berapa lama animasi */
enum class Care(val emoji: String, val label: String, val ms: Long) {
    PAT("🤗", "Pat", 2500),
    LISTEN("👂", "Listen", 3500),
    SNACK("🍟", "Snack", 3000),
    NAP("💤", "Nap", 4000),
    WELCOME("💖", "Hi", 2500)   // pelukan selamat datang bila buka app
}

class YnaHouseState(private val prefs: SharedPreferences) {

    /* ---------- nilai yang UI baca ---------- */
    var mood by mutableStateOf(Mood.WATCHING)
        private set
    var happiness by mutableFloatStateOf(70f)
        private set
    var tummy by mutableFloatStateOf(70f)
        private set
    var rest by mutableFloatStateOf(70f)
        private set
    var need by mutableStateOf<Need?>(null)
        private set
    var care by mutableStateOf<Care?>(null)       // animasi care yang sedang berlaku
        private set
    var careUntil by mutableLongStateOf(0L)
        private set
    var speech by mutableStateOf<String?>(null)   // ayat dalam bubble cakap Yna
        private set
    var foodMood by mutableStateOf("")             // food mood terakhir dari gacha (contoh "I want ricee")
        private set

    private var lastUpdate = System.currentTimeMillis()

    /* ---------- tetapan meter (boleh ubah) ---------- */
    private val dropPerHour = 4f          // semua meter turun 4 mata sejam
    private val restUpPerHour = 10f       // tidur / tengok drama naikkan rest
    private val tummyUpPerHour = 12f      // makan naikkan tummy
    private val needChance = 12           // setiap 30 saat, peluang 1 dalam 12 untuk need baru

    /* ---------- simpan / ambil ---------- */
    fun load() {
        mood = Mood.entries.getOrNull(prefs.getInt("mood", 0)) ?: Mood.WATCHING
        happiness = prefs.getFloat("happiness", 70f)
        tummy = prefs.getFloat("tummy", 70f)
        rest = prefs.getFloat("rest", 70f)
        foodMood = prefs.getString("foodMood", "") ?: ""
        lastUpdate = prefs.getLong("lastUpdate", System.currentTimeMillis())
    }

    fun save() {
        prefs.edit {
            putInt("mood", mood.ordinal)
            putFloat("happiness", happiness)
            putFloat("tummy", tummy)
            putFloat("rest", rest)
            putString("foodMood", foodMood)
            putLong("lastUpdate", lastUpdate)
        }
    }

    /* Bila app dibuka: kira meter yang turun masa app tutup, kemudian sambut Diyana */
    fun onAppOpen() {
        val now = System.currentTimeMillis()
        val awayHours = (now - lastUpdate) / 3_600_000f
        tick(now)
        if (awayHours > 12f) {
            need = Need.SULK                                   // lama tak datang: merajuk dulu
        } else {
            startCare(Care.WELCOME, now, "Yay, you're back! 💖")
        }
    }

    /* Dipanggil setiap 30 saat: meter berubah ikut masa + mungkin muncul need baru */
    fun tick(now: Long = System.currentTimeMillis()) {
        val hours = ((now - lastUpdate).coerceAtLeast(0L)) / 3_600_000f
        lastUpdate = now

        happiness -= dropPerHour * hours
        tummy -= dropPerHour * hours
        rest -= dropPerHour * hours
        // bilik "me time" dan makan naikkan meter sendiri
        when (mood) {
            Mood.WATCHING, Mood.SLEEPING -> rest += restUpPerHour * hours
            Mood.EATING, Mood.STRESS_EATING -> tummy += tummyUpPerHour * hours
            else -> Unit
        }
        clampMeters()

        // care dah habis? buang animasi
        if (care != null && now > careUntil) {
            care = null
            speech = null
        }
        // need baru (bukan masa tidur, bukan masa animasi care)
        if (need == null && care == null && mood != Mood.SLEEPING && Random.nextInt(needChance) == 0) {
            need = Need.entries.random()
        }
        save()
    }

    /* Diyana tukar mood (hanya owner) */
    fun changeMood(newMood: Mood) {
        tick()
        mood = newMood
        save()
    }

    /* Diyana set meter ikut mood sebenar (slider) */
    fun setMeters(newHappiness: Float, newTummy: Float, newRest: Float) {
        happiness = newHappiness
        tummy = newTummy
        rest = newRest
        clampMeters()
        save()
    }

    /* Food mood terakhir (dari gacha) */
    fun saveFoodMood(text: String) {
        foodMood = text
        save()
    }

    /* Care: pat / listen / snack / nap */
    fun doCare(what: Care, now: Long = System.currentTimeMillis()) {
        val solvedNeed = need?.fixedBy == what
        when (what) {
            Care.PAT -> happiness += if (solvedNeed) 15f else 6f
            Care.LISTEN -> happiness += if (solvedNeed) 15f else 5f
            Care.SNACK -> { tummy += 12f; happiness += 2f }
            Care.NAP -> { rest += if (solvedNeed) 15f else 8f; happiness += 2f }
            Care.WELCOME -> Unit
        }
        if (solvedNeed) need = null
        clampMeters()

        val line = when (what) {
            Care.PAT -> YnaLines.pat.random()
            Care.LISTEN -> YnaLines.yap.random()
            Care.SNACK -> YnaLines.snack.random()
            Care.NAP -> "Zzz… 💤"
            Care.WELCOME -> null
        }
        startCare(what, now, line)
        save()
    }

    private fun startCare(what: Care, now: Long, line: String?) {
        care = what
        careUntil = now + what.ms
        speech = line
    }

    private fun clampMeters() {
        happiness = happiness.coerceIn(0f, 100f)
        tummy = tummy.coerceIn(0f, 100f)
        rest = rest.coerceIn(0f, 100f)
    }
}
