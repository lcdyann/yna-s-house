package com.example.ynashouse.gacha

import androidx.compose.ui.graphics.Color

/*
 * FOODMOODS.KT
 * Senarai mood makanan untuk gacha (sama macam website Hangry Bear).
 * Tambah / ubah mood di sini sahaja.
 *
 * Setiap mood:
 * - phrase  : ayat yang orang selalu sebut
 * - menu    : contoh makanan
 * - cuisine : tag "cuisine" OpenStreetMap yang sesuai
 * - words   : perkataan dalam nama kedai yang sesuai
 * - amenity : jenis kedai OpenStreetMap yang sesuai
 * - late    : true = kedai yang buka lewat malam pun dikira
 * - tiktok  : perkataan carian TikTok "hidden spots"
 */
data class FoodMood(
    val id: String,
    val emoji: String,
    val phrase: String,
    val color: Color,
    val menu: String,
    val cuisine: List<String>,
    val words: List<String>,
    val amenity: List<String> = emptyList(),
    val late: Boolean = false,
    val tiktok: String
)

val FOOD_MOODS = listOf(
    FoodMood(
        "rice", "🍚", "I want ricee", Color(0xFFFFE08A),
        "Anything with rice: rice bowls · chicken rice · nasi lemak",
        listOf("malaysian", "malay", "indonesian", "chinese", "thai", "japanese", "korean", "indian", "asian", "rice", "chicken_rice", "nasi_lemak", "nasi_kandar", "regional"),
        listOf("nasi", "rice", "bowl", "don", "bento", "kandar", "lemak", "campur", "briyani", "biryani", "mandi", "claypot"),
        tiktok = "nasi best"
    ),
    FoodMood(
        "soupy", "🍲", "Gimme soupy!", Color(0xFFA9E4F0),
        "Hearty & comforting: chicken soup · bubur · sup tulang · tomyam",
        listOf("noodle", "ramen", "soup", "vietnamese", "thai", "hot_pot", "korean", "chinese", "pho", "porridge"),
        listOf("sup", "soup", "bubur", "porridge", "congee", "tomyam", "tom yam", "ramen", "mee", "pho", "laksa", "steamboat", "hotpot", "bak kut teh", "soto"),
        tiktok = "bubur sup best"
    ),
    FoodMood(
        "savoury", "🍟", "Flavour bomb pls", Color(0xFFFFC89E),
        "Saucy & tasty: black pepper · cheese sauce · seasoned fries",
        listOf("western", "american", "korean", "chicken", "fried_chicken", "burger", "fries", "steak_house", "grill", "japanese"),
        listOf("fries", "western", "grill", "chop", "sauce", "cheese", "korean", "chicken", "black pepper", "loaded"),
        tiktok = "sauce sedap"
    ),
    FoodMood(
        "isi", "🍗", "Feed me meat!", Color(0xFFFFB3B3),
        "Ayam goreng · satay · burger · steak",
        listOf("burger", "chicken", "fried_chicken", "steak_house", "barbecue", "bbq", "grill", "kebab", "satay", "arab", "turkish"),
        listOf("ayam", "chicken", "satay", "sate", "steak", "bbq", "grill", "burger", "kebab", "daging", "beef", "lamb", "kambing"),
        tiktok = "ayam daging best"
    ),
    FoodMood(
        "heat", "🌶️", "Make it SPICY", Color(0xFFFF9AA2),
        "Ayam gepuk · mala · sambal anything",
        listOf("thai", "indonesian", "sichuan", "korean", "indian", "malaysian", "malay", "mala", "spicy"),
        listOf("gepuk", "penyet", "mala", "tomyam", "tom yam", "sambal", "spicy", "pedas", "curry", "kari", "thai", "tteok"),
        tiktok = "pedas best"
    ),
    FoodMood(
        "opener", "🥟", "Just a lil starter", Color(0xFFB8F0C8),
        "Dim sum · rojak · popiah · spring rolls",
        listOf("dim_sum", "tapas", "snack", "snacks", "dumpling", "salad", "sushi"),
        listOf("dim sum", "dimsum", "popiah", "rojak", "dumpling", "snack", "sushi", "cucur", "pau", "bao", "spring roll"),
        tiktok = "dim sum snack"
    ),
    FoodMood(
        "sweet", "🍦", "Sweet tooth attack", Color(0xFFFFC6E6),
        "Ice cream · bingsu · cendol · waffles",
        listOf("dessert", "ice_cream", "cake", "waffle", "donut", "crepe", "frozen_yogurt", "bubble_tea", "pancake"),
        listOf("bingsu", "cendol", "dessert", "cake", "ice cream", "aiskrim", "waffle", "donut", "crepe", "gelato", "sweet", "ais kacang"),
        amenity = listOf("ice_cream"),
        tiktok = "dessert ice cream"
    ),
    FoodMood(
        "energy", "☕", "Need a boost", Color(0xFFD4C2FF),
        "Drinks that fire you up: coffee · teh tarik · boba · smoothie",
        listOf("coffee_shop", "coffee", "tea", "bubble_tea", "juice", "smoothie"),
        listOf("kopi", "coffee", "cafe", "café", "tea", "boba", "juice", "smoothie", "kopitiam", "espresso"),
        amenity = listOf("cafe"),
        tiktok = "cafe coffee"
    ),
    FoodMood(
        "stress", "🍕", "Stress eating time", Color(0xFFE9CFB4),
        "Pizza · fried chicken · cheesy fries",
        listOf("pizza", "fried_chicken", "burger", "american", "fast_food"),
        listOf("pizza", "kfc", "mcdonald", "burger", "fried chicken", "domino", "texas", "marrybrown", "cheese"),
        amenity = listOf("fast_food"),
        tiktok = "comfort food"
    ),
    FoodMood(
        "night", "🌙", "Midnight munchies", Color(0xFFB9C3FF),
        "Roti canai · maggi goreng · burger stall",
        listOf("indian", "indian_muslim", "mamak", "malaysian"),
        listOf("mamak", "24", "roti", "maggi", "burger", "nasi kandar", "kopitiam"),
        late = true,
        tiktok = "supper malam"
    )
)

fun foodMoodById(id: String): FoodMood? = FOOD_MOODS.firstOrNull { it.id == id }
