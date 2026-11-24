package com.example.ynashouse.yna

import androidx.annotation.DrawableRes
import com.example.ynashouse.R
import com.example.ynashouse.mood.Mood
import com.example.ynashouse.state.Care
import com.example.ynashouse.state.Need
import com.example.ynashouse.state.YnaHouseState
import com.example.ynashouse.watch.Reaction

/*
 * YNAPOSE.KT
 * Pilih gambar Yna yang patut keluar SEKARANG.
 *
 * Peraturan (yang paling atas menang):
 * 1. Sedang dijaga (pat, listen, snack, nap, welcome)
 * 2. Need bila idle (sulk, nudge, yawn)
 * 3. Meter sangat rendah (lapar / sedih) - kecuali masa tidur
 * 4. Aktiviti ikut mood (bilik)
 *
 * "now" berubah setiap 100ms, jadi gambar bertukar frame untuk animasi kecil.
 * "showReaction" = reaksi cerita yang sedang ditonton (Theater), supaya Yna ikut emosi cerita.
 */
@DrawableRes
fun ynaPose(state: YnaHouseState, now: Long, showReaction: Reaction? = null): Int {

    // 1. sedang dijaga
    val care = state.care
    if (care != null && now < state.careUntil) {
        return when (care) {
            Care.PAT -> R.drawable.yna_patted
            Care.LISTEN -> frames(now, 300, R.drawable.yna_yap_1, R.drawable.yna_yap_2)
            Care.SNACK -> if (now > state.careUntil - care.ms + 700) {
                frames(now, 400, R.drawable.yna_eat_1, R.drawable.yna_eat_2)
            } else {
                R.drawable.yna_snack            // tangkap snack dulu, lepas tu makan
            }
            Care.NAP -> frames(now, 1200, R.drawable.yna_sleep_1, R.drawable.yna_sleep_2)
            Care.WELCOME -> R.drawable.yna_excited
        }
    }

    // 2. need bila idle
    when (state.need) {
        Need.SULK -> return R.drawable.yna_sulk
        Need.NUDGE -> return R.drawable.yna_nudge
        Need.YAWN -> return R.drawable.yna_yawn
        null -> Unit
    }

    // 3. meter sangat rendah (bukan masa tidur)
    if (state.mood != Mood.SLEEPING) {
        if (state.tummy < 10f) return R.drawable.yna_starving
        if (state.tummy < 20f) return R.drawable.yna_hungry
        if (state.happiness < 20f) return R.drawable.yna_teary
    }

    // 4. aktiviti ikut mood
    return when (state.mood) {
        Mood.WATCHING -> {
            // reaksi ikut cerita yang sedang ditonton (kalau ada)
            val react = when (showReaction) {
                Reaction.LAUGH, Reaction.LOVE -> R.drawable.yna_watch_laugh
                Reaction.CRY -> R.drawable.yna_watch_cry
                Reaction.SHOCK -> R.drawable.yna_watch_shock
                null -> null
            }
            val reactions = if (react != null) {
                listOf(R.drawable.yna_watch, react, R.drawable.yna_watch, react, react)
            } else {
                // tiada cerita: tengok biasa, sekali-sekala ketawa / terkejut / menangis
                listOf(
                    R.drawable.yna_watch, R.drawable.yna_watch, R.drawable.yna_watch_laugh,
                    R.drawable.yna_watch, R.drawable.yna_watch, R.drawable.yna_watch_shock,
                    R.drawable.yna_watch, R.drawable.yna_watch_cry
                )
            }
            reactions[((now / 3500) % reactions.size).toInt()]
        }
        Mood.CODING -> {
            // jeling dulu, lepas tu taip lama, sekali-sekala "crash" atas laptop
            when (((now / 3000) % 10).toInt()) {
                0 -> R.drawable.yna_code_eyeroll
                9 -> R.drawable.yna_code_crash
                else -> frames(now, 250, R.drawable.yna_code_type_1, R.drawable.yna_code_type_2)
            }
        }
        Mood.GAMING -> R.drawable.yna_game
        Mood.STRESS_EATING -> frames(now, 450, R.drawable.yna_stress_eat_1, R.drawable.yna_stress_eat_2)
        Mood.EATING -> frames(now, 600, R.drawable.yna_eat_1, R.drawable.yna_eat_2)
        Mood.SLEEPING -> frames(now, 1400, R.drawable.yna_sleep_1, R.drawable.yna_sleep_2)
    }
}

/* tukar antara beberapa gambar setiap "everyMs" milisaat */
@DrawableRes
private fun frames(now: Long, everyMs: Long, vararg pics: Int): Int =
    pics[((now / everyMs) % pics.size).toInt()]
