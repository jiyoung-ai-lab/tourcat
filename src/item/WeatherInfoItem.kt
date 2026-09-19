package com.waveapp.tourcat.item

data class WeatherInfoItem(
    val city: String,
    val month: Int,
    val referenceYear: Int,
    val tempAvg: Double?,
    val version: Int,
    val regDate: String,
    val en: String?,
    val ko: String?,
    val ja: String?,
    val zh: String?,
    val fr: String?,
    val de: String?,
    val it: String?,
    val th: String?,
    val es: String?
)

fun WeatherInfoItem.getLocalizedSummary(langcode: String): String? {
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

