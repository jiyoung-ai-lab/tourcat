package com.waveapp.tourcat.helper

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.waveapp.tourcat.common.CityList
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.common.NationList
import com.waveapp.tourcat.util.LogUtil
import java.util.Locale

object UserLocationHelper {

    /**
     * 현재 위치 기반 도시/국가 Pref 저장 (권한 없으면 국가 기본값만)
     */
    @Suppress("MissingPermission")
    fun updateLocation(
        context: Context,
        fusedLocationClient: FusedLocationProviderClient? = null,
        manualCheck: Boolean = false
    ) {
        // 1. 위치 권한 체크 (COARSE or FINE)
        val hasLocationPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)

        if (!hasLocationPermission) {
            // 권한 없으면 국가 기본값만 Pref에 세팅 (도시/위경도 null)
            prefs.edit().apply {
                putString(ComConstant.PREF_KEYS.POSITION_CITY, null)
                putString(ComConstant.PREF_KEYS.POSITION_NATION, ComConstant.USER_NATION_CODE)
                putString(ComConstant.PREF_KEYS.LAST_KNOWN_LATITUDE, null)
                putString(ComConstant.PREF_KEYS.LAST_KNOWN_LONGITUDE, null)
                putLong(ComConstant.PREF_KEYS.LAST_LOGIN_TIME, System.currentTimeMillis())
            }.apply()
            LogUtil.w("위치 권한 미동의: 국가 기본값(${ComConstant.USER_NATION_CODE})만 저장", gubun = "UserLocation")
            return
        }

        // 2. FusedLocationProviderClient 인스턴스화 (파라미터가 null이면 새로 생성)
        val fusedClient = fusedLocationClient ?: LocationServices.getFusedLocationProviderClient(context)

        // 3. 최신 위치 요청 (getCurrentLocation, 가장 심플)
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    try {
                        val geocoder = Geocoder(context, Locale.getDefault())
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            val newCity = address.locality
                                ?: address.subAdminArea
                                ?: address.adminArea

                            val newNation = address.countryCode // 예: "KR", "JP"
                            val lastCity = prefs.getString(ComConstant.PREF_KEYS.POSITION_CITY, null)
                            val lastNation = prefs.getString(ComConstant.PREF_KEYS.POSITION_NATION, null)
                            // 변경 체크
                            if (manualCheck || newCity != lastCity || newNation != lastNation) {
                                prefs.edit().apply {
                                    putString(ComConstant.PREF_KEYS.POSITION_CITY, newCity)
                                    putString(ComConstant.PREF_KEYS.POSITION_NATION, newNation)
                                    putString(ComConstant.PREF_KEYS.LAST_KNOWN_LATITUDE, location.latitude.toString())
                                    putString(ComConstant.PREF_KEYS.LAST_KNOWN_LONGITUDE, location.longitude.toString())
                                    putLong(ComConstant.PREF_KEYS.LAST_LOGIN_TIME, System.currentTimeMillis())
                                }.apply()
                                LogUtil.i("위치 갱신됨: $newCity, $newNation (${location.latitude}, ${location.longitude})", gubun = "UserLocation")
                            } else {
                                LogUtil.d("위치 변경 없음 → Pref 유지", gubun = "UserLocation")
                            }
                        } else {
                            LogUtil.w("Geocoder 변환 실패", gubun = "UserLocation")
                        }
                    } catch (e: Exception) {
                        LogUtil.e("Geocoder 실행 중 예외 발생", gubun = "UserLocation", tr = e)
                    }
                } else {
                    LogUtil.w("getCurrentLocation 결과 null", gubun = "UserLocation")
                }
            }
            .addOnFailureListener {
                LogUtil.e("위치 가져오기 실패", gubun = "UserLocation", tr = it)
            }
    }

    /** Pref 기반 도시코드 또는 국가코드의 영문명 반환 (도시 → 국가 fallback) */
    fun getTourPositionName(context: Context): String {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        val cityName = prefs.getString(ComConstant.PREF_KEYS.POSITION_CITY, null)
        val nationCode = prefs.getString(ComConstant.PREF_KEYS.POSITION_NATION, ComConstant.USER_NATION_CODE)

//        // 1. 도시 코드 → 영문 도시명 반환
//        if (!cityName.isNullOrBlank()) {
//            val city = CityList.findCity(cityName)
//            val cityEn = city?.names?.get("en")
//            if (!cityEn.isNullOrBlank()) return cityEn
//        }

        //도시명이 있으면 도시명 return
        if (!cityName.isNullOrBlank()) return cityName

        // 2. 국가 영문명 반환
        val nationEn = try {
            NationList.findName(nationCode ?: ComConstant.USER_NATION_CODE, ComConstant.LOCALE_EN)
        } catch (e: Exception) {
            null
        }
        if (!nationEn.isNullOrBlank()) return nationEn

        // 3. 국가코드 반환
        if (!nationCode.isNullOrBlank()) return nationCode

        // 4. 기본값
        return ComConstant.USER_NATION_CODE
    }


    /** Pref에서 저장된 도시 반환 (권한없으면 null) */
    fun getSavedCity(context: Context): String? {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ComConstant.PREF_KEYS.POSITION_CITY, null)
    }

    /** Pref에서 저장된 국가 코드 반환 (권한없으면 ComConstant.USER_NATION) */
    fun getSavedNation(context: Context): String? {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ComConstant.PREF_KEYS.POSITION_NATION, ComConstant.USER_NATION_CODE)
    }

    /** Pref에서 저장된 마지막 위도 (권한없으면 null) */
    fun getLastLatitude(context: Context): Double? {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ComConstant.PREF_KEYS.LAST_KNOWN_LATITUDE, null)?.toDoubleOrNull()
    }

    /** Pref에서 저장된 마지막 경도 (권한없으면 null) */
    fun getLastLongitude(context: Context): Double? {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(ComConstant.PREF_KEYS.LAST_KNOWN_LONGITUDE, null)?.toDoubleOrNull()
    }

    /** Pref에서 저장된 마지막 갱신 시간 (권한없으면 최근 호출 시각, 없으면 0) */
    fun getLastUpdateTime(context: Context): Long {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(ComConstant.PREF_KEYS.LAST_LOGIN_TIME, 0)
    }
}
