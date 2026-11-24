package com.example.ynashouse.watch

import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/*
 * WATCHLIST.KT
 * Senarai tontonan Diyana untuk bilik Theater.
 * - Now watching : yang sedang ditonton (boleh lebih dari satu)
 * - Recent       : yang dah ditonton baru-baru ini
 * - Favourites   : kegemaran sepanjang zaman
 * Disimpan dalam phone sebagai JSON.
 */

enum class ShowKind(val emoji: String, val label: String) {
    ANIME("🎌", "Anime"),
    CDRAMA("🏮", "C-drama"),
    OTHER("🎬", "Other")
}

/* reaksi Yna pada cerita tu */
enum class Reaction(val emoji: String, val label: String) {
    LAUGH("😂", "Funny"),
    CRY("😭", "Crying"),
    SHOCK("😱", "Shocked"),
    LOVE("😍", "In love")
}

/* status cerita dalam senarai */
enum class ShowStatus(val label: String) {
    NOW("Now watching"),
    RECENT("Recent watches"),
    FAVOURITE("All-time favourites")
}

data class Show(
    val title: String,
    val kind: ShowKind,
    val reaction: Reaction,
    val status: ShowStatus
)

class WatchListStore(private val prefs: SharedPreferences) {

    /* senarai yang UI baca (berubah = skrin auto update) */
    val shows = mutableStateListOf<Show>()

    fun load() {
        shows.clear()
        val saved = prefs.getString("shows", null)
        if (saved == null) {
            // kali pertama: isi dengan kegemaran Diyana
            shows += Show("Avatar", ShowKind.OTHER, Reaction.LOVE, ShowStatus.FAVOURITE)
            shows += Show("Ejen Ali", ShowKind.OTHER, Reaction.LOVE, ShowStatus.FAVOURITE)
            shows += Show("Kaiju No. 8", ShowKind.ANIME, Reaction.SHOCK, ShowStatus.FAVOURITE)
            save()
            return
        }
        val array = JSONArray(saved)
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            shows += Show(
                title = o.getString("title"),
                kind = ShowKind.valueOf(o.getString("kind")),
                reaction = Reaction.valueOf(o.getString("reaction")),
                status = ShowStatus.valueOf(o.getString("status"))
            )
        }
    }

    fun add(show: Show) {
        shows.add(0, show)        // yang baru di atas
        save()
    }

    fun remove(show: Show) {
        shows.remove(show)
        save()
    }

    /* tukar status: contoh "Now watching" → "Recent" bila dah habis tengok */
    fun move(show: Show, to: ShowStatus) {
        val i = shows.indexOf(show)
        if (i >= 0) {
            shows[i] = show.copy(status = to)
            save()
        }
    }

    /* cerita pertama yang sedang ditonton (untuk butang Theater) */
    fun nowWatching(): Show? = shows.firstOrNull { it.status == ShowStatus.NOW }

    private fun save() {
        val array = JSONArray()
        shows.forEach { s ->
            array.put(
                JSONObject()
                    .put("title", s.title)
                    .put("kind", s.kind.name)
                    .put("reaction", s.reaction.name)
                    .put("status", s.status.name)
            )
        }
        prefs.edit { putString("shows", array.toString()) }
    }
}
