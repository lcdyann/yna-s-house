package com.example.ynashouse.watch

import com.example.ynashouse.posters.PosterStore

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
    val status: ShowStatus,
    val poster: String? = null      // laluan fail poster dalam phone (null = belum ada)
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
        // kalau data rosak, jangan crash — mula dengan senarai kosong
        try {
            val array = JSONArray(saved)
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                shows += Show(
                    title = o.getString("title"),
                    kind = ShowKind.valueOf(o.getString("kind")),
                    reaction = Reaction.valueOf(o.getString("reaction")),
                    status = ShowStatus.valueOf(o.getString("status")),
                    poster = o.optString("poster").ifEmpty { null }
                )
            }
        } catch (e: Exception) {
            shows.clear()
        }
    }

    fun add(show: Show) {
        shows.add(0, show)        // yang baru di atas
        save()
    }

    fun remove(show: Show) {
        shows.remove(show)
        PosterStore.delete(show.poster)
        save()
    }

    /* letak / tukar poster cerita (cari ikut tajuk sebab objek Show mungkin dah berubah) */
    fun setPoster(title: String, path: String) {
        val i = shows.indexOfFirst { it.title == title }
        if (i >= 0) {
            val old = shows[i].poster
            shows[i] = shows[i].copy(poster = path)
            if (old != path) PosterStore.delete(old)
            save()
        } else {
            PosterStore.delete(path)
        }
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
                    .put("poster", s.poster ?: "")
            )
        }
        prefs.edit { putString("shows", array.toString()) }
    }
}
