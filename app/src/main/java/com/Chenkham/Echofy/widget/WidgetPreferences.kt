package com.Chenkham.Echofy.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.Chenkham.Echofy.constants.WidgetBackgroundMode
import com.Chenkham.Echofy.constants.WidgetBackgroundModeKey
import com.Chenkham.Echofy.constants.WidgetCornerRadiusKey
import com.Chenkham.Echofy.constants.WidgetScrimOpacityKey
import com.Chenkham.Echofy.constants.WidgetShowProgressBarKey
import com.Chenkham.Echofy.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

object WidgetPreferences {

    @Volatile var cachedBackgroundMode: WidgetBackgroundMode = WidgetBackgroundMode.BLUR
    @Volatile var cachedScrimOpacity: Float = 0.32f
    @Volatile var cachedCornerRadius: Float = 24f
    @Volatile var cachedShowProgressBar: Boolean = true

    fun flow(context: Context): Flow<Preferences> = context.dataStore.data

    fun backgroundModeFlow(context: Context): Flow<WidgetBackgroundMode> =
        context.dataStore.data.map { prefs ->
            (prefs[WidgetBackgroundModeKey]
                ?.let { raw -> runCatching { WidgetBackgroundMode.valueOf(raw) }.getOrNull() }
                ?: WidgetBackgroundMode.BLUR).also { cachedBackgroundMode = it }
        }

    fun scrimOpacityFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[WidgetScrimOpacityKey] ?: 0.32f).also { v -> cachedScrimOpacity = v } }

    fun cornerRadiusFlow(context: Context): Flow<Float> =
        context.dataStore.data.map { (it[WidgetCornerRadiusKey] ?: 24f).also { v -> cachedCornerRadius = v } }

    fun showProgressBarFlow(context: Context): Flow<Boolean> =
        context.dataStore.data.map { (it[WidgetShowProgressBarKey] ?: true).also { v -> cachedShowProgressBar = v } }

    suspend fun getBackgroundMode(context: Context): WidgetBackgroundMode = runCatching {
        context.dataStore.data.first()[WidgetBackgroundModeKey]
            ?.let { raw -> runCatching { WidgetBackgroundMode.valueOf(raw) }.getOrNull() }
    }.getOrNull() ?: cachedBackgroundMode

    suspend fun getScrimOpacity(context: Context): Float = runCatching {
        context.dataStore.data.first()[WidgetScrimOpacityKey]
    }.getOrNull() ?: cachedScrimOpacity

    suspend fun getCornerRadius(context: Context): Float = runCatching {
        context.dataStore.data.first()[WidgetCornerRadiusKey]
    }.getOrNull() ?: cachedCornerRadius

    suspend fun getShowProgressBar(context: Context): Boolean = runCatching {
        context.dataStore.data.first()[WidgetShowProgressBarKey]
    }.getOrNull() ?: cachedShowProgressBar

    suspend fun setBackgroundMode(context: Context, mode: WidgetBackgroundMode) {
        cachedBackgroundMode = mode
        context.dataStore.edit { it[WidgetBackgroundModeKey] = mode.name }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setScrimOpacity(context: Context, value: Float) {
        cachedScrimOpacity = value
        context.dataStore.edit { it[WidgetScrimOpacityKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setCornerRadius(context: Context, value: Float) {
        cachedCornerRadius = value
        context.dataStore.edit { it[WidgetCornerRadiusKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }

    suspend fun setShowProgressBar(context: Context, value: Boolean) {
        cachedShowProgressBar = value
        context.dataStore.edit { it[WidgetShowProgressBarKey] = value }
        WidgetPreferencesSync.notifyChanged(context)
    }
}
