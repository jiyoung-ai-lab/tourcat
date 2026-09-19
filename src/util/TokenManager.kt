package com.waveapp.tourcat.helper

import android.content.Context
import com.waveapp.tourcat.common.ComConstant

/**
 * 토큰(크레딧/코인) 관련 유틸리티 통합 클래스
 * - 서버에서 토큰 조회 및 Pref 반영
 * - Pref에서 토큰 즉시 조회
 * - 결제 후 서버+Pref 동시 갱신
 * 2025년 기준, 기존 로직/함수 활용, UI 변경 없음
 */
object TokenManager {

    /**
     * 서버에서 최신 토큰(크레딧) 조회 후 Pref에 저장하고 콜백 실행
     * @param context Context
     * @param userId 서버에 넘길 user_id(주로 이메일)
     * @param onComplete Pref 저장 후 실행될 콜백
     */
    fun fetchTokenAndSavePref(
        context: Context,
        userId: String,
        onComplete: (() -> Unit)? = null
    ) {
        TokenBalanceApiHelper.getBalance(
            context = context,
            userId = userId,
            onSuccess = { leftToken ->
                saveTokenToPref(context, leftToken)
                onComplete?.invoke()
            },
            onFailure = {
                // 실패 시 0으로 저장 후 콜백
                saveTokenToPref(context, 0)
                onComplete?.invoke()
            }
        )
    }

    /**
     * Pref에서 토큰(코인/크레딧) 값 즉시 반환 (0 기본값)
     */
    fun getTokenFromPref(context: Context): Int {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(ComConstant.PREF_KEYS.VALID_CREDIT, 0)
    }

    /**
     * 결제 정상 완료 후
     * - 서버에 토큰 갱신(유효성 체크)
     * - Pref 갱신
     * - 완료 콜백 실행
     * @param context Context
     * @param userId 서버에 넘길 user_id(주로 이메일)
     * @param onComplete Pref 저장 후 실행될 콜백
     */
    fun updateTokenAfterCharge(
        context: Context,
        userId: String,
        onComplete: (() -> Unit)? = null
    ) {
        TokenChargeApiHelper.checkToken(
            context = context,
            userId = userId,
            onSuccess = { leftToken ->
                saveTokenToPref(context, leftToken)
                onComplete?.invoke()
            },
            onFailure = {
                // 실패 시 0으로 저장 후 콜백
                saveTokenToPref(context, 0)
                onComplete?.invoke()
            }
        )
    }

    /**
     * Pref에 토큰(코인/크레딧) 값 저장 (내부 사용)
     */
    private fun saveTokenToPref(context: Context, token: Int) {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(ComConstant.PREF_KEYS.VALID_CREDIT, token).apply()
    }
}
