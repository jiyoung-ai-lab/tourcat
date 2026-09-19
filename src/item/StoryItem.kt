package com.waveapp.tourcat.item

import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.Nation

data class StoryItem(
    val type: StoryType,
    val startDate: String? = null,
    val endDate: String? = null,
    val nation: String = "",
    val city: String = "",
    val imageResId: Int = R.drawable.ic_placeholder ,    // 기본 이미지
    val planId: Long? = null    // ← 추가!
)

enum class StoryType {
    REGISTER, // 등록용
    TRAVEL    // 여행 정보용(나중에 여행등록 정보로)
}