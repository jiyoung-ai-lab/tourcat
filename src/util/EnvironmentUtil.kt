package com.waveapp.tourcat.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.waveapp.tourcat.common.ComConstant
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class FCMTokenData(
    val language: String = "",
    val updated_at: String = ""
)


object EnvironmentUtil {

    private val db = FirebaseDatabase.getInstance()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Asia/Seoul")
    }


//    //안드로이드 id(앱설치 고객관리 : 무료회원 관리에 필요)
//    fun getAndroidId(context: Context): String {
//        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
//    }

    //폰 국가, 언어
    fun getDeviceLocaleInfo(): Pair<String, String> {
        val locale = Locale.getDefault()
        val languageCode = locale.language    // 예: "en", "ko"
        val countryCode = locale.country      // 예: "US", "KR"
        return Pair(languageCode, countryCode)
    }

    //Google
    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null
    }

    //    // (1) 토큰+언어+날짜 등록/전체 업데이트
    fun saveToken(token: String, languageCode: String) {
        val now = dateFormat.format(Date())
        val data = FCMTokenData(language = languageCode, updated_at = now)
        db.getReference("fcm_tokens").child(token).setValue(data)
    }
    /*
     Fcm 토큰 취득후 변경사항이나 최초 호출시 DB 처리(언어코드는 기본 locale -prefs에 있으면 그걸로)
     */
    fun saveFcmToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        val prevToken = prefs.getString(ComConstant.PREF_KEYS.KEY_FCM_TOKEN, null)

        // 이미 저장된 토큰과 같으면 아무것도 안함!
        if ( token == prevToken) return

        // 새 토큰이면 저장
        prefs.edit().putString(ComConstant.PREF_KEYS.KEY_FCM_TOKEN, token).apply()

        // 언어코드 읽어서 FCMTokenManager로 등록
        var language  = prefs.getString(ComConstant.PREF_KEYS.LANGUAGE, "")
        if (language == null || (language != null && language.trim { it <= ' ' } == "")) {
            language = ComUtil.geLocalLanguage()
        }
        saveToken(token, language)
    }

    // (2) 언어코드만 업데이트 (토큰은 동일, 날짜만 갱신)
    fun updateLanguage(token: String, newLanguageCode: String) {
        val now = dateFormat.format(Date())
        val updates = mapOf<String, Any>(
            "language" to newLanguageCode,
            "updated_at" to now
        )
        db.getReference("fcm_tokens").child(token).updateChildren(updates)
    }

    fun getFcmToken(context: Context): String? {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ComConstant.PREF_KEYS.KEY_FCM_TOKEN, null)
    }

    // (선택) 토큰 삭제
    fun deleteToken(token: String) {
        db.getReference("fcm_tokens").child(token).removeValue()
    }


    //prefs 토큰 정보 취득수 서버에 전송 --> TEMP 나중에
    fun registerFcmTokenIfNeeded(context: Context, userId: String) {
        val token = getFcmToken(context)
        if (!token.isNullOrEmpty()) {
            // 서버에 사용자ID + FCM 토큰 등록!
//            MyApi.registerFcmToken(userId, token)  --> AWS 구축시
        }
    }

//    // 내부저장소 루트 (/data/user/0/패키지명)
//    fun getAppRootDir(context: Context): File = context.filesDir.parentFile!!
//
//    // 내부저장소 파일 경로 (/data/user/0/패키지명/files)
//    fun getInternalFilesDir(context: Context): File = context.filesDir
//
//    // 로그 파일 폴더 (/files/log)
//    fun getLogDir(context: Context): File = File(context.filesDir, "log").apply { mkdirs() }
//
//    // 백업 파일 폴더 (/files/backup)
//    fun getBackupDir(context: Context): File = File(context.filesDir, "backup").apply { mkdirs() }
//
//    // 이미지 저장 폴더 (/files/image)
//    fun getImageDir(context: Context): File = File(context.filesDir, "image").apply { mkdirs() }
//
//    // 이미지 캐시 폴더 (/cache/image_cache)
//    fun getImageCacheDir(context: Context): File = File(context.cacheDir, "image_cache").apply { mkdirs() }


}
