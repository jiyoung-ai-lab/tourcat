package com.waveapp.tourcat.item

data class TranslationItem(
    val id: Long = 0,
    val imagePath: String = "",
    val ocrText: String = "",
    val translatedText: String = "",
    val langCode: String = "",
    val createdAt: String = ""
)
