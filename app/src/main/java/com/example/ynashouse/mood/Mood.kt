package com.example.ynashouse.mood

/*
 * MOOD.KT
 * Mood sebenar Diyana. Setiap mood = satu bilik dalam rumah (lihat HouseRoom.kt).
 * Nak tambah mood baru: tambah di sini, kemudian sambungkan ke bilik dalam HouseRoom.kt.
 */
enum class Mood(
    val label: String,   // nama mood yang orang nampak
    val emoji: String,   // emoji pada butang
    val nowText: String  // ayat pada butang "tukar mood" (contoh: "I'm gaming now")
) {
    WATCHING("Watching", "🎬", "I'm watching now"),
    CODING("Busy coding", "💻", "I'm coding now"),
    GAMING("Gaming", "🎮", "I'm gaming now"),
    STRESS_EATING("Stress eating", "🍟", "I'm stress eating"),
    EATING("Eating", "🍽️", "I'm eating now"),
    SLEEPING("Sleeping", "😴", "Me time: sleeping")
}
