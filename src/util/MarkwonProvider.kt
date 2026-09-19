// MarkwonProvider.kt
package com.waveapp.tourcat.util

import android.content.Context
import android.graphics.Typeface
import androidx.core.content.ContextCompat
import io.noties.markwon.Markwon
import io.noties.markwon.AbstractMarkwonPlugin
import com.waveapp.tourcat.R
import io.noties.markwon.core.MarkwonTheme

//object MarkwonProvider {
//    @Volatile
//    private var markwonInstance: Markwon? = null
//
//    fun get(context: Context): Markwon {
//        return markwonInstance ?: synchronized(this) {
//            markwonInstance ?: Markwon.builder(context)
//                .usePlugin(object : AbstractMarkwonPlugin() {
//                    override fun configureTheme(builder: MarkwonTheme.Builder) {
//                        builder.textColor(ContextCompat.getColor(context, R.color.md_heading))
//                            .headingTextSizeMultipliers(floatArrayOf(2.0f, 1.5f, 1.2f, 1.0f, 1.0f, 1.0f))
//                            .headingTypeface(Typeface.DEFAULT_BOLD)
//                    }
//                })
//                .build()
//                .also { markwonInstance = it }
//        }
//    }
//}