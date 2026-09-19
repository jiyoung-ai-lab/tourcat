package com.waveapp.tourcat.util

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogUtil {
    private const val DEFAULT_TAG = "tourcat"
    private const val IS_DEBUG = true // BuildConfig.DEBUG 권장

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private fun now(): String = dateFormat.format(Date())

    private fun formatMsg(gubun: String, msg: String?): String {
        val time = now()
        if (msg.isNullOrBlank()) return "[$time]"
        val content = if (gubun.isNotBlank()) "[$gubun] $msg" else msg
        return "[$time] $content"
    }

    /** ===== 공통 호출 (간단 호출용) ===== */
    fun d(msg: String?, tag: String = DEFAULT_TAG, gubun: String = "") {
        if (IS_DEBUG) Log.d(tag, formatMsg(gubun, msg))
    }

    fun i(msg: String?, tag: String = DEFAULT_TAG, gubun: String = "") {
        if (IS_DEBUG) Log.i(tag, formatMsg(gubun, msg))
    }

    fun w(msg: String?, tag: String = DEFAULT_TAG, gubun: String = "") {
        if (IS_DEBUG) Log.w(tag, formatMsg(gubun, msg))
    }

    fun e(msg: String?, tag: String? = DEFAULT_TAG, gubun: String = "", tr: Throwable? = null) {
        if (IS_DEBUG) Log.e(tag, formatMsg(gubun, msg), tr)
    }
    fun e(message: String, tag: String? = DEFAULT_TAG, tr: Throwable? = null) {
        if (tr != null) Log.e(tag, message, tr) else Log.e(tag, message)
    }
}
