package com.example.ynashouse.gacha

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import kotlin.coroutines.resume

/*
 * FOODPLACES.KT
 * Cari kedai makan berdekatan (percuma, tiada API key)
 * 1. Lokasi phone
 * 2. Ambil kedai dari OpenStreetMap (Overpass API)
 * 3. Waktu buka: buka sekarang ke tak?
 * 4. Padankan kedai dengan mood
 * 5. findPlaces(): fungsi utama yang gacha panggil
 */

data class Place(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val km: Float,
    val tags: Map<String, String>,
    val openNow: Boolean?,      // true / false / null (tak tahu)
    val openLate: Boolean
) {
    /* "1.2 km · Open now" */
    val info: String
        get() {
            val distance = if (km < 1f) "${(km * 1000).toInt()} m" else "%.1f km".format(km)
            val hours = if (openNow == true) "Open now" else "Hours unknown, check first"
            return "$distance · $hours"
        }

    /* apa yang kedai jual (dari tag cuisine) */
    val food: String
        get() = tags["cuisine"].orEmpty()
            .split(';', ',')
            .map { it.trim().replace('_', ' ') }
            .filter { it.isNotEmpty() }
            .take(3)
            .joinToString(" · ")
}

/* hasil carian: list = padan mood, backup = semua kedai dekat (guna bila list kosong) */
data class PlaceResult(val list: List<Place>, val backup: List<Place>)

class NoLocationException : Exception()

/* ---------- 1. Lokasi phone ---------- */
@SuppressLint("MissingPermission")    // kebenaran diminta dalam GachaScreen sebelum panggil ni
suspend fun getLocation(context: Context): Location? = withContext(Dispatchers.Main) {
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = lm.getProviders(true)
    val last = providers.mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
    // lokasi lama (kurang 10 minit) cukup baik
    if (last != null && System.currentTimeMillis() - last.time < 10 * 60_000L) {
        return@withContext last
    }
    val provider = when {
        LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
        LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
        else -> return@withContext last
    }
    // minta lokasi baru, tunggu paling lama 15 saat
    val fresh = withTimeoutOrNull(15_000L) {
        suspendCancellableCoroutine<Location> { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
                @Deprecated("Untuk phone lama sahaja")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }
            lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
        }
    }
    fresh ?: last
}

/* ---------- 2. Kedai dari OpenStreetMap ---------- */
private val OVERPASS_SERVERS = listOf(
    "https://overpass-api.de/api/interpreter",
    "https://overpass.kumi.systems/api/interpreter"
)

suspend fun loadPlaces(here: Location, km: Float): List<Place> = withContext(Dispatchers.IO) {
    val query = "[out:json][timeout:25];" +
        "nwr[\"amenity\"~\"^(restaurant|fast_food|cafe|food_court|ice_cream)$\"][\"name\"]" +
        "(around:${(km * 1000).toInt()},${here.latitude},${here.longitude});" +
        "out center tags 400;"

    for (server in OVERPASS_SERVERS) {
        try {
            val conn = URL(server).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.setRequestProperty("User-Agent", "YnasHouse/1.0")
            conn.outputStream.use { it.write(("data=" + URLEncoder.encode(query, "UTF-8")).toByteArray()) }
            if (conn.responseCode == 200) {
                val text = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                return@withContext parsePlaces(text, here)
            }
            conn.disconnect()
        } catch (e: Exception) {
            // cuba server seterusnya
        }
    }
    throw IOException("no-internet")
}

private fun parsePlaces(json: String, here: Location): List<Place> {
    val elements = JSONObject(json).getJSONArray("elements")
    val out = ArrayList<Place>()
    for (i in 0 until elements.length()) {
        val el = elements.getJSONObject(i)
        val tagsJson = el.optJSONObject("tags") ?: continue
        val tags = HashMap<String, String>()
        tagsJson.keys().forEach { key -> tags[key] = tagsJson.optString(key) }
        val name = tags["name"] ?: continue

        // titik tengah (untuk "way" ada "center")
        val center = el.optJSONObject("center")
        val lat = center?.optDouble("lat") ?: el.optDouble("lat")
        val lng = center?.optDouble("lon") ?: el.optDouble("lon")
        if (lat.isNaN() || lng.isNaN()) continue

        val distance = FloatArray(1)
        Location.distanceBetween(here.latitude, here.longitude, lat, lng, distance)
        val hours = checkHours(tags["opening_hours"])

        out += Place(
            id = el.optString("type") + el.optLong("id"),
            name = name,
            lat = lat,
            lng = lng,
            km = distance[0] / 1000f,
            tags = tags,
            openNow = hours.first,
            openLate = hours.second
        )
    }
    return out
}

/* ---------- 3. Waktu buka ----------
   Faham format biasa: "24/7", "Mo-Su 10:00-22:00", "Mo-Fr 08:00-17:00; Sa 09:00-14:00", "18:00-02:00"
   Balik: (buka sekarang? true/false/null, buka lewat malam?) */
private val DAY_NAMES = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
private val RULE = Regex("""^((?:(?:Mo|Tu|We|Th|Fr|Sa|Su|PH)(?:-(?:Mo|Tu|We|Th|Fr|Sa|Su))?,?\s*)+)?\s*(.*)$""")
private val TIME_RANGE = Regex("""^(\d{1,2}):(\d{2})\s*-\s*(\d{1,2}):(\d{2})\+?$""")

fun checkHours(text: String?): Pair<Boolean?, Boolean> {
    if (text.isNullOrBlank()) return Pair(null, false)
    val clean = text.trim()
    if (clean == "24/7") return Pair(true, true)

    // jadual 7 hari: setiap hari ada senarai (mula, tamat) dalam minit
    val week = Array(7) { emptyList<Pair<Int, Int>>() }
    var anyRule = false
    for (part in clean.split(';')) {
        val rule = part.trim()
        if (rule.isEmpty()) continue
        val m = RULE.find(rule) ?: return Pair(null, false)
        val days = m.groupValues[1].takeIf { it.isNotBlank() }?.let { readDays(it) } ?: (0..6).toList()
        val timesText = m.groupValues[2].trim()
        val times = ArrayList<Pair<Int, Int>>()
        if (!timesText.equals("off", true) && !timesText.equals("closed", true)) {
            for (piece in timesText.split(',')) {
                val t = TIME_RANGE.find(piece.trim()) ?: return Pair(null, false)   // format pelik: tak tahu
                val (h1, m1, h2, m2) = t.destructured
                times += Pair(h1.toInt() * 60 + m1.toInt(), h2.toInt() * 60 + m2.toInt())
            }
        }
        days.forEach { week[it] = times }     // peraturan kemudian menang
        anyRule = true
    }
    if (!anyRule) return Pair(null, false)

    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK) - 1          // 0 = Ahad
    val yesterday = (today + 6) % 7
    val mins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

    var open = week[today].any { (start, end) ->
        val realEnd = if (end <= start) end + 24 * 60 else end   // tutup lepas tengah malam
        mins in start until realEnd
    }
    // semalam yang tutup lepas tengah malam (contoh 18:00-02:00)
    if (week[yesterday].any { (start, end) -> end <= start && mins < end }) open = true

    val late = week.any { day -> day.any { (start, end) -> end <= start || end >= 23 * 60 } }
    return Pair(open, late)
}

/* "Mo-Fr,Su" -> [1,2,3,4,5,0] */
private fun readDays(text: String): List<Int> {
    val out = ArrayList<Int>()
    for (piece in text.split(',')) {
        val p = piece.trim()
        if (p.isEmpty() || p == "PH") continue
        val ends = p.split('-')
        val a = DAY_NAMES.indexOf(ends[0])
        val b = if (ends.size > 1) DAY_NAMES.indexOf(ends[1]) else a
        if (a == -1 || b == -1) continue
        var d = a
        while (true) {
            out += d
            if (d == b) break
            d = (d + 1) % 7
        }
    }
    return out
}

/* ---------- 4. Padan mood ---------- */
fun matchesMood(place: Place, mood: FoodMood): Boolean {
    val cuisines = place.tags["cuisine"].orEmpty().lowercase().split(';', ',').map { it.trim() }
    val name = (place.name + " " + place.tags["name:en"].orEmpty()).lowercase()
    if (place.tags["amenity"] in mood.amenity) return true
    if (mood.late && place.openLate) return true
    if (mood.cuisine.any { it in cuisines }) return true
    if (mood.words.any { name.contains(it) }) return true
    return false
}

/* ---------- 5. findPlaces ---------- */
suspend fun findPlaces(context: Context, moods: List<FoodMood>, km: Float): PlaceResult {
    val here = getLocation(context) ?: throw NoLocationException()
    val near = loadPlaces(here, km).filter { it.km <= km && it.openNow != false }   // buang yang confirm tutup
    val list = near.filter { place -> moods.any { matchesMood(place, it) } }
    return PlaceResult(list = list, backup = near)
}
