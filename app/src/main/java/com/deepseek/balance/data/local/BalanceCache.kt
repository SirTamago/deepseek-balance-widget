package com.deepseek.balance.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.balanceDataStore by preferencesDataStore(name = "balance_cache")

/**
 * 余额缓存 (DataStore), 设置页与小组件共享同一份数据
 */
class BalanceCache(private val context: Context) {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(CachedBalance::class.java)

    data class CachedBalance(
        /** 人民币余额, 原始字符串 (如 "128.889") */
        val cnyBalance: String,
        /** 更新时间戳 (epoch millis) */
        val updatedAt: Long,
    )

    val balance: Flow<CachedBalance?> = context.balanceDataStore.data.map { prefs ->
        val json = prefs[KEY_BALANCE] ?: return@map null
        runCatching { adapter.fromJson(json) }.getOrNull()
    }

    suspend fun save(cnyBalance: String) {
        val json = adapter.toJson(
            CachedBalance(cnyBalance = cnyBalance, updatedAt = System.currentTimeMillis())
        )
        context.balanceDataStore.edit { it[KEY_BALANCE] = json }
    }

    companion object {
        private val KEY_BALANCE = stringPreferencesKey("cached_balance")
    }
}
