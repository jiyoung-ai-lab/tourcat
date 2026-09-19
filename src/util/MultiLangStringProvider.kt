package com.waveapp.tourcat.util

import android.content.Context
import org.json.JSONObject
import java.nio.charset.Charset

class MultiLangStringProvider(
    private val context: Context,
    private val assetFileName: String = "lang/strings_all.json"
) {
    // Map<key, Map<lang, value>>
    private val langMap: MutableMap<String, MutableMap<String, String>> = mutableMapOf()

    init {
        load()
    }

    private fun load() {
        try {
            val jsonString = context.assets.open(assetFileName)
                .use { it.readBytes().toString(Charset.forName("UTF-8")) }
            val jsonObj = JSONObject(jsonString)
            for (key in jsonObj.keys()) {
                val langObj = jsonObj.getJSONObject(key)
                val innerMap = mutableMapOf<String, String>()
                for (lang in langObj.keys()) {
                    innerMap[lang] = langObj.optString(lang, "")
                }
                langMap[key] = innerMap
            }
        } catch (e: Exception) {
            // 예외: 파일 없음, 파싱 오류 등
            e.printStackTrace()
        }
    }

    /**
     * 문자열을 가져온다. 우선순위: 언어값 > 영어(en) > [key]
     * @param key: JSON 키
     * @param langCode: 언어 코드("ko", "en", "ja", "zh" 등)
     */
    fun getString(key: String, langCode: String?): String {
        if (key.isBlank()) return "[empty_key]"
        val inner = langMap[key]
        if (inner == null) return "[$key]" // 아예 없는 키

        // 1. 우선 langCode
        val value = langCode?.let { inner[it] }?.takeIf { !it.isNullOrBlank() }
        if (!value.isNullOrBlank()) return value

        // 2. fallback: 영어
        val enValue = inner["en"]?.takeIf { !it.isNullOrBlank() }
        if (!enValue.isNullOrBlank()) return enValue

        // 3. fallback: 다른 언어 아무거나
        val anyValue = inner.values.firstOrNull { !it.isNullOrBlank() }
        if (!anyValue.isNullOrBlank()) return anyValue

        // 4. 모두 없으면
        return "[$key]"
    }

    /**
     * // 1. 생성 (권장: 앱 시작 시 1회 생성 후 앱 전역으로 관리)
     * val stringProvider = MultiLangStringProvider(context = this)
     *
     * // 2. 사용
     * val msgKo = stringProvider.getString("hello_msg", "ko") // 한국어 있으면, 없으면 영어, 다 없으면 [hello_msg]
     * val msgJa = stringProvider.getString("hello_msg", "ja") // 일본어 있으면, 없으면 영어, 다 없으면 [hello_msg]
     * val msgZh = stringProvider.getString("hello_msg", "zh")
     * val msgXX = stringProvider.getString("hello_msg", "xx") // 없는 언어코드: 영어 fallback
     *
     * val notExist = stringProvider.getString("not_exist_key", "en") // 결과: [not_exist_key]
     */
}