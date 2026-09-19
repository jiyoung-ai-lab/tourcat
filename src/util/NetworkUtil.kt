package com.waveapp.tourcat.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.InputStream

object NetworkUtil {

    //wifi 가능여부 : true, false
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    //네트워크 가능여부:true,false
    fun isNetworkConnected(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    //네트워크는 정상이나 wifi만 아닌경우 ( 다운로드시 warning 용)
    fun isNetworkAvailableButNotWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false

        val isNetworkAvailable = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        return isNetworkAvailable && !isWifi
    }

    //중국 network 인지 체크 (중국의 경우 서비스 자체가 불가)  :  api 사용 또는 로그인 요청시에만 체크
    suspend fun isCurrentIpFromChina(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://ipinfo.io/json")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext false
                    val json = JSONObject(response.body?.string() ?: "")
                    val country = json.optString("country", "")
                    return@withContext country.equals("CN", ignoreCase = true)
                }
            } catch (e: Exception) {
                false
            }
        }
        /**
         * lifecycleScope.launch {
         *     val isChina = isCurrentIpFromChina()
         *     if (isChina) {
         *         // 중국 IP 접속 시 처리
         *     }
         * }
         */
    }

    //네트워크 속도 체크 ( 텍스트 : 0.1~0.5M , 이미지 : 1~5 M)  --> NetworkCallback   요걸로 구현하는거 추천 ... 나중에 TEMP
    suspend fun measureNetworkSpeed(testUrl: String = "https://speed.hetzner.de/100MB.bin", sampleSizeBytes: Int = 1024 * 1024): Double {
        return withContext(Dispatchers.IO) {
            val client = OkHttpClient()
            val request = Request.Builder().url(testUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext 0.0

                val inputStream: InputStream = response.body?.byteStream() ?: return@withContext 0.0
                val buffer = ByteArray(8192)
                var bytesRead = 0
                var totalBytes = 0
                val startTime = System.currentTimeMillis()

                // sampleSizeBytes 만큼만 읽어서 속도 측정
                while (totalBytes < sampleSizeBytes && inputStream.read(buffer).also { bytesRead = it } != -1) {
                    totalBytes += bytesRead
                }
                val elapsedTime = System.currentTimeMillis() - startTime
                if (elapsedTime == 0L) return@withContext 0.0
                // Mbps(메가비트/초)로 변환
                val speedMbps = (totalBytes * 8.0 / 1_000_000) / (elapsedTime / 1000.0)
                speedMbps // 결과 리턴
            }
        }
        /**
         * lifecycleScope.launch {
         *     val speed = measureNetworkSpeed() // Mbps
         *     Toast.makeText(context, "측정된 속도: %.2f Mbps".format(speed), Toast.LENGTH_LONG).show()
         * }
         */
    }

    //최저 속도 미만일 경우 msg
    suspend fun isTooLateNatworkMsg( requiredMbps: Double , measureMbps:Double   ): String {

            if ( measureMbps < requiredMbps ) {
                return "현재 네트워크가 느립니다.\nWi-Fi 사용을 권장합니다."
            } else {
                return ""
            }
    }

}