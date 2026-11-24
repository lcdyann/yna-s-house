package com.example.ynashouse.links

/*
 * MYLINKS.KT
 * Link & kunci peribadi. Tukar di sini sahaja.
 *
 * SITE      : alamat website portfolio (rumah Yna untuk pelawat ada di SITE/yna.html)
 * OWNER_KEY : perkataan rahsia yang SAMA dengan YNA_OWNER_KEY dalam Vercel
 *             (Vercel → project → Settings → Environment Variables). Jangan kongsi.
 *
 * Spotify: tak perlu tampal link lagi — playlist, lagu terkini & top songs datang sendiri
 * dari website selepas Spotify disambung sekali (lihat api/yna-spotify.js dalam portfolio).
 * SPOTIFY_PLAYLIST hanya dipakai kalau Spotify belum disambung.
 */
object MyLinks {
    const val SITE = "https://diyananadra.vercel.app"
    const val OWNER_KEY = "yna-loves-fries-and-cola"

    const val SPOTIFY_PLAYLIST = "https://open.spotify.com/playlist/PASTE_YOUR_PLAYLIST_ID"
    const val SPOTIFY_PROFILE = "https://open.spotify.com/user/PASTE_YOUR_USER_ID"

    /* link / kunci belum ditukar? */
    fun isPlaceholder(link: String) = link.contains("PASTE_YOUR")
}
