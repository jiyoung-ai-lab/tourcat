package com.waveapp.tourcat.util

import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 *  시간은 시간대를 포함한 UDT 적용 ("yyyy-MM-dd'T'HH:mm:ssXXX")
 *  - 입력이 ISO 변형이어도 깨지지 않도록 유연 파싱 적용
 *  - 파싱 실패 시 원문 반환 대응
 */
object DateTimeUtil {

    private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val displaySimpleFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val ISO_UTC_SECONDS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT)
    private val ISO_UTC_MILLISECONDS = DateTimeFormatter.ISO_INSTANT

    fun nowUtcIsoSeconds(): String {
        val instant = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        return instant.atOffset(ZoneOffset.UTC).format(ISO_UTC_SECONDS)
    }

    fun nowUtcIsoMiliSeconds(): String {
        val instant = Instant.now().truncatedTo(ChronoUnit.MILLIS)
        return ISO_UTC_MILLISECONDS.format(instant)
    }

    fun toUtcIsoSeconds(instant: Instant): String {
        val trimmed = instant.truncatedTo(ChronoUnit.SECONDS)
        return trimmed.atOffset(ZoneOffset.UTC).format(ISO_UTC_SECONDS)
    }

    /**
     * ISO 문자열 → Instant 변환 (유연 파싱)
     * 실패 시 null 반환
     */
    fun parseUtcIso(isoUtc: String): Instant? {
        val s = isoUtc.trim()

        if (s.all { it.isDigit() }) {
            return try {
                when {
                    s.length >= 13 -> Instant.ofEpochMilli(s.toLong())
                    else -> Instant.ofEpochSecond(s.toLong())
                }
            } catch (e: Exception) {
                null
            }
        }

        val attempts: List<() -> Instant> = listOf(
            { Instant.parse(s) },
            { OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant() },
            {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXX", Locale.ROOT)
                OffsetDateTime.parse(s, fmt).toInstant()
            },
            {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT)
                OffsetDateTime.parse(s, fmt).toInstant()
            },
            {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXX", Locale.ROOT)
                OffsetDateTime.parse(s, fmt).toInstant()
            },
            { ZonedDateTime.parse(s, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant() },
            {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", Locale.ROOT)
                LocalDateTime.parse(s, fmt).atZone(ZoneId.systemDefault()).toInstant()
            },
            {
                LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    .atZone(ZoneId.systemDefault()).toInstant()
            }
        )

        for (fn in attempts) {
            try {
                return fn()
            } catch (_: Throwable) {
            }
        }

        return null
    }

    /**
     * 포맷된 문자열 반환 (실패 시 원문 반환)
     */
    fun formatForDisplay(
        isoUtc: String,
        pattern: String = "yyyy/MM/dd hh:mm:ss",
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        return try {
            val instant = parseUtcIso(isoUtc) ?: return isoUtc
            val zoned = instant.atZone(zoneId)
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            zoned.format(formatter)
        } catch (_: Exception) {
            isoUtc
        }
    }
    fun formatForDisplayJustDate(
        isoUtc: String,
        pattern: String = "yyyy/MM/dd hh:mm:ss",
        locale: Locale = Locale.getDefault(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): String {
        return try {
            val instant = parseUtcIso(isoUtc) ?: return isoUtc
            val zoned = instant.atZone(zoneId)
            val formatter = DateTimeFormatter.ofPattern(pattern, locale)
            zoned.format(formatter)
        } catch (_: Exception) {
            isoUtc
        }
    }
    /**
     * ISO → ZonedDateTime 변환 (실패 시 현재 시간 반환)
     */
    fun toDeviceZonedDateTime(
        isoUtc: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ZonedDateTime {
        return try {
            val instant = parseUtcIso(isoUtc) ?: return ZonedDateTime.now(zoneId)
            instant.atZone(zoneId)
        } catch (_: Exception) {
            ZonedDateTime.now(zoneId)
        }
    }

    /**
     * "yyyyMMdd" 문자열을 로컬 시간으로 변환 (실패 시 원문 반환)
     */
    fun toDeviceZonedDateTimeStr(dateStr: String): String {
        return try {
            val localDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"))
            localDate.atStartOfDay(ZoneId.systemDefault()).format(displayFormatter)
        } catch (_: Exception) {
            dateStr
        }
    }

    fun getTodayCompact(): String {
        return LocalDate.now(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    /**
     * Long 타입 시간(ms)을 현재 시간과 비교해서 (System.currrenmilisecond 값)
     * - 1시간 이하는 "xx분 전"
     * - 1시간 초과 ~ 24시간 이하는 "xx시간 전"
     * - 24시간 초과는 "yyyy-MM-dd" 형식으로 변환
     */
    fun formatRelativeTime(receivedAt: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = now - receivedAt

        return when {
            diffMillis < TimeUnit.HOURS.toMillis(1) -> {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis)
                "${minutes}분 전"
            }
            diffMillis < TimeUnit.DAYS.toMillis(1) -> {
                val hours = TimeUnit.MILLISECONDS.toHours(diffMillis)
                "${hours}시간 전"
            }
            else -> {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateFormat.format(receivedAt)
            }
        }
    }

}
