package com.waveapp.tourcat.util

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset


/**
 *  다국어 지원을 위해 언어별 json 파일을 호출 - key ,value 로 동적으로 처리
 */
object StringResourceManager {
    private var stringMap: Map<String, String>? = null

    fun loadLanguage(context: Context, langCode: String) {
        // 언어 코드에 맞는 파일명 구성
        val filename = "lang/strings_${langCode}.json"
        val jsonString = context.assets.open(filename).use { it.readBytes().toString(Charset.forName("UTF-8")) }
        val jsonObject = JSONObject(jsonString)

        // jsonObject를 Map으로 변환
        stringMap = jsonObject.keys().asSequence().associateWith { jsonObject.getString(it) }
    }

    fun getString(key: String): String {
        return stringMap?.get(key) ?: "[$key]"
    }

    /**
     * // 1. 앱 시작 시 언어 코드에 따라 로딩 (예: 한국어)
     * StringResourceManager.loadLanguage(context = this, langCode = "ko")
     *
     * // 2. 문자열 사용 예시
     * val appName = StringResourceManager.getString("app_name")
     * textView.text = appName
     */
}