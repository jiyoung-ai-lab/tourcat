package com.waveapp.tourcat.design

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import kotlin.math.max
import kotlin.math.min

object FontScaleManager {

    fun wrap(base: Context, scale: Double): Context {
        val safe = min(1.8f, max(0.8f, scale.toFloat())) // 가드
        val conf = Configuration(base.resources.configuration)
        conf.fontScale = safe
        val res = base.createConfigurationContext(conf)
        return object : ContextWrapper(res) {}
    }
}