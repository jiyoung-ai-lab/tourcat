package com.waveapp.tourcat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {
//    // 언어코드, 파라미터 등 다국적 데이터 예시
//    private val _currentLanguage = MutableLiveData<String>("ko")
//    val currentLanguage: LiveData<String> get() = _currentLanguage
//
//    fun setLanguage(langCode: String) {
//        _currentLanguage.value = langCode
//    }

    // 프래그먼트 간 파라미터 전달용 데이터
    private val _paramFromHome = MutableLiveData<String?>()
    val paramFromHome: LiveData<String?> get() = _paramFromHome

    fun setParamFromHome(param: String) {
        _paramFromHome.value = param
    }
}
