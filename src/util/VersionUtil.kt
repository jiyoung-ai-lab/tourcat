package com.waveapp.tourcat.util

import android.content.Context
import android.content.pm.PackageManager

object VersionUtil {
    // 앱의 버전명 (ex: 1.2.3)
    var versionName: String = ""
        private set

    // 앱의 버전코드 (ex: 10003)
    var versionCode: Long = 0
        private set

//            (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
    // 최초 1회만 앱 버전 정보를 읽어옴 (App start시 초기화 권장)
    fun init(context: Context) {
        try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = info.versionName ?: ""
            versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
                info.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                info.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            versionName = ""
            versionCode = 0
        }
    }

    // 버전 문자열을 숫자 배열로 변환 (ex: "1.2.3" -> [1,2,3])
    private fun parseVersion(version: String): List<Int> =
        version.split(".").map { it.toIntOrNull() ?: 0 }

    // 버전 비교 (ex: "1.2.3" >= "1.2.1"  -> true)
    fun isAtLeast(target: String): Boolean =
        compareVersion(versionName, target) >= 0

    fun isBelow(target: String): Boolean =
        compareVersion(versionName, target) < 0

    fun isSame(target: String): Boolean =
        compareVersion(versionName, target) == 0

    // 범위 내인지 (inclusive)
    fun isBetween(min: String, max: String): Boolean =
        compareVersion(versionName, min) >= 0 && compareVersion(versionName, max) <= 0

    // 버전 문자열 비교 결과 (-1,0,1)
    fun compareVersion(ver1: String, ver2: String): Int {
        val v1 = parseVersion(ver1)
        val v2 = parseVersion(ver2)
        val max = maxOf(v1.size, v2.size)
        for (i in 0 until max) {
            val num1 = v1.getOrNull(i) ?: 0
            val num2 = v2.getOrNull(i) ?: 0
            if (num1 != num2) return num1 - num2
        }
        return 0
    }

    //API 21 전후로 많은 것이 바뀌어서 해당 내용 확인하는 소스코드

}
