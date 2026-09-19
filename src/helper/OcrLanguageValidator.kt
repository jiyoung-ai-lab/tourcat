package com.waveapp.tourcat.helper

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.util.ComUtil

class OcrLanguageValidator {
    /**
     * 메인 진입: 언어코드, OCR 추출 텍스트 받아서 언어별 체크
     * @param langCode: "ko", "ja"
     * @param text: OCR 추출 텍스트
     * @return Boolean (true: 정상, false: 해당 언어 아님)
     */
    fun validate(langCode: String, text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false

        return when (langCode.lowercase()) {
            "ko" -> isKoreanOrLatinValidText(trimmed)
            "ja" -> isJapaneseOrLatinValidText(trimmed)
            "zh" -> isChineseOrLatinValidText(trimmed)
            else -> isLatinOnlyValidText(trimmed)
        }
    }
    /** 한글(30% 이상) 또는 알파벳(라틴)만 90% 이상 허용 */
    private fun isKoreanOrLatinValidText(text: String): Boolean {
        val cleaned = text.filter { it.isLetter() || it.isWhitespace() }
        val total = cleaned.length.toDouble()
        if (total < 2) return false // 너무 짧으면 무의미

        val korCount = cleaned.count { it in '\uAC00'..'\uD7AF' || it in '\u3130'..'\u318F' }
        val latinCount = cleaned.count { it in '\u0041'..'\u005A' || it in '\u0061'..'\u007A' || it in '\u00C0'..'\u00FF' }
        val jpCount = cleaned.count { it in '\u3040'..'\u30FF' }
        val cjkCount = cleaned.count { it in '\u4E00'..'\u9FFF' }

        val korRate = korCount / total
        val latinRate = latinCount / total

        // (1) 한글이 30% 이상
        if (korRate > 0.3) return true
        // (2) 라틴계 90% 이상이고 한자·일본어 거의 없음
        if (korCount == 0 && jpCount == 0 && cjkCount == 0 && latinRate > 0.9) return true

        // --- [여기서부터 추가!] ---
        // 의미있는 라틴 단어(4글자 이상) 1개라도 있으면 true
        val tokens = text.split(" ").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.any { it.length >= 4 && it.all { ch -> ch.isLetter() || ch == '-' } &&
                    it.any { ch -> ch in '\u0041'..'\u005A' || ch in '\u0061'..'\u007A' || ch in '\u00C0'..'\u00FF' }
            }) return true

        return false
    }

    /** 일본어(히라가나+가타카나 20% 이상) 또는 알파벳(라틴)만 90% 이상 허용 */
    private fun isJapaneseOrLatinValidText(text: String): Boolean {
        val cleaned = text.filter { it.isLetter() || it.isWhitespace() }
        val total = cleaned.length.toDouble()
        if (total < 2) return false

        val hiraCount = cleaned.count { it in '\u3040'..'\u309F' }
        val kataCount = cleaned.count { it in '\u30A0'..'\u30FF' }
        val jpRate = (hiraCount + kataCount) / total
        val cjkCount = cleaned.count { it in '\u4E00'..'\u9FFF' }
        val latinCount = cleaned.count { it in '\u0041'..'\u005A' || it in '\u0061'..'\u007A' || it in '\u00C0'..'\u00FF' }

        // (1) 히라가나+가타카나 20% 이상
        if (jpRate > 0.2) return true
        // (2) 라틴만 90% 이상, 한자·히라가나·가타카나 거의 없음
        if ((hiraCount + kataCount + cjkCount == 0) && (latinCount / total > 0.9)) return true

        // --- [여기서부터 추가!] ---
        val tokens = text.split(" ").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.any { it.length >= 4 && it.all { ch -> ch.isLetter() || ch == '-' } &&
                    it.any { ch -> ch in '\u0041'..'\u005A' || ch in '\u0061'..'\u007A' || ch in '\u00C0'..'\u00FF' }
            }) return true

        return false
    }

    /** 중국어(한자 30% 이상) 또는 알파벳(라틴)만 90% 이상 허용 */
    private fun isChineseOrLatinValidText(text: String): Boolean {
        val cleaned = text.filter { it.isLetter() || it.isWhitespace() }
        val total = cleaned.length.toDouble()
        if (total < 2) return false

        val cjkCount = cleaned.count { it in '\u4E00'..'\u9FFF' }
        val hiraCount = cleaned.count { it in '\u3040'..'\u309F' }
        val kataCount = cleaned.count { it in '\u30A0'..'\u30FF' }
        val latinCount = cleaned.count { it in '\u0041'..'\u005A' || it in '\u0061'..'\u007A' || it in '\u00C0'..'\u00FF' }

        // (1) 한자(중국어) 30% 이상
        if (cjkCount / total > 0.3) return true
        // (2) 라틴만 90% 이상, 한자·히라가나·가타카나 거의 없음
        if ((cjkCount + hiraCount + kataCount == 0) && (latinCount / total > 0.9)) return true

        // --- [여기서부터 추가!] ---
        val tokens = text.split(" ").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.any { it.length >= 4 && it.all { ch -> ch.isLetter() || ch == '-' } &&
                    it.any { ch -> ch in '\u0041'..'\u005A' || ch in '\u0061'..'\u007A' || ch in '\u00C0'..'\u00FF' }
            }) return true

        return false
    }

    /** 라틴 문자(영어/유럽어)만 80% 이상, 한글/한자/일본어 합이 5% 미만 */
    fun isLatinOnlyValidText(text: String): Boolean {
        if (text.isBlank()) return false

        // (1) 오직 문자만 비율 계산
        val validCharList = text.filter { it.isLetter() || it.isDigit() }
        val total = validCharList.length.toDouble()
        if (total == 0.0) return false // 문자 없음

        val latinCount = validCharList.count {
            it in '\u0041'..'\u005A'    // A-Z
                    || it in '\u0061'..'\u007A' // a-z
                    || it in '\u00C0'..'\u00FF' // Latin-1 확장
        }
        val korCount = validCharList.count { it in '\uAC00'..'\uD7AF' || it in '\u3130'..'\u318F' }
        val cjkCount = validCharList.count { it in '\u4E00'..'\u9FFF' }
        val hiraCount = validCharList.count { it in '\u3040'..'\u309F' }
        val kataCount = validCharList.count { it in '\u30A0'..'\u30FF' }

        val latinRate = latinCount / total
        val korRate = korCount / total
        val cjkRate = cjkCount / total
        val jpRate = (hiraCount + kataCount) / total

        // (기존) 라틴문자가 전체의 80% 이상이고, 동양문자 5% 미만만 허용
        if (!((latinRate > 0.8) && (korRate + cjkRate + jpRate < 0.05))) return false

        // (2) 의미없는 "숫자+1~2자 단어"만 있는 경우 false
        val tokens = text.split(" ")
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val avgWordLen = if (tokens.isNotEmpty()) tokens.map { it.length }.average() else 0.0
        val numShortTokens = tokens.count { it.length < 3 }
        // 전체 단어 중 3자 미만이 80% 이상이고, 평균 길이도 2.5 이하(예: 코드/로트/약어 혼용)
        if (tokens.isNotEmpty() && numShortTokens.toDouble() / tokens.size > 0.8 && avgWordLen <= 2.5) return false

        // (3) "모두 대문자+숫자" 또는 "숫자/알파벳 반복" 형태만 있을 때 (예: "1303 L9 2 La 252% c4")
        val pureCodePattern = Regex("^[\\dA-Z% ]+\$")
        if (pureCodePattern.matches(text.replace(".", "").replace(",", ""))) return false

        // (4) (선택) 단어가 4개 이하이면서 모두 1~2자라면 false (거의 의미 없음)
        if (tokens.size <= 4 && tokens.all { it.length < 3 }) return false

        // --- [여기서부터 추가!] ---
        // 의미있는 라틴 단어(4글자 이상) 1개라도 있으면 true
        if (tokens.any { it.length >= 4 && it.all { ch -> ch.isLetter() || ch == '-' } &&
                    it.any { ch -> ch in '\u0041'..'\u005A' || ch in '\u0061'..'\u007A' || ch in '\u00C0'..'\u00FF' }
            }) return true

        return true
    }

    fun showLangValidationDialog(
        context: Context,
        ocrText: String,
        langLabel: String = ComConstant.LOCALE_EN,
        message: String = context.getString(R.string.msg_quest_unmatch_language),
        onContinue: () -> Unit = {},
        onCancel: () -> Unit = {}
    ) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_lang_validation, null)
        val chkDontAskAgain = dialogView.findViewById<CheckBox>(R.id.chkDontAskAgain)
        val btnContinue = dialogView.findViewById<Button>(R.id.btnContinue)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val tvDialogMessage = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val tvOcrText = dialogView.findViewById<TextView>(R.id.tvOcrText)

        tvDialogMessage.text = "(" +  langLabel + ")" +  message
        tvOcrText.text = ComUtil.cutStr(ocrText , 30)
//        btnContinue.text = context.getString(R.string.continue)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnContinue.setOnClickListener {
            if (chkDontAskAgain.isChecked) {
                ComConstant.TRANSLATION_MATCH = true
            }
            onContinue()
            alertDialog.dismiss()
        }
        btnCancel.setOnClickListener {
            onCancel()
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

}
