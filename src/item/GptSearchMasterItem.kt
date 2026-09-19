package com.waveapp.tourcat.item

data class GptSearchMasterItem(
    val id: Long = 0L,
    val userUid: String,
    val userEmail: String,
    val category: String,          // "상품", "메뉴판", "리뷰" 등
    val imageUrl: String?,         // ★ 추가: 이미지 URL(없으면 null)
    val queryText: String,
    val resultText: String?,
    val status: String,            // "success", "fail"
//    val price: Int,                // 요청 1건당 기준 과금 (실패/성공 정책에 맞춤)
//    val prevCredit: Int?,          // ★ 추가: 이전 크레딧 (nullable)
//    val usedCredit: Int?,          // ★ 추가: 사용 크레딧 (nullable)
//    val finalCredit: Int?,         // ★ 추가: 최종 크레딧 (nullable)
    val requestedAt: String,
    val responseAt: String?,
    val failReason: String?
)
