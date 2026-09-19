package com.waveapp.tourcat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import com.waveapp.tourcat.common.ComConstant
import com.waveapp.tourcat.connection.assetdata.ExchangeRateSyncWorker
import com.waveapp.tourcat.helper.MessageHelper
import com.waveapp.tourcat.util.EnvironmentUtil
import com.waveapp.tourcat.util.FileUtil
import com.waveapp.tourcat.util.LogUtil
import com.waveapp.tourcat.util.NetworkUtil

class TourCatApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. FIle Path 저장 (공용갤러리패스 / 앱내부 패스)
        ComConstant.PATH_ORIGINAL_IMG = FileUtil.getGalleryFolderPath(ComConstant.FOLDER_TOURCAT)
        ComConstant.PATH_HISTORY_IMG = FileUtil.getAppInternalFolderPath(this, ComConstant.FOLDER_THUMB)

        // 2. 스케줄 작업 호출 (환율 동기화 워커 등록)
        ExchangeRateSyncWorker.scheduleExchangeRateSync(this)

        createNotificationChannel()

        if (NetworkUtil.isNetworkConnected(this)) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    EnvironmentUtil.saveFcmToken(applicationContext, token)
                    LogUtil.d("푸시 토큰: $token", "FCM")

                } else {
                    LogUtil.w("Fetching FCM registration token failed", "FCMLog", task.exception?.toString() ?: "")
                    MessageHelper.showToastLong(this, "Google Firebase token failed.")
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "default"
            val name = "default"
            val descriptionText = "default description"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
