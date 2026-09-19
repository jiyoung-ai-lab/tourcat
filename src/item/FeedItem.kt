package com.waveapp.tourcat.item

data class FeedItem(
    val planId: Long? = null,
    val imageResId: Int? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val country: String? = null,
    val city: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val period: String? = null,
    val weatherYear: Int? = null,
    val weatherMonth: Int? = null,
    val tempAvg: Double? = null,
    val weatherSummary: String? = null,
    val exchangeRate: String? = null,
    val holidayInfo: String? = null,
    val festivalInfo: String? = null,
    val weatherLink: String? = null,
    val exchangeLink: String? = null,
    val mapLink: String? = null,
    val travelTimeDisplay: String? = null  // 여행지 시간 정보 추가!
)
