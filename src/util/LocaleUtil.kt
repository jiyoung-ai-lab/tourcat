package com.waveapp.tourcat.util

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.waveapp.tourcat.common.ComConstant
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object LocaleUtil {

    // 앱 전역에서 사용하는 Locale (앱 설정값, 기본은 시스템 Locale)
    private var _currentLocale: Locale = Locale.getDefault()
    val currentLocale: Locale
        get() = _currentLocale

    /**
     * 앱 내 Locale을 변경 (언어코드 + 국가코드, 예: "en", "KR")
     * @param languageCode 예: "en", "ko"
     * @param countryCode 예: "KR", "US"
     */
    fun setLocale(languageCode: String, countryCode: String) {
        _currentLocale = Locale(languageCode, countryCode)
        Locale.setDefault(_currentLocale)
    }

    /**
     * 국가코드만으로 Locale 설정 (언어는 시스템 기본)
     */
    fun setLocaleByCountry(countryCode: String) {
        _currentLocale = Locale("", countryCode)
        Locale.setDefault(_currentLocale)
    }
    fun getDeviceSystemLocaleInfo(): Pair<String, String> {
        val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 (Nougat) 이상은 다중 로케일 지원
            val locales = android.content.res.Resources.getSystem().configuration.locales
            if (locales.isEmpty) Locale.getDefault() else locales.get(0)
        } else {
            // 그 이하 버전
            android.content.res.Resources.getSystem().configuration.locale
        }
        val languageCode = locale.language    // 예: "ko", "en"
        val countryCode = locale.country      // 예: "KR", "US"
        return Pair(languageCode, countryCode)
    }
    // 날짜 현지화 (Medium)
    fun formatDate(date: Date): String {
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM, _currentLocale)
        return df.format(date)
    }

    // 커스텀 날짜 포맷
    fun formatDateCustom(date: Date, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, _currentLocale)
        return sdf.format(date)
    }

    // 화폐 현지화
    fun formatCurrency(amount: Number): String {
        val nf = NumberFormat.getCurrencyInstance(_currentLocale)
        return nf.format(amount)
    }

    // 숫자 현지화
    fun formatNumber(number: Number): String {
        val nf = NumberFormat.getNumberInstance(_currentLocale)
        return nf.format(number)
    }

    // 요일 현지화
    fun getDayOfWeek(date: Date): String {
        val sdf = SimpleDateFormat("EEEE", _currentLocale)
        return sdf.format(date)
    }


    fun dateToDbString(date: Date): String {
        val dbFormat = SimpleDateFormat("yyyyMMdd", Locale.US) // DB는 변동 없는 US 추천
        return dbFormat.format(date)
    }

    fun dbStringToDate(dbString: String): Date? {
        return try {
            val dbFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
            dbFormat.parse(dbString)
        } catch (e: Exception) {
            null
        }
    }
    /** 시작/종료일을 한 번에 표시 (기간용) */
    fun formatPeriod(start: String?, end: String?): String {
        if (start.isNullOrBlank() || end.isNullOrBlank()) return ""
        val startDate = dbStringToDate(start)
        val endDate = dbStringToDate(end)
        return if (startDate != null && endDate != null) {
            "${formatDate(startDate)} ~ ${formatDate(endDate)}"
        } else {
            ""
        }
    }

    // Date → 언어별 월명/숫자 (포맷: "MMM" 또는 "M" 등)
    fun getMonthTextByLocale(date: Date?, langCode: String, pattern: String = "MMM"): String {
        if (date == null) return ""
        val locale = when (langCode) {
            "ko" -> Locale.KOREAN
            "ja" -> Locale.JAPANESE
            "zh" -> Locale.CHINESE
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "es" -> Locale("es")
            "it" -> Locale.ITALIAN
            "th" -> Locale("th")
            else -> Locale.ENGLISH
        }
        val sdf = SimpleDateFormat(pattern, locale)
        return sdf.format(date)
    }

    // YYYYMMDD → 언어별 월명/숫자 (간단히 한 번에)
    fun getMonthTextFromDbStringByLocale(dbString: String?, langCode: String, pattern: String = "MMM"): String {
        if (dbString.isNullOrBlank()  ) return ""
        val date = dbStringToDate(dbString)
        return getMonthTextByLocale(date, langCode, pattern)
    }


    fun changeAppLanguage(context: Context, languageCode: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33) 이상
            val appLocale = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
            // Activity recreate()만 하면 자동 적용됨
        } else {
            // Android 12 이하
            val locale = Locale(languageCode)
            Locale.setDefault(locale)
            val resources = context.resources
            val config = Configuration(resources.configuration)
            config.setLocale(locale)
            resources.updateConfiguration(config, resources.displayMetrics)
            // Activity 재시작 필수!
        }
    }

    /**
     *  언어코드와 같은 컬럼을 return
     */
    fun getValidLangCode(langcode: String): String {
//        val supportedLangs = setOf("en", "ko", "ja", "zh", "fr", "de", "it", "th", "es")
        return if (ComConstant.SUPPORT_LANGUAGE.contains(langcode)) langcode else "en"
    }

//    val tempFormats = mapOf(
//        "en" to "%s%s('%s %s)",         // 🌡️28°F('24 7)
//        "ko" to "%s%s(%s년%s월)",         // 🌡️28°(24년7월)
//        "ja" to "%s%s(%s年%s月)",         // 🌡️28°(24年7月)
//        "zh" to "%s%s(%s年%s月)",         // 🌡️28°(24年7月)
//        "fr" to "%s%s(%s/%s)",           // 🌡️28°(24/7)
//        "de" to "%s%s(%s/%s)",           // 🌡️28°(24/7)
//        "it" to "%s%s(%s/%s)",           // 🌡️28°(24/7)
//        "th" to "%s%s(%sปี %sด)",         // 🌡️28°(24ปี 7ด)
//        "es" to "%s%s(%s/%s)"            // 🌡️28°(24/7)
//    )

    val tempFormats = mapOf(
        "en" to "%s%s",         // 🌡️28°F('24 7)
        "ko" to "%s%s",         // 🌡️28°(24년7월)
        "ja" to "%s%s",         // 🌡️28°(24年7月)
        "zh" to "%s%s",         // 🌡️28°(24年7月)
        "fr" to "%s%s",           // 🌡️28°(24/7)
        "de" to "%s%s",           // 🌡️28°(24/7)
        "it" to "%s%s",           // 🌡️28°(24/7)
        "th" to "%s%s",         // 🌡️28°(24ปี 7ด)
        "es" to "%s%s"            // 🌡️28°(24/7)
    )

    // 평균기온 다국어 텍스트 생성 함수 (국가코드에 따라 화씨 변환)
    // langcode가 tempFormats에 없으면 "en"을 사용하고, 무조건 도씨(°)만 표기
    fun getAvgTempText(temp: Double?, year: Int?, month: Int?, langcode: String): String {
        // 온도 데이터 없으면 빈 문자열
        if (temp == null) return ""

        val hasLangFormat = tempFormats.containsKey(langcode)
        val useEnFormat = !hasLangFormat
        val isFahrenheit = ComConstant.USER_NATION_CODE.equals("US", ignoreCase = true) && hasLangFormat

        val tempValue = if (isFahrenheit) ((temp * 9 / 5) + 32).toInt() else temp.toInt()
        val unit = if (isFahrenheit) "°F" else "°"

        // 연도/월이 null 또는 0이면 빈값(아예 표시 안함)
        val yearShort = if (year != null && year != 0) year.toString().takeLast(2) else ""
        val monthValue = if (month != null && month in 1..12) month else ""

        // 영어는 월을 약어(Jan, ...)로, 나머지는 숫자
        val monthText = if (langcode == "en" && monthValue is Int) {
            val abbr = arrayOf(
                "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            abbr[monthValue]
        } else {
            if (monthValue != "") monthValue.toString() else ""
        }

        val format = tempFormats[langcode] ?: tempFormats["en"]!!
        val safeFormat = if (useEnFormat) tempFormats["en"]!! else format
        val safeUnit = if (useEnFormat) "°" else unit

        // 연도, 월 둘 중 하나라도 빈 값이면 해당 자리 비움
        return if (yearShort == "" && monthText == "") {
            "" // 연도와 월 모두 없으면 출력하지 않음
        } else {
            String.format(safeFormat, tempValue, safeUnit, yearShort, monthText)
        }
    }

    // ---- 다국어 포맷 정의 ----
    private data class RelativeFormat(
        val minuteFormat: String, // ex) "%d분 전"
        val hourFormat: String,   // ex) "%d시간 전"
        val datePattern: String   // ex) "yyyy-MM-dd"
    )

    // 앱에서 사용할 언어코드별 포맷 맵 (필요 시 추가)
    private val relativeTimeFormats: Map<String, RelativeFormat> = mapOf(
        "ko" to RelativeFormat("%d분 전", "%d시간 전", "yyyy-MM-dd"),
        "en" to RelativeFormat("%d min ago", "%d hr ago", "MMM d, yyyy"),
        "ja" to RelativeFormat("%d分前", "%d時間前", "yyyy/MM/dd"),
        // 변경: 分鐘/小時 → 分钟/小时 (간체)
        "zh" to RelativeFormat("%d 分钟前", "%d 小时前", "yyyy-MM-dd"),
        "fr" to RelativeFormat("il y a %d min", "il y a %d h", "d MMM yyyy"),
        "de" to RelativeFormat("vor %d Min.", "vor %d Std.", "d MMM yyyy"),
        "it" to RelativeFormat("%d min fa", "%d h fa", "d MMM yyyy"),
        "th" to RelativeFormat("%d นาทีที่แล้ว", "%d ชั่วโมงที่แล้ว","yyyy-MM-dd"),
        "es" to RelativeFormat("hace %d min", "hace %d h", "d MMM yyyy")
    )
    /**
     * 수신 시각(ms)을 현재와 비교해 상대시간 텍스트로 변환
     * - 1시간(<=60분) 이하는 "분 전"
     * - 1시간 초과 ~ 24시간 이하는 "시간 전"
     * - 24시간 초과는 언어별 datePattern으로 절대일자
     *
     * @param receivedAt System.currentTimeMillis 기준 ms
     * @param langcode   "ko", "en", ... (relativeTimeFormats에 없으면 "en" 사용)
     *
     * 빈/이상값 처리:
     * - receivedAt == null 또는 0 이면 "" 반환
     * - 미래 시간(음수 차이)은 0으로 보정해 최소 1분 전으로 표기
     */
    fun getRelativeTimeText(receivedAt: Long?, langcode: String): String {
        if (receivedAt == null || receivedAt == 0L) return ""

        // val hasLang = relativeTimeFormats.containsKey(langcode) // <- 제거
        val fmt = relativeTimeFormats[langcode] ?: relativeTimeFormats["en"]!!

        val now = System.currentTimeMillis()
        val rawDiff = now - receivedAt
        val diff = if (rawDiff < 0) 0 else rawDiff

        val oneHourMillis = TimeUnit.HOURS.toMillis(1)
        val oneDayMillis = TimeUnit.DAYS.toMillis(1)

        return when {
            diff <= oneHourMillis -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diff).coerceAtLeast(1)
                String.format(fmt.minuteFormat, minutes)
            }
            diff <= oneDayMillis -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diff).coerceAtLeast(1)
                String.format(fmt.hourFormat, hours)
            }
            else -> {
                val sdf = SimpleDateFormat(fmt.datePattern, Locale(langcode ))
                sdf.format(receivedAt)
            }
        }
    }

//    // 언어코드 → Locale 매핑 (없으면 fallback)
//    private fun langToLocale(code: String, fallback: Locale = Locale.ENGLISH): Locale {
//        return when (code.lowercase(Locale.ROOT)) {
//            "ko" -> Locale.KOREAN
//            "en" -> Locale.ENGLISH
//            "ja" -> Locale.JAPANESE
//            "zh" -> Locale.CHINESE
//            "es" -> Locale("es")
//            else -> fallback
//        }
//    }

}



/*
// 1. 국가코드(및 필요시 언어코드)로 Locale 변경
LocaleHelper.setLocaleByCountry("JP", "ja") // 일본
// LocaleHelper.setLocaleByCountry("FR", "fr") // 프랑스
// LocaleHelper.setLocaleByCountry("US", "en") // 미국

// 2. 날짜, 화폐, 숫자, 요일 현지화
val today = Date()
val sampleAmount = 123456.78

val dateStr = LocaleHelper.formatDate(today)
val customDateStr = LocaleHelper.formatDateCustom(today, "yyyy/MM/dd")
val currencyStr = LocaleHelper.formatCurrency(sampleAmount)
val numberStr = LocaleHelper.formatNumber(sampleAmount)
val dayOfWeekStr = LocaleHelper.getDayOfWeek(today)

// 3. 화면에 표시
tvDate.text = dateStr          // 예: 2025/07/06 (일본)
tvMoney.text = currencyStr     // 예: ￥123,457
tvNumber.text = numberStr      // 예: 123,456.78
tvDayOfWeek.text = dayOfWeekStr// 예: 日曜日
 */