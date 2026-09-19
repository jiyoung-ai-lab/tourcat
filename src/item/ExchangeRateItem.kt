package com.waveapp.tourcat.item

data class ExchangeRateItem(
    val baseCurrency: String,
    val baseCountryCode: String,
    val targetCurrency: String,
    val targetCountryCode: String,
    val targetSymbol: String?,
    val rate: Double,
    val refDate: String,
    val version: Int = 1,
    val regDate: String = "" // ISO8601 등, 예: 2024-07-11T11:33:50
)
