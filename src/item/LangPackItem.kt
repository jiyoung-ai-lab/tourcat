package com.waveapp.tourcat.item

data class LangPackItem(
    val name: String,
    val code: String,
    var isInstalled: Boolean = false,
    var isChecked: Boolean = false,    // 체크박스 UI와 동기화
    var isInProgress: Boolean = false // ← 작업 중 표시용 필드

)
