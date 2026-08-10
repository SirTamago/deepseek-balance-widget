package com.deepseek.balance.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.deepseek.balance.data.model.ApiKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.UUID
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * API Key 存储: EncryptedSharedPreferences (Android Keystore 加密), 明文不出设备
 */
class KeyStorage(context: Context) {

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val keysAdapter =
        moshi.adapter<List<ApiKey>>(Types.newParameterizedType(List::class.java, ApiKey::class.java))

    /** key 列表, 监听变化实时推送 */
    val keysFlow: Flow<List<ApiKey>> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> trySend(getKeys()) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getKeys())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val selectedKeyIdFlow: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SELECTED) trySend(getSelectedKeyId())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(getSelectedKeyId())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    fun getKeys(): List<ApiKey> {
        val json = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching { keysAdapter.fromJson(json) }.getOrNull() ?: emptyList()
    }

    fun addKey(label: String, key: String): ApiKey {
        val newKey = ApiKey(id = UUID.randomUUID().toString(), label = label.trim(), key = key.trim())
        val keys = getKeys() + newKey
        prefs.edit().putString(KEY_LIST, keysAdapter.toJson(keys)).apply()
        // 第一个 key 自动设为当前选中
        if (getSelectedKeyId() == null) setSelectedKeyId(newKey.id)
        return newKey
    }

    fun deleteKey(id: String) {
        val keys = getKeys().filterNot { it.id == id }
        prefs.edit().putString(KEY_LIST, keysAdapter.toJson(keys)).apply()
        if (getSelectedKeyId() == id) {
            setSelectedKeyId(keys.firstOrNull()?.id)
        }
    }

    fun getSelectedKeyId(): String? = prefs.getString(KEY_SELECTED, null)

    fun setSelectedKeyId(id: String?) {
        prefs.edit().putString(KEY_SELECTED, id).apply()
    }

    /** 当前选中的 key; 未选中时回退到第一个 */
    fun getSelectedKey(): ApiKey? =
        getKeys().firstOrNull { it.id == getSelectedKeyId() }
            ?: getKeys().firstOrNull()

    companion object {
        private const val KEY_LIST = "key_list"
        private const val KEY_SELECTED = "selected_key_id"
    }
}
