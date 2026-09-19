package com.waveapp.tourcat.util

import android.icu.text.BreakIterator

/**
 * 번역 입력 세이프가드:
 * 1) 공백 정규화
 * 2) 동일 토큰의 과도한 연속 반복 축약 (maxRepeat)
 * 3) 총 길이 상한 (maxChars, 문자 경계 안전 절단)
 *
 * 기존 번역 로직을 변경하지 않고, 번역 호출 직전에 한 번만 통과시키면 됩니다.
 */
fun sanitizeForNmt(
    raw: String,
    maxChars: Int = 2000,
    maxRepeat: Int = 8
): String {
    if (raw.isBlank()) return raw

    // 1) 공백 정규화
    var t = raw.replace(Regex("\\s+"), " ").trim()

    // 2) 동일 토큰 과도 반복 축약 (한/영/숫자 토큰 기준)
    t = buildString {
        var last = ""
        var cnt = 0
        for (token in t.split(' ')) {
            val same = token == last
            cnt = if (same) cnt + 1 else 1
            if (!same || cnt <= maxRepeat) {
                if (isNotEmpty()) append(' ')
                append(token)
            }
            last = token
        }
    }

    // 3) 총 길이 상한 (문자 경계 기준 안전 절단)
    if (t.length > maxChars) t = safeTakeChars(t, maxChars)

    return t
}

/** 문자 경계(그래프림 클러스터) 기준으로 안전하게 앞에서 N자 자르기 */
fun safeTakeChars(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(text)
    val end = bi.following(maxChars)
    val cut = if (end == BreakIterator.DONE) maxChars else end
    return text.substring(0, cut)
}

/**
 * 안전 분할:
 * - 먼저 '기존의' 문장 분리 결과(호출측 함수) 리스트를 입력으로 받는다고 가정
 * - 각 문장을 다시 '문자 경계' 기준으로 잘라서 maxChars 이하의 청크로 만듦
 */
fun reChunkByCharBoundary(sentence: String, maxChars: Int): List<String> {
    if (sentence.length <= maxChars) return listOf(sentence)
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(sentence)

    val chunks = ArrayList<String>()
    var lastCut = 0
    var boundary = bi.first()

    while (true) {
        val next = bi.next()
        if (next == BreakIterator.DONE) {
            // 마지막 남은 조각
            if (lastCut < sentence.length) {
                chunks.add(sentence.substring(lastCut))
            }
            break
        }
        // 경계 구간이 maxChars를 초과하면 직전 경계까지 청크로 추가
        if (next - lastCut >= maxChars) {
            chunks.add(sentence.substring(lastCut, next))
            lastCut = next
        }
        boundary = next
    }

    return chunks
}

/**
 * '문장 분리 결과'에 대해 안전 재분할을 적용하고,
 * 각 조각을 한 번 더 sanitize 해 반환.
 */
fun makeSafeChunks(
    sentenceChunks: List<String>,
    innerMaxChars: Int = 600,
    maxRepeat: Int = 8
): List<String> {
    val out = ArrayList<String>(sentenceChunks.size)
    for (s in sentenceChunks) {
        val pieces = reChunkByCharBoundary(s, innerMaxChars)
        for (p in pieces) {
            val cleaned = sanitizeForNmt(p, maxChars = innerMaxChars, maxRepeat = maxRepeat)
            if (cleaned.isNotBlank()) out.add(cleaned)
        }
    }
    return out
}