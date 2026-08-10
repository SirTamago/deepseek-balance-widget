package com.deepseek.balance.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * 用户设置 (DataStore Preferences): 自动刷新间隔
 */
class SettingsStore(private val context: Context) {

    /** 自动刷新间隔 (分钟) */
    val refreshIntervalMinutes: Flow<Long> =
        context.settingsDataStore.data.map { it[KEY_INTERVAL] ?: DEFAULT_INTERVAL_MINUTES }

    suspend fun getRefreshIntervalMinutes(): Long = refreshIntervalMinutes.first()

    suspend fun setRefreshIntervalMinutes(minutes: Long) {
        context.settingsDataStore.edit { it[KEY_INTERVAL] = minutes }
    }

    companion object {
        private val KEY_INTERVAL = longPreferencesKey("refresh_interval_minutes")
        const val DEFAULT_INTERVAL_MINUTES = 30L
    }
}
