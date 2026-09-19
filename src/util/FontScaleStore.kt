package com.waveapp.tourcat.util


import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

object FontScaleStore {
    private val KEY_FONT_SCALE = doublePreferencesKey("font_scale") // 0.85 ~ 1.50 권장
    const val DEFAULT_SCALE = 1.0

    fun fontScaleFlow(context: Context): Flow<Double> =
        context.dataStore.data.map { it[KEY_FONT_SCALE] ?: DEFAULT_SCALE }

    suspend fun setFontScale(context: Context, value: Double) {
        context.dataStore.edit { prefs -> prefs[KEY_FONT_SCALE] = value }
    }
}