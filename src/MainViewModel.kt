package com.waveapp.tourcat

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.waveapp.tourcat.common.ComConstant

class MainViewModel : ViewModel() {
    private val _coin = MutableLiveData<Int>(0)
    val coin: LiveData<Int> = _coin

    /** Pref에서 코인값 읽어서 LiveData에 반영 (앱 진입, 화면 복귀시) */
    fun loadCoin(context: Context) {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        _coin.value = prefs.getInt(ComConstant.PREF_KEYS.VALID_CREDIT, 0)
    }

    /** LiveData의 코인값을 Pref에 저장 (코인 사용 등) */
    fun saveCoin(context: Context) {
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(ComConstant.PREF_KEYS.VALID_CREDIT, _coin.value ?: 0).apply()
    }

    /** 서버 등에서 새로운 코인값을 받은 경우: LiveData+Pref 동시 저장 */
    fun setCoin(value: Int, context: Context) {
        _coin.value = value
        val prefs = context.getSharedPreferences(ComConstant.PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(ComConstant.PREF_KEYS.VALID_CREDIT, value).apply()
    }
}
