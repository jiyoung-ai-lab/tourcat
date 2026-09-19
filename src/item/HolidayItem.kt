package com.waveapp.tourcat.item

data class HolidayItem(
    val year: Int,
    val month: Int,
    val day: Int,
    val country: String,
    val version: Int,
    val type: String?,
    val en: String?,
    val ko: String?,
    val ja: String?,
    val zh: String?,
    val fr: String?,
    val de: String?,
    val it: String?,
    val th: String?,
    val es: String?,
    val regDate: String
)

fun HolidayItem.getLocalizedSummary(langcode: String): String? {
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
