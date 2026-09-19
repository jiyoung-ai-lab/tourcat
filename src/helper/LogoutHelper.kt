package com.waveapp.tourcat.helper

import android.app.Activity
import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.waveapp.tourcat.common.ComConstant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object LogoutHelper {
    /**
     * 구글/파이어베이스 로그아웃 표준 처리 함수
     * - 모든 인증 상태 및 Pref 클리어
     * - 로그아웃 완료 후 콜백(예: 화면 이동 등) 호출 가능
     */
    fun performLogout(context: Context, onComplete: (() -> Unit)? = null) {
        // 1. Firebase 인증 세션 종료
        Firebase.auth.signOut()
        // 2. CredentialManager 상태 초기화 (비동기)
        val credentialManager = CredentialManager.create(context)
        CoroutineScope(Dispatchers.Main).launch {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                // 일부 디바이스에서 예외 발생 가능, 무시해도 됨
            }
            // 3. Pref 로그인 관련 항목만 초기화
            val prefs = context.getSharedPreferences(
                ComConstant.PREF_NAME,
                Context.MODE_PRIVATE
            )
            prefs.edit()
                .remove(ComConstant.PREF_KEYS.ID_TOKEN)
                .remove(ComConstant.PREF_KEYS.USER_UID)
                .remove(ComConstant.PREF_KEYS.USER_EMAIL)
                .remove(ComConstant.PREF_KEYS.IS_LOGGED_IN)
                .remove(ComConstant.PREF_KEYS.LAST_LOGIN_TIME)
                .remove(ComConstant.PREF_KEYS.VALID_CREDIT) // 크레딧 잔액도 삭제
                .apply()
            // 4. 콜백 처리 (예: 화면 이동, 토스트 등)
            onComplete?.invoke()
        }
    }
}
