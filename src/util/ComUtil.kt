package com.waveapp.tourcat.util

import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Build
import android.widget.Spinner
import android.widget.Toast
import com.google.mlkit.vision.text.Text
import com.waveapp.tourcat.common.ComConstant
import java.text.NumberFormat
import java.util.Locale

object ComUtil {

    fun setBlank(value: String?): String = value ?: ""

    // 숫자인지 체크(입력문자값 중 숫자가 아닌 경우 false
    fun isNumber(str: String?): Boolean {
        if (str.isNullOrEmpty()) return false
        return str.all { it in '0'..'9' }
    }

//    // dummy test 제거 (OCR에서 사용) --> 대체
//     fun cleanAndReplace(input: String, delimiter: String = ","): String {
//        val escapedDelim = Regex.escape(delimiter)
//        return input
//            .replace("[\\t\\r\\n]| {2,}".toRegex(), delimiter)   // 탭, 엔터, 2개 이상 공백 → delimiter
//            .replace("$escapedDelim{2,}".toRegex(), delimiter)   // 연속 delimiter → 1개 delimiter
//            .trim { it.toString() == delimiter }                 // 양 끝 delimiter 제거
//    }

//    fun cleanTextList(block: Text.TextBlock): List<Pair<String, String>> {
//        val meaninglessRegex = Regex("^[\\p{Punct}\\s]+$") // 특수기호/공백만
//
//        val lang = block.recognizedLanguage.ifBlank { "und" }
//        val raw = block.text
//
//        val cleaned = raw
//            .replace("[\\t\\r\\n]".toRegex(), " ")     // 탭/개행 → 공백
//            .replace(" {2,}".toRegex(), " ")           // 다중 공백 → 1칸
//            .trim()
//
//        return if (
//            cleaned.isNotBlank() &&
//            cleaned.length >= 2 &&
//            !meaninglessRegex.matches(cleaned)
//        ) {
//            listOf(lang to cleaned)
//        } else {
//            emptyList()
//        }
//    }
//
//    //특정 문자열을 지정한 문자로 치환
    fun replaceAllRegex(find: String, to: String, s: String?): String {
        return s?.replace(find.toRegex(), to) ?: ""
    }


    //특정문자열 존재여부 체크(find기능)
    fun isInStr(find: String, s: String?): Boolean {
        return !s.isNullOrBlank() && s.contains(find)
    }

    //특정문자열 위치값
    fun isInStrPos(find: String, s: String?): Int =
        s?.indexOf(find) ?: -1


    //특정길이만큼 앞에 0 채우기 (ex.0001)
    fun fillSpaceToZero(str: String?, len: Int): String {
        return str?.trim()?.padStart(len, '0') ?: ""
    }

    fun fillSpaceToZero(num: Int, len: Int): String {
        return fillSpaceToZero(num.toString(), len)
    }

    //특정길이만큼 오른쪽 blank 채우기(ex: "안녕    ")
    fun fillSpaceToBlank(str: String?, len: Int): String {
        return str?.trim()?.padEnd(len, ' ') ?: ""
    }

    //특정길이만큼 왼쪽 blank 채우기(ex: "   안녕")
    fun fillSpaceToBlankF(num: Int, len: Int): String {
        return num.toString().padStart(len, ' ')
    }

    //특정문자를 구분자로 인식, 글자를 잘라서 배열로 return
    fun splitStr(pInsStr: String, delim: String): ArrayList<String> {
        return pInsStr.split(delim).toCollection(ArrayList())
    }

    //일정 글자수 이상인 경우 뒤에 생략점 붙이기("...")
    fun cutStr(s: String?, i: Int): String {
        return when {
            s.isNullOrBlank() -> ""
            s.length > i      -> s.take(i) + "..."
            else              -> s
        }
    }

    fun setRadioCheckYN(inVal: Boolean?): String {
        return if (inVal == true) "Y" else ""
    }

    fun setCheckYN(inVal: Boolean?): String {
        return if (inVal == true) "Y" else ""
    }

    fun getCheckYN(inVal: String?): Boolean {
        return inVal == "Y"
    }

    fun getRadioCheckYN(inVal: String?, id: Int): Int {
        return if (inVal == "Y") id else 0
    }

    fun stringToInt(inVal: String?): Int {
        return inVal?.takeIf { it.isNotBlank() && it != "null" }?.toIntOrNull() ?: 0
    }

    fun intToString(inVal: Int): String {
        return inVal.toString()
    }

    fun stringToLong(inVal: String?): Long {
        return inVal?.takeIf { it.isNotBlank() }?.toLongOrNull() ?: 0L
    }

    fun longToString(inVal: Long): String {
        return inVal.toString()
    }

    fun replaceString(pSrcString: String?, pOldPattern: String, pNewPattern: String): String {
        return pSrcString?.replace(pOldPattern, pNewPattern) ?: ""
    }

    fun addComma(value: Long): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }
    fun addComma(value: Double): String {
        return NumberFormat.getNumberInstance(Locale.US).format(value)
    }

    // [언어코드] 내용 형식  ->  Pair<언어코드, 내용> 형태로 리스트
    fun splitTextByLangTag(text: String): List<Pair<String, String>> {
        val regex = Regex("""\[(\w+)]\s*([^\[]+)""") // \[ 를 [] 내부로 빼야 함
        return regex.findAll(text).map {
            val (lang, content) = it.destructured
            lang to content.trim()
        }.toList()
    }


    /*
    fun K2E(str: String?): String {
        if (str == null) return ""
        return String(str.toByteArray(Charsets.EUC_KR), Charsets.ISO_8859_1)
    }

    fun E2K(str: String?): String {
        if (str == null) return ""
        return String(str.toByteArray(Charsets.ISO_8859_1), Charsets.EUC_KR)
    }

    fun encodeURL(url: String?): String {
        return url?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
    }
     */

//    //전화번호 setting --> 이건 국제전화번호로 변경해야해서 향후 수정필요
//    fun getSplitTel(tel: String?): Array<String> {
//        val telno = arrayOf("", "", "")
//        if (!tel.isNullOrEmpty()) {
//            when {
//                tel.length > 10 -> {  // 11자리(휴대폰, 서울 외 시외번호)
//                    telno[0] = tel.substring(0, 3)
//                    telno[1] = tel.substring(3, 7)
//                    telno[2] = tel.substring(7)
//                }
//                tel.startsWith("02") -> { // 서울번호
//                    if (tel.length < 10) {
//                        telno[0] = tel.substring(0, 2)
//                        telno[1] = tel.substring(2, 5)
//                        telno[2] = tel.substring(5)
//                    } else {
//                        telno[0] = tel.substring(0, 2)
//                        telno[1] = tel.substring(2, 6)
//                        telno[2] = tel.substring(6)
//                    }
//                }
//                tel.length == 10 -> { // 서울 외 시외 10자리
//                    telno[0] = tel.substring(0, 3)
//                    telno[1] = tel.substring(3, 6)
//                    telno[2] = tel.substring(6)
//                }
//                else -> {
//                    // 혹시라도 길이가 너무 짧으면
//                    telno[0] = tel
//                }
//            }
//        }
//        return telno
//    }

//    //배열에서 특정 값 index return
//    fun getSpinner(ctx: Context, id: Int, parm1: String?): Int {
//        val arr = ctx.resources.getStringArray(id)
//        return arr.indexOfFirst { it == parm1 }.takeIf { it >= 0 } ?: 0
//    }

//    //특정위치 배열데이터 가져오기
//    fun setSpinner(ctx: Context, id: Int, sp: Spinner): String {
//        val arr = ctx.resources.getStringArray(id)
//        val position = sp.selectedItemPosition
//        return if (position in arr.indices) arr[position] else ""
//    }

//    //key 배열값으로 value 배열 찾기( key, value가 array 형태로 보관할때 사용)
//    fun getSpinnerText(ctx: Context, keyarr: Int, arr: Int, value: String?): String {
//        val keys = ctx.resources.getStringArray(keyarr)
//        val texts = ctx.resources.getStringArray(arr)
//        val idx = keys.indexOfFirst { it == value }
//        return if (idx in texts.indices) texts[idx] else ""
//    }

//    fun getSpinnerFromDb(keyList: List<Long>, id: Long): Int {
//        return keyList.indexOfFirst { it == id }.takeIf { it >= 0 } ?: 0
//    }
//
//    fun setSpinnerFromDb(keyList: List<Long>, sp: Spinner): Long {
//        val position = sp.selectedItemPosition
//        return if (position in keyList.indices) keyList[position] else 0L
//    }

    fun setYesReturnValue(str: String?, value: Int): Int {
        return if (str?.equals("Y", ignoreCase = true) == true) value else 0
    }

    fun setYesReturnValue(str: String?, value: String): String {
        return if (str?.trim() == "Y") value else " "
    }

    fun getArrayFromString(str: String): Array<String> {
        return str.map { it.toString() }.toTypedArray()
    }


    fun geLocalLanguage(): String {

        val localeInfo = EnvironmentUtil.getDeviceLocaleInfo()
        var language: String = localeInfo.first           // 예: "ko"

        if (language.isNullOrBlank() ) {
            language = ComConstant.LOCALE_LANGUAGE
        }
        return language
    }
    fun geLocalNation(): String {

        val localeInfo = EnvironmentUtil.getDeviceLocaleInfo()
        var nation: String = localeInfo.second        // 예: "KR"

        if (nation.isNullOrBlank() ) {
            nation = ComConstant.LOCALE_NATION
        }
        return nation
    }

//    fun changeAppLanguage(context: Context, langCode: String) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            // Android 13+
//            val appLocale = LocaleListCompat.forLanguageTags(langCode)
//            AppCompatDelegate.setApplicationLocales(appLocale)
//        } else {
//            // Android 12 이하
//            val config = context.resources.configuration
//            val locale = Locale(langCode)
//            Locale.setDefault(locale)
//            config.setLocale(locale)
//            context.resources.updateConfiguration(config, context.resources.displayMetrics)
//        }
//        // 현재 화면을 다시 그려야 변경 내용 반영
//        if (context is Activity) {
//            context.recreate()
//        }
//    }

//    fun isLanguageFront(ctx: Context): Boolean {
//        return (ComConstant.LOCALE != null && (ComConstant.LOCALE.trim() == ComConstant.LOCALE_KO
//                || ComConstant.LOCALE.trim() == ComConstant.LOCALE_JA
//                || ComConstant.LOCALE.trim() == ComConstant.LOCALE_ZH))
//    }

    fun getStrResource(ctx: Context, id: Int): String {
        return try {
            ctx.resources.getString(id)
        } catch (e: Resources.NotFoundException) {
            ""
        }
    }

//    fun makeBackupFileName(option: String?): String {
//        val filename = StringBuilder()
//        filename.append("smdata")
//        if (!option.isNullOrEmpty()) {
//            filename.append("_")
//            filename.append(option)
//        }
//        return filename.toString()
//    }

//    fun setActionTitle(ctx: Context, id: Int, cnt: Int): String {
//        val str = StringBuilder()
//        str.append(getStrResource(ctx, id))
//        if (cnt > 0) {
//            str.append("(")
//            str.append(intToString(cnt))
//            str.append(")")
//        }
//        return str.toString()
//    }

//    fun setActionTitle(ctx: Context, id: Int, addstr: String?): String {
//        val str = StringBuilder()
//        if (isLanguageFront(ctx)) {
//            str.append(getStrResource(ctx, id))
//            if (!addstr.isNullOrBlank()) {
//                str.append(" ")
//                str.append(addstr)
//            }
//        } else {
//            if (!addstr.isNullOrBlank()) {
//                str.append(addstr)
//                str.append(" ")
//            }
//            str.append(getStrResource(ctx, id))
//        }
//        return str.toString()
//    }

//    fun makeScheduleMsg(ctx: Context, info: ScheduleInfo, date: String?): String {
//        val buffer = StringBuilder()
//        buffer.append(info.scheduleName)
//        if (!info.username.isNullOrBlank()) {
//            buffer.append("(")
//            buffer.append(info.username)
//            buffer.append(")")
//        }
//        buffer.append("\n")
//        if (!date.isNullOrBlank()) {
//            buffer.append(SmDateUtil.getDateSimpleFormat(ctx, date, ComConstant.SEPERATE_DOT, true))
//            buffer.append("\n")
//        } else {
//            if (!info.startDate.isNullOrBlank()) {
//                buffer.append(
//                    SmDateUtil.getDateSimpleFormat(
//                        ctx, info.startDate, ComConstant.SEPERATE_DOT, false
//                    ) + "~" + SmDateUtil.getDateSimpleFormat(ctx, info.endDate, ComConstant.SEPERATE_DOT, false)
//                )
//                buffer.append("\n")
//                buffer.append(setYesReturnValue(info.cycle, getStrResource(ctx, R.string.everyweek)))
//                if (!info.dayOfWeekFullText.isNullOrBlank()) {
//                    buffer.append(" ")
//                    buffer.append(info.dayOfWeekFullText)
//                    buffer.append("\n")
//                }
//            }
//        }
//        if (!info.allDayYn.isNullOrBlank()) {
//            buffer.append(setYesReturnValue(info.allDayYn, getStrResource(ctx, R.string.allday)))
//        } else {
//            if (!info.startTime.isNullOrBlank()) {
//                buffer.append(
//                    SmDateUtil.getTimeFullFormat(ctx, info.startTime) +
//                            "~" + SmDateUtil.getTimeFullFormat(ctx, info.endTime)
//                )
//            }
//        }
//        return buffer.toString()
//    }

//    fun makeScheduleMsg(ctx: Context, info: SpecialDayInfo): String {
//        val buffer = StringBuilder()
//        buffer.append(info.name)
//        buffer.append("\n")
//        if (!info.solardate.isNullOrBlank()) {
//            buffer.append(SmDateUtil.getDateSimpleFormat(ctx, info.solardate, ComConstant.SEPERATE_DOT, true))
//            buffer.append("\n")
//        }
//        return buffer.toString()
//    }

//    fun getLeapText(ctx: Context, leap: String?): String {
//        return when (leap) {
//            "1" -> "(-)"
//            "2" -> getStrResource(ctx, R.string.yun) + "(-)"
//            else -> "(+)"
//        }
//    }

    fun getAppVersion(ctx: Context): String {
        return try {
            val pm = ctx.packageManager
            val packageName = ctx.packageName
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0)).versionName ?: ""
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0).versionName ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     *  Android 12버전(API 31) 이하 버전의 경우 별도 처리  -> 하위모델에 대한 지원 힘들다
     */

    fun getAppVersionCode(ctx: Context): Long {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13 이상
                ctx.packageManager.getPackageInfo(
                    ctx.packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            }

            // longVersionCode는 API 28 이상에서 사용 가능
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }

    fun getAppPackage(ctx: Context): String {
        return ctx.packageName
    }

//    fun isEmail(str: String?): Boolean {
//        return !str.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(str).matches()
//    }

//    fun isFormallVer(ctx: Context): Boolean {
//        if (VersionConstant.APPID == VersionConstant.APP_LITE) {
//            showToast(ctx, getStrResource(ctx, R.string.msg_lite_limite))
//            return false
//        }
//        return true
//    }

//    fun hasStorage(requireWriteAccess: Boolean): Boolean {
//        val state = Environment.getExternalStorageState()
//        if (Environment.MEDIA_MOUNTED == state) {
//            return true
//        } else if (!requireWriteAccess && Environment.MEDIA_MOUNTED_READ_ONLY == state) {
//            return true
//        }
//        return false
//    }

//    fun changeEnterKeyToOther(str: String?): String? {
//        return if (!str.isNullOrBlank()) {
//            replaceString(str, "\n", "%n")
//        } else {
//            str
//        }
//    }

//    fun changeOtherToEnterKey(str: String?): String? {
//        return if (!str.isNullOrBlank()) {
//            replaceString(str, "%n", "\n")
//        } else {
//            str
//        }
//    }


}
