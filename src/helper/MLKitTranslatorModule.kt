package com.waveapp.tourcat.helper

import android.content.Context
import android.icu.text.Transliterator
import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.vision.text.Text
import com.waveapp.tourcat.R
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.common.TranscriptionEnToKo
import com.waveapp.tourcat.util.NetworkUtil
import com.waveapp.tourcat.util.makeSafeChunks
import com.waveapp.tourcat.util.sanitizeForNmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


object MLKitTranslatorModule {

    // 0. 지원언어 코드 체크
    fun isLanguageSupported(langCode: String): Boolean {
        val supported = listOf(
            "en", "ko", "ja", "fr", "de", "it", "zh", "ar", "id", "th", "hi", "ru", "es", "vi", "pt", "tr"
        )
        return langCode in supported
    }

    // 1. 입력 문장 언어 감지 (suspend)
    suspend fun detectLanguage(text: String): String = suspendCancellableCoroutine { cont ->
        val identifier: LanguageIdentifier = LanguageIdentification.getClient()
        identifier.identifyLanguage(text)
            .addOnSuccessListener { lang -> cont.resume(lang) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    // 3. 단일 문장 번역 (suspend)
    suspend fun translateText(
        context: Context,
        sourceLang: String,
        targetLang: String,
        text: String
    ): String = suspendCancellableCoroutine { cont ->
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.fromLanguageTag(sourceLang) ?: TranslateLanguage.ENGLISH)
            .setTargetLanguage(TranslateLanguage.fromLanguageTag(targetLang) ?: TranslateLanguage.KOREAN)
            .build()
        val translator = Translation.getClient(options)
        cont.invokeOnCancellation { translator.close() }

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.translate(text)
                    .addOnSuccessListener { result ->
                        translator.close(); cont.resume(result)
                    }
                    .addOnFailureListener { e ->
                        translator.close(); cont.resumeWithException(e)
                    }
            }
            .addOnFailureListener { e ->
                translator.close(); cont.resumeWithException(e)
            }
    }

    // 5. 리스트 번역 (suspend) : 길이 제한, 구분자, 복원  --> 이건 참조용으로 보존해야함...
    suspend fun translateList(
        context: Context,
        inputList: List<String>,
        sourceLang: String,
        targetLang: String,
        chunkLimit: Int = 300,
        separator: String = "|||"
    ): List<String> = withContext(Dispatchers.IO) {
        if (inputList.isEmpty()) return@withContext inputList
        val groupedChunks = mutableListOf<String>()
        val originalCounts = mutableListOf<Int>()
        val currentChunk = mutableListOf<String>()
        var currentLen = 0

        inputList.forEach { item ->
            val itemLen = if (currentChunk.isEmpty()) item.length else item.length + separator.length
            if (currentLen + itemLen <= chunkLimit) {
                currentChunk.add(item)
                currentLen += itemLen
            } else {
                groupedChunks.add(currentChunk.joinToString(separator))
                originalCounts.add(currentChunk.size)
                currentChunk.clear(); currentChunk.add(item)
                currentLen = item.length
            }
        }
        if (currentChunk.isNotEmpty()) {
            groupedChunks.add(currentChunk.joinToString(separator))
            originalCounts.add(currentChunk.size)
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)
        val translatedGroups = mutableListOf<String>()

        try {
                ensureModelsDownloaded(
                    context,
                    listOf(sourceLang, targetLang))

//            ensureModelsDownloaded(context, listOf(sourceLang, targetLang))
            translator.downloadModelIfNeeded().await()
            for (chunk in groupedChunks) {
                try {
                    val translated = translator.translate(chunk).await()
                    val normalized = convertFullWidthToHalfWidth(normalizeToHalfWidthICU(translated))
                    translatedGroups.add(normalized)
                } catch (e: Exception) {
                    translatedGroups.add(chunk)
                }
            }
        } catch (e: Exception) {
            translatedGroups.clear(); translatedGroups.addAll(groupedChunks)
        } finally {
            translator.close()
        }

        // 복원
        val result = mutableListOf<String>()
        var index = 0
        for ((i, group) in translatedGroups.withIndex()) {
            val items = group.split(separator).map { it.trim() }
            if (items.size == originalCounts[i]) result.addAll(items)
            else result.addAll(inputList.subList(index, index + originalCounts[i]))
            index += originalCounts[i]
        }
        result
    }

//    fun translateMultiple(
//        context: Context,
//        textList: List<String>,
//        targetLang: String,
//        defaultSourceLang: String = "en",
//        onResult: (List<String>) -> Unit
//    ) {
//        (context as? LifecycleOwner)?.lifecycleScope?.launch {
//            val result = translateMultiple(context, textList, targetLang, defaultSourceLang)
//            onResult(result)
//        }
//    }


    // 7. 언어 감지 + 클린 + 언어 리스트 세트
    fun detectLanguagesAndClean(
        textList: List<String>,
        defaultSourceLang: String,
        onResult: (List<Pair<String, String>>) -> Unit
    ) {
        val langTextList = mutableListOf<Pair<String, String>>()
        val identifier = LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.7f).build()
        )
        var completedCount = 0
        textList.forEach { text ->
            val checkText = safeSubstring(text.trim(), 50)
            val trimmed = text.trim()
            if (trimmed.isBlank()) {
                completedCount++
                if (completedCount == textList.size) onResult(langTextList)
                return@forEach
            }
            identifier.identifyLanguage(checkText)
                .addOnSuccessListener { langCode ->
                    if (isLanguageSupported(langCode) || langCode == "und") langTextList.add(langCode to trimmed)
                    else langTextList.add(ComConstant.LOCALE_EN to trimmed)    //지원언어외의 언어는 기본 영어로...
                    completedCount++
                    if (completedCount == textList.size) onResult(langTextList)
                }
                .addOnFailureListener {
                    langTextList.add(ComConstant.LOCALE_EN to trimmed)
                    completedCount++
                    if (completedCount == textList.size) onResult(langTextList)
                }
        }
    }

    fun defaultSourceLanguageSet(
        langTextList: List<Pair<String, String>>, defaultLang: String
    ): List<Pair<String, String>> {
        val langFrequencyMap = langTextList.map { it.first }
            .filter { it != "und" }.groupingBy { it }.eachCount()
        val maxFrequency = langFrequencyMap.values.maxOrNull() ?: 0
        val mostFrequentLangs = langFrequencyMap.filterValues { it == maxFrequency }.keys
        val replaceLang = mostFrequentLangs.firstOrNull() ?: defaultLang
        return langTextList.map { if (it.first == "und") replaceLang to it.second else it }
    }

    fun safeSubstring(text: String, maxLength: Int): String =
        text.trim().toList().take(maxLength).joinToString("")



    // 9. 언어모델 다운로드 여부 체크 (콜백)
    fun isTranslationModelDownloaded(
        langCode: String, callback: (Boolean) -> Unit
    ) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val modelManager = RemoteModelManager.getInstance()
        modelManager.isModelDownloaded(model)
            .addOnSuccessListener { isDownloaded -> callback(isDownloaded) }
            .addOnFailureListener { callback(false) }
    }

    // 10. 여러개 언어모델 다운로드 체크 및 필요시 다운로드 (suspend)
    suspend fun ensureModelsDownloaded(
        context: Context,
        languages: List<String>,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val modelManager = RemoteModelManager.getInstance()
        val uniqueLangs = languages.distinct()
        val missingModels = mutableListOf<TranslateRemoteModel>()
        var checkedCount = 0

        fun handleDownload() {
            if (missingModels.isEmpty()) {
                cont.resume(Unit)
                return
            }
            if (!NetworkUtil.isNetworkConnected(context)) {
                MessageHelper.showAlert(
                    context,
                    title = context.getString(R.string.network_required),
                    message = context.getString(R.string.msg_error_network_disconnected_unabledownload),
                    positiveText = context.getString(R.string.confirm)
                )
                cont.resumeWithException(Exception(context.getString(R.string.msg_network_notconnect)))
                return
            }
            val downloadAction = {
                var downloadedCount = 0
                missingModels.forEach { model ->
                    val conditions = DownloadConditions.Builder().build()
                    modelManager.download(model, conditions)
                        .addOnSuccessListener {

                            MessageHelper.showToast(context, "${model.language} ${context.getString(R.string.msg_mlkit_download_complet)}")
                            downloadedCount++
                            if (downloadedCount == missingModels.size) {
                                cont.resume(Unit)
                            }
                        }
                        .addOnFailureListener { e ->
                            MessageHelper.showToast(context, "${model.language} Download Failed : ${e.message}")
                            downloadedCount++
                            if (downloadedCount == missingModels.size) cont.resume(Unit)
                        }
                }
            }
            if (!NetworkUtil.isWifiConnected(context)) {
                //네트워크 상황 체크
                MessageHelper.showAlert(
                    context,
                    title = context.getString(R.string.notification),
                    message = context.getString(R.string.msg_network_notwifi),
                    positiveText = context.getString(R.string.confirm),
                    negativeText = context.getString(R.string.cancel),
                    cancelable = false,
                    onPositive = {
                        downloadAction()
                    },
                    onNegative = {
                        cont.resumeWithException(Exception( context.getString(R.string.cancel)))
                    }
                )
            } else {
                MessageHelper.showSimpleAlert(
                    context, context.getString(R.string.notification), context.getString(R.string.msg_mlkit_autodownload),
                    positiveAction = { downloadAction() }
                )
            }
        }

        uniqueLangs.forEach { lang ->
            val model = TranslateRemoteModel.Builder(lang).build()
            modelManager.isModelDownloaded(model)
                .addOnSuccessListener { isDownloaded ->
                    if (!isDownloaded) missingModels.add(model)
                    checkedCount++
                    if (checkedCount == uniqueLangs.size) handleDownload()
                }
                .addOnFailureListener { e ->
                    checkedCount++
                    if (checkedCount == uniqueLangs.size) handleDownload()
                }
        }
    }

    // 11. 전각->반각 변환 등 유틸
    val translit = Transliterator.getInstance("Fullwidth-Halfwidth")
    fun normalizeToHalfWidthICU(text: String): String = translit.transliterate(text)
    fun convertFullWidthToHalfWidth(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            val code = char.code
            when {
                code in 0xFF01..0xFF5E -> sb.append((code - 0xFEE0).toChar())
                code == 0x3000 -> sb.append(' ')
                code == 0xFF64 -> sb.append(',') // 일본어 중점(･)
                else -> sb.append(char)
            }
        }
        return sb.toString()
    }

//    fun splitByLanguageGroup(text: String): List<String> {
//        val regex = Regex("([\\p{IsHan}\\p{InHiragana}\\p{InKatakana}ー]+|[a-zA-Z0-9\\s\\p{Punct}]+|[^\\p{L}\\p{N}]+)")
//        return regex.findAll(text)
//            .map { it.value.trim() }
//            .filter { it.isNotBlank() }
//            .toList()
//    }

    fun cleanBracketChars(text: String): String {
        return text.replace(Regex("[\\[\\]{}()<>]"), " ").replace("\\s+".toRegex(), " ").trim()
    }




    // Ml Kit 언어팩 다운로드
    suspend fun downloadMLkitLanguage(
        sourceLanguageCode: String,
        targetLanguageCode: String
    ): Unit = suspendCancellableCoroutine { cont ->
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(
                TranslateLanguage.fromLanguageTag(sourceLanguageCode) ?: TranslateLanguage.ENGLISH
            )
            .setTargetLanguage(
                TranslateLanguage.fromLanguageTag(targetLanguageCode) ?: TranslateLanguage.KOREAN
            )
            .build()
        val translator = Translation.getClient(options)

        cont.invokeOnCancellation { translator.close() }

        translator.downloadModelIfNeeded()
            .addOnSuccessListener {
                translator.close()
                cont.resume(Unit)
            }
            .addOnFailureListener { e ->
                translator.close()
                cont.resumeWithException(e)
            }
    }


    // MLKit 언어팩 삭제 (suspend)
    suspend fun deleteMLKitLanguage(langCode: String): Boolean = suspendCancellableCoroutine { cont ->
        val model = TranslateRemoteModel.Builder(langCode).build()
        val modelManager = RemoteModelManager.getInstance()
        modelManager.deleteDownloadedModel(model)
            .addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { cont.resume(false) }
    }


//    fun translateOcrBlocksByLanguage(
//        context: Context,
//        blockList: List<Text.TextBlock>,
//        targetLang: String,
//        defaultSourceLang: String = "en",
//        onResult: (List<Pair<String, String>>) -> Unit
//    ) {
//        val ocrTexts = blockList.map { it.text.trim() }
//        if (ocrTexts.isEmpty()) {
//            onResult(emptyList())
//            return
//        }
//        // 1) 언어감지
//        detectLanguagesAndClean(ocrTexts, defaultSourceLang) { langTextList ->
//            // 2) 언어별 그룹핑 (langTextList: List<Pair<langCode, text>>)
//            val langGroupMap = langTextList.mapIndexed { idx, pair -> Triple(idx, pair.first, pair.second) }
//                .groupBy { it.second } // langCode 기준
//            val resultList = MutableList(ocrTexts.size) { Pair("", "") }
//            var finishedLangCount = 0
//
//            if (langGroupMap.isEmpty()) {
//                onResult(resultList)
//                return@detectLanguagesAndClean
//            }
//
//            langGroupMap.forEach { (langCode, groupList) ->
//                val indices = groupList.map { it.first }
//                val textsForTranslate = groupList.map { it.third }
//                // "und" 또는 지원하지 않는 언어면 "en"으로 처리
//                val safeLangCode = if (MLKitTranslatorModule.isLanguageSupported(langCode)) langCode else "en"
//
//                (context as? LifecycleOwner)?.lifecycleScope?.launch {
//                    val translatedList = try {
//                        MLKitTranslatorModule.translateMultiple(context, textsForTranslate, targetLang, safeLangCode)
//                    } catch (e: Exception) {
//                        textsForTranslate // 실패시 원문 반환
//                    }
//                    indices.forEachIndexed { i, idx ->
//                        resultList[idx] = textsForTranslate[i] to translatedList.getOrElse(i) { "" }
//                    }
//                    finishedLangCount++
//                    if (finishedLangCount == langGroupMap.size) {
//                        onResult(resultList)
//                    }
//                }
//            }
//        }
//    }
    // 너무 짧은 블록은 이전/다음 블록과 합치거나, 일정 길이 이상이 될 때까지 병합
//    fun isMeaninglessBlock(text: String): Boolean {
//        val trimmed = text.trim()
//        return trimmed.length < 2
//                || Regex("^[a-zA-Z\\s.\\-_,…\\d]+$").matches(trimmed) // 알파벳, 숫자, 특수문자만
//                || Regex("^[.\\-_,…\\s]+$").matches(trimmed)
//    }

    fun mergeFragmentedBlocks(blocks: List<Text.TextBlock>, minLength: Int = 15): List<String> {
        val mergedList = mutableListOf<String>()
        var buffer = StringBuilder()
        for (block in blocks) {
            val text = block.text.trim()
            if (buffer.isNotEmpty()) buffer.append(" ")
            buffer.append(text)
            if (buffer.length >= minLength) {
                mergedList.add(buffer.toString())
                buffer = StringBuilder()
            }
        }
        if (buffer.isNotEmpty()) mergedList.add(buffer.toString())
        return mergedList
    }
    fun splitByLanguageGroup(input: String): List<String> {
        if (input.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        val buffer = StringBuilder()
        var currentGroup = languageType(input.first())

        for (c in input) {
            val type = languageType(c)
            if (type == currentGroup) {
                buffer.append(c)
            } else {
                if (buffer.isNotEmpty()) result.add(buffer.toString())
                buffer.clear().append(c)
                currentGroup = type
            }
        }

        if (buffer.isNotEmpty()) result.add(buffer.toString())
        return result.filter { it.isNotBlank() }
    }

    private fun languageType(c: Char): String = when (c.code) {
        in 0x3040..0x30FF -> "ja"   // 일본어 가타카나/히라가나
        in 0x4E00..0x9FFF -> "zh"   // 한자
        in 0xAC00..0xD7AF -> "ko"   // 한글
        in 0x0041..0x007A, in 0x0030..0x0039 -> "en" // 영문자, 숫자
        else -> "etc"
    }


    fun romanizeText(text: String, srcLang: String, targetLang: String): String {
        return when {
            // 영어 → 한글 (발음/음차)
            srcLang.startsWith("en") && targetLang.startsWith("ko") ->
                TranscriptionEnToKo.transliterateSentence(text)

            // 한글 → 영어 (로마자 변환, 예: Revised Romanization)
            srcLang.startsWith("ko") && targetLang.startsWith("en") ->
                romanizeKorean(text)

            // 일본어 → 영어 (로마자)
            srcLang.startsWith("ja") && targetLang.startsWith("en") ->
                romanizeJapanese(text)

//            // 일본어 → 한글 (음차, 필요한 경우 직접 구현)
//            srcLang.startsWith("ja") && targetLang.startsWith("ko") ->
//                transliterateJapToKor(text) // 직접 구현 필요
//
//            // 중국어 → 한글
//            srcLang.startsWith("zh") && targetLang.startsWith("ko") ->
//                transliterateChnToKor(text) // 직접 구현 필요

            // 그 외: 원문 그대로
            else -> text
        }
    }
    // 1. 한글 로마자 변환 (ICU Transliterator 이용)
    fun romanizeKorean(text: String): String {
        val transliterator = android.icu.text.Transliterator.getInstance("Hangul-Latin")
        return transliterator.transliterate(text)
    }

    // 2. 일본어 로마자 변환 (ICU Transliterator 이용)
    fun romanizeJapanese(text: String): String {
        val transliterator = android.icu.text.Transliterator.getInstance("Katakana-Latin; Hiragana-Latin")
        return transliterator.transliterate(text)
    }

//    // 3. 중국어(한자) → 핀인 변환은 pinyin4j 등의 외부 라이브러리 필요!
//// 없을 경우 원문 반환 (또는 추후 구현)
//    fun romanizeChinese(text: String): String {
//        // TODO: pinyin4j 등 외부 라이브러리 연동 필요
//        return text
//    }

    /**
     * 여러 텍스트(블록)를 다국어로 번역(코루틴 기반, 예외 robust)
     * - 각 텍스트마다 언어 감지 후, 필요한 언어팩을 일괄 다운로드, 번역 진행
     */
    suspend fun translateMultipleWithRobustHandling(
        context: Context,
        textList: List<String>,
        targetLang: String,
        defaultSourceLang: String = "en",
        splitMaxLen: Int = 500
    ): List<String> = withContext(Dispatchers.IO) {

        if (textList.isEmpty()) return@withContext textList

        // [1] 언어 감지
        val langTextList: List<Pair<String, String>> = suspendCancellableCoroutine { cont ->
            detectLanguagesAndClean(textList, defaultSourceLang) { result -> cont.resume(result) }
        }

        if (langTextList.isEmpty()) return@withContext textList

        // [2] "und" 보정 → 최빈값 또는 기본 언어
        val finalLangTextList = defaultSourceLanguageSet(langTextList, defaultSourceLang)

        // [3] 언어 그룹핑
        val langGrouped = finalLangTextList.mapIndexed { idx, pair -> Triple(idx, pair.first, pair.second) }
            .groupBy { it.second }  // langCode 기준

        val resultList = MutableList(textList.size) { "" }

        // [4] 필요한 모델 다운로드
        val allLangs = langGrouped.keys + targetLang
        try {
            ensureModelsDownloaded(context, allLangs.toList())
        } catch (e: Exception) {
            return@withContext textList
        }

        // [5] 언어 그룹별로 번역 수행
        for ((langCode, blockList) in langGrouped) {
            val srcLang = if (isLanguageSupported(langCode)) langCode else defaultSourceLang
            val indices = blockList.map { it.first }
            val texts = blockList.map { it.third }

            val translatedChunks = mutableListOf<String>()

            for (text in texts) {
                try {
                    // [SAFEGUARD] 문서 전체 1차 정제 (과도 반복/길이 컷)
                    val safeWhole = sanitizeForNmt(
                        raw = text,
                        maxChars = 2000,
                        maxRepeat = 8
                    )

                    // 기존: 문장 분리
                    val split = smartChunkByPunctuation(safeWhole, splitMaxLen)

                    // [SAFEGUARD] 문장 분리 결과를 다시 안전 재분할 + sanitize (문장 자체가 길 때 보호)
                    val safeChunks = makeSafeChunks(
                        sentenceChunks = split,
                        innerMaxChars = 600,  // 청크 크기 상한(권장 400~800)
                        maxRepeat = 8
                    )

                    val merged = StringBuilder()
                    for (chunk in safeChunks) {
                        // [TIMEOUT] 청크 단위 타임아웃 (NMT 응답 지연 방지)
                        val translated = withTimeout(7_000) {
                            translateText(context, srcLang, targetLang, chunk)
                        }

                        // 번역 결과가 원문과 "완전히" 동일하면 발음으로 변환
                        val finalText = if (translated.trim().lowercase() == chunk.trim().lowercase() && srcLang != targetLang) {
                            romanizeText(chunk, srcLang, targetLang)
                        } else {
                            translated
                        }
                        merged.append(finalText)
                    }
                    translatedChunks.add(merged.toString())
                } catch (ce: CancellationException) {
                    // [CANCEL] 취소는 전파하여 상위 스코프에서 안전 종료
                    throw ce
                } catch (toe: TimeoutCancellationException) {
                    // 청크 또는 모델 타임아웃
                    Log.e("Translate", "Timeout during translation chunk", toe)
                    translatedChunks.add("[번역 실패: 시간 초과]")
                } catch (iae: IllegalArgumentException) {
                    Log.e("Translate", "Invalid argument to translator", iae)
                    translatedChunks.add("[번역 실패: 입력 오류]")
                } catch (oom: OutOfMemoryError) {
                    // 드물지만 네이티브 OOM을 캐치할 수 있도록 분리
                    Log.e("Translate", "Native OOM during translation", oom)
                    translatedChunks.add("[번역 실패: 메모리 부족]")
                } catch (t: Throwable) {
                    // 네이티브 크래시 전 프레임에서 잡히는 예외 로깅(안정성 확보)
                    Log.e("Translate", "Unexpected error during translation", t)
                    translatedChunks.add("[번역 실패]")
                }
            }

            // [6] 결과 복원
            indices.forEachIndexed { i, idx ->
                resultList[idx] = translatedChunks.getOrElse(i) { "" }
            }
        }

        return@withContext resultList
    }
    // --------------------------------------------
// [신규] 긴 텍스트를 구두점 기준으로 안전 분할
// - 일본어: 。、！？；：  / 영어: . , ! ? ; :  / 개행 포함
// - maxLen 근처에서 '가장 가까운 이전 구두점'을 찾아 자름
// - 못 찾으면 공백 기준, 그것도 없으면 강제 절단(원래 동작과 동일)
// --------------------------------------------
    private fun smartChunkByPunctuation(
        input: String,
        maxLen: Int
    ): List<String> {
        if (input.length <= maxLen) return listOf(input)

        val punctRegex = Regex("[。．\\.！？!?,、，；;：:]+") // 구두점
        val chunks = mutableListOf<String>()
        var start = 0
        val n = input.length

        while (start < n) {
            val remain = n - start
            if (remain <= maxLen) {
                chunks += input.substring(start, n)
                break
            }

            val endCandidate = start + maxLen
            val window = input.substring(start, endCandidate)

            // 1) 구두점 중 가장 뒤에 있는 것
            val punctMatches = punctRegex.findAll(window).toList()
            val punctCut = punctMatches.lastOrNull()?.range?.last

            val cutAt = when {
                // 구두점 발견: 거기서 자름
                punctCut != null -> start + punctCut + 1

                // 2) 개행이 있다면 마지막 개행 직후에서 자름
                window.lastIndexOf('\n').takeIf { it >= 0 }?.let { start + it + 1 } != null ->
                    start + window.lastIndexOf('\n') + 1

                // 3) 공백이 있다면 마지막 공백 직후에서 자름
                window.lastIndexOf(' ').takeIf { it >= 0 }?.let { start + it + 1 } != null ->
                    start + window.lastIndexOf(' ') + 1

                // 4) 아무 것도 없으면 강제 절단(기존과 동일한 fallback)
                else -> endCandidate
            }

            chunks += input.substring(start, cutAt).trimEnd()
            start = cutAt
            // 연속 공백/개행 정리(원문 훼손 없이 시작부만 정리)
            while (start < n && (input[start] == ' ' || input[start] == '\n' || input[start] == '\r' || input[start] == '\t')) {
                start++
            }
        }
        return chunks
    }


    fun splitTextBySentences(text: String, maxLen: Int = 500): List<String> {
        val rawSentences = text.split(Regex("[\\.\\n]+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val result = mutableListOf<String>()
        var current = StringBuilder()
        for (sentence in rawSentences) {
            if (current.length + sentence.length > maxLen && current.isNotEmpty()) {
                result.add(current.toString().trim())
                current = StringBuilder()
            }
            // ⬇️ 마침표 붙이지 않고, sentence만 추가
            if (current.isNotEmpty()) current.append(" ")
            current.append(sentence)
        }
        if (current.isNotEmpty()) result.add(current.toString().trim())
        return result
    }
//    suspend fun translateMultipleWithRobustHandling(
//        context: Context,
//        textList: List<String>,
//        targetLang: String,
//        defaultSourceLang: String = "en",
//        splitMaxLen: Int = 500
//    ): List<String> = withContext(Dispatchers.IO) {
//
//        if (textList.isEmpty()) return@withContext textList
//
//        // [1] 언어 감지
//        val langTextList: List<Pair<String, String>> = suspendCancellableCoroutine { cont ->
//            detectLanguagesAndClean(textList, defaultSourceLang) { result -> cont.resume(result) }
//        }
//
//        if (langTextList.isEmpty()) return@withContext textList
//
//        // [2] "und" 보정 → 최빈값 또는 기본 언어
//        val finalLangTextList = defaultSourceLanguageSet(langTextList, defaultSourceLang)
//
//        // [3] 언어 그룹핑
//        val langGrouped = finalLangTextList.mapIndexed { idx, pair -> Triple(idx, pair.first, pair.second) }
//            .groupBy { it.second }  // langCode 기준
//
//        val resultList = MutableList(textList.size) { "" }
//
//        // [4] 필요한 모델 다운로드
//        val allLangs = langGrouped.keys + targetLang
//        try {
//            ensureModelsDownloaded(context, allLangs.toList())
//        } catch (e: Exception) {
//            return@withContext textList
//        }
//
//        // [5] 언어 그룹별로 번역 수행
//        for ((langCode, blockList) in langGrouped) {
//            val srcLang = if (isLanguageSupported(langCode)) langCode else defaultSourceLang
//            val indices = blockList.map { it.first }
//            val texts = blockList.map { it.third }
//
//            val translatedChunks = mutableListOf<String>()
//
//            for (text in texts) {
//                try {
//                    val split = splitTextBySentences(text, splitMaxLen)
//                    val merged = StringBuilder()
//                    for (chunk in split) {
//                        val translated = translateText(context, srcLang, targetLang, chunk)
//                        // 번역 결과가 원문과 "완전히" 동일하면 발음으로 변환
//                        val finalText = if (translated.trim().lowercase() == chunk.trim().lowercase() && srcLang != targetLang) {
//                            romanizeText(chunk, srcLang, targetLang)
//                        } else {
//                            translated
//                        }
//                        merged.append(finalText)
//                    }
//                    translatedChunks.add(merged.toString())
//                } catch (e: Exception) {
//                    translatedChunks.add("[번역 실패]")
//                }
//            }
//
//            // [6] 결과 복원
//            indices.forEachIndexed { i, idx ->
//                resultList[idx] = translatedChunks.getOrElse(i) { "" }
//            }
//        }
//
//        return@withContext resultList
//    }

    /*
    공통코드 영역 중 번역이 필요한 경우 사용 (단 지원언어는 skip,, 현재는 영어만)
     */
    suspend fun translateListToList(
        context: Context,
        inputList: List<String>,
        sourceLang: String,
        targetLang: String,
        maxLength: Int = 300,
        separator: String = ","
    ): List<String> = withContext(Dispatchers.IO) {
        if (inputList.isEmpty()) return@withContext inputList

        val groupedChunks = mutableListOf<String>()
        val originalItemCounts = mutableListOf<Int>()
        val currentItems = mutableListOf<String>()
        var currentLength = 0


//        // source 와 target이 같은 경우는 skip
//        if ( sourceLang == targetLang ) return@withContext inputList

        //지원 언어의 경우 skip
        if ( targetLang == ComConstant.LOCALE_EN) return@withContext inputList

        for (item in inputList) {
            val itemLengthWithSep = if (currentItems.isEmpty()) item.length else item.length + separator.length
            if (currentLength + itemLengthWithSep <= maxLength) {
                currentItems.add(item)
                currentLength += itemLengthWithSep
            } else {
                groupedChunks.add(currentItems.joinToString(separator))
                originalItemCounts.add(currentItems.size)
                currentItems.clear()
                currentItems.add(item)
                currentLength = item.length
            }
        }
        if (currentItems.isNotEmpty()) {
            groupedChunks.add(currentItems.joinToString(separator))
            originalItemCounts.add(currentItems.size)
        }

        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)
        val translatedGroups = mutableListOf<String>()

        try {
            MLKitTranslatorModule.ensureModelsDownloaded(context, listOf(sourceLang, targetLang))
            translator.downloadModelIfNeeded().await()

            for (chunk in groupedChunks) {
                try {
                    val translated = translator.translate(chunk).await()
                    val normalized = MLKitTranslatorModule.convertFullWidthToHalfWidth(
                        MLKitTranslatorModule.normalizeToHalfWidthICU(translated)
                    )
                    translatedGroups.add(normalized)
                } catch (e: Exception) {
                    translatedGroups.add(chunk)
                }
            }
        } catch (e: Exception) {
            translatedGroups.clear()
            translatedGroups.addAll(groupedChunks)
        } finally {
            translator.close()
        }

        // 복원
        val finalList = mutableListOf<String>()
        var inputIndex = 0
        for ((groupIndex, group) in translatedGroups.withIndex()) {
            val expectedItemCount = originalItemCounts[groupIndex]
            // separator로 나눈 뒤, 공백이 아닌 것만 사용!
            val items = group.split(separator).map { it.trim() }.filter { it.isNotEmpty() }
            if (items.size == expectedItemCount) {
                finalList.addAll(items)
            } else {
                finalList.addAll(inputList.subList(inputIndex, inputIndex + expectedItemCount))
            }
            inputIndex += expectedItemCount
        }
        return@withContext finalList
    }


    /**
     * OCR 후처리: 숫자 오인식(I→1, O→0), 마침표, 다중공백 등 보정
     */
    fun fixCommonOcrNumberErrors(text: String): String {
        return text
            // 'I'가 숫자(콤마/점 포함) 바로 앞에 있으면 '1'로 치환
            .replace(Regex("""\bI(?=[,.\d])"""), "1")
            // 'O'가 숫자(콤마/점 포함) 바로 앞에 있으면 '0'로 치환
            .replace(Regex("""\bO(?=[,.\d])"""), "0")
            // 탭/개행/리턴 → 공백
            .replace(Regex("[\\t\\r\\n]+"), " ")
            // 다중 공백 → 단일 공백
            .replace(Regex(" {2,}"), " ")
            .trim()
    }



}

//    /**
//     * 여러 문장/문단(다국어 포함) → 모두 원하는 언어로 번역
//     * - 각 문장별로 감지언어가 targetLang과 같으면 "en"으로 강제 번역
//     * - 감지언어가 지원 언어가 아니면 defaultSourceLang 사용
//     * - 원본 순서 유지
//     * @param context Context
//     * @param textList 번역 대상 문장(들)
//     * @param targetLang 번역 목표 언어 (예: "ko")
//     * @param defaultSourceLang 기본 소스 언어 (예: "en")
//     * @return List<String> (번역 결과)
//     */
//    suspend fun translateMultiple(
//        context: Context,
//        textList: List<String>,
//        targetLang: String,
//        defaultSourceLang: String = "en"
//    ): List<String> = withContext(Dispatchers.IO) {
//        if (textList.isEmpty()) {
//            MessageHelper.showToast(context, context.getString(R.string.msg_translation_notexist_text))
//            return@withContext textList
//        }
//
//        // 1. 각 문장별 언어 감지 & 타겟과 같으면 "en"으로 강제, 지원 언어 아니면 default
//        val identifier = LanguageIdentification.getClient(
//            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.7f).build()
//        )
//        val langTextList = MutableList<Pair<String, String>?>(textList.size) { null }
//        var completed = 0
//
//        suspendCancellableCoroutine<List<Pair<String, String>>> { cont ->
//            textList.forEachIndexed { idx, text ->
//                val trimmed = text.trim()
//                if (trimmed.isBlank()) {
//                    langTextList[idx] = defaultSourceLang to trimmed
//                    completed++
//                    if (completed == textList.size) cont.resume(langTextList.map { it!! })
//                    return@forEachIndexed
//                }
//                val checkText = trimmed.take(50)
//                identifier.identifyLanguage(checkText)
//                    .addOnSuccessListener { langCode ->
//                        val useLang = when {
//                            //langCode == targetLang -> "en"
//                            MLKitTranslatorModule.isLanguageSupported(langCode) || langCode == "und" -> langCode
//                            else -> defaultSourceLang
//                        }
//                        langTextList[idx] = useLang to trimmed
//                        completed++
//                        if (completed == textList.size) cont.resume(langTextList.map { it!! })
//                    }
//                    .addOnFailureListener {
//                        langTextList[idx] = defaultSourceLang to trimmed
//                        completed++
//                        if (completed == textList.size) cont.resume(langTextList.map { it!! })
//                    }
//            }
//        }.let { detectedList ->
//            val srcLangs = detectedList.map { it.first }.distinct()
//            try {
//                MLKitTranslatorModule.ensureModelsDownloaded(context, srcLangs + targetLang)
//            } catch (_: Exception) { /* ignore */ }
//            val resultList = mutableListOf<String>()
//            for ((srcLang, txt) in detectedList) {
//                val translated = try {
//                    MLKitTranslatorModule.translateText(context, srcLang, targetLang, txt)
//                } catch (_: Exception) {
//                    txt
//                }
//                resultList.add(translated)
//            }
//            resultList
//        }
//    }