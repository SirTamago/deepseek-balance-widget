package com.deepseek.balance

import android.app.Application
import androidx.work.WorkManager
import com.deepseek.balance.data.local.BalanceCache
import com.deepseek.balance.data.local.KeyStorage
import com.deepseek.balance.data.local.SettingsStore
import com.deepseek.balance.data.remote.DeepSeekApi
import com.deepseek.balance.data.repository.BalanceRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * 应用入口: 手动服务定位 (项目小, 无需 DI 框架)
 */
class DeepSeekBalanceApp : Application() {

    val keyStorage: KeyStorage by lazy { KeyStorage(this) }
    val balanceCache: BalanceCache by lazy { BalanceCache(this) }
    val settingsStore: SettingsStore by lazy { SettingsStore(this) }

    val balanceRepository: BalanceRepository by lazy {
        BalanceRepository(keyStorage, balanceCache, deepSeekApi)
    }

    val workManager: WorkManager by lazy { WorkManager.getInstance(this) }

    private val deepSeekApi: DeepSeekApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
        Retrofit.Builder()
            .baseUrl("https://api.deepseek.com/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(DeepSeekApi::class.java)
    }
}
