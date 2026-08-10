package com.deepseek.balance.data.repository

import com.deepseek.balance.data.local.BalanceCache
import com.deepseek.balance.data.local.KeyStorage
import com.deepseek.balance.data.remote.DeepSeekApi
import retrofit2.HttpException

/**
 * 协调: 选中 key → 拉取余额 → 写缓存
 */
class BalanceRepository(
    private val keyStorage: KeyStorage,
    private val balanceCache: BalanceCache,
    private val api: DeepSeekApi,
) {

    /** 拉取当前选中 key 的余额并写入缓存; 失败时返回 cause */
    suspend fun refresh(): Result<Unit> = try {
        val apiKey = keyStorage.getSelectedKey()
            ?: return Result.failure(IllegalStateException("尚未添加 API Key"))
        val response = api.getBalance("Bearer ${apiKey.key}")
        val cny = response.balanceInfos.firstOrNull { it.currency == "CNY" }
            ?: response.balanceInfos.firstOrNull()
            ?: return Result.failure(IllegalStateException("响应中没有余额数据"))
        balanceCache.save(cny.totalBalance)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** 把异常翻译成用户可读的错误信息 */
    fun friendlyMessage(e: Throwable?): String = when (e) {
        is HttpException -> when (e.code()) {
            401 -> "API Key 无效 (401)"
            402 -> "余额不足 (402)"
            429 -> "请求过于频繁 (429)"
            else -> "请求失败 (HTTP ${e.code()})"
        }
        is java.io.IOException -> "网络连接失败，请检查网络"
        is IllegalStateException -> e.message ?: "未知错误"
        else -> e?.message ?: "未知错误"
    }
}
