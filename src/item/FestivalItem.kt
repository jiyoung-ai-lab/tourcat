package com.waveapp.tourcat.item

data class FestivalItem(
    val country: String,
    val city: String,
    val year: Int,       // <- 추가!
    val month: Int,
    val day: Int,
    val version: Int,
    val en: String?,
    val ko: String?,
    val ja: String?,
    val zh: String?,
    val fr: String?,
    val de: String?,
    val it: String?,
    val th: String?,
    val es: String?,
    val regDate: String   // 반드시 regDate까지 포함
)

fun FestivalItem.getLocalizedSummary(langcode: String): String? {
    return when (langcode) {
        "ko" -> ko
        "en" -> en
        "ja" -> ja
        "zh" -> zh
        "fr" -> fr
        "de" -> de
        "it" -> this.it
        "th" -> th
        "es" -> es
        else -> en // fallback
    }
}
