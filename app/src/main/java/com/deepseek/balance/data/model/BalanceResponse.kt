package com.deepseek.balance.data.model

import com.squareup.moshi.Json

/**
 * GET https://api.deepseek.com/user/balance 响应
 * 参考官方文档: balance_infos 为各币种余额数组
 */
data class BalanceResponse(
    @Json(name = "is_available") val isAvailable: Boolean,
    @Json(name = "balance_infos") val balanceInfos: List<BalanceInfo>,
) {
    data class BalanceInfo(
        @Json(name = "currency") val currency: String,
        @Json(name = "total_balance") val totalBalance: String,
        @Json(name = "granted_balance") val grantedBalance: String,
        @Json(name = "topped_up_balance") val toppedUpBalance: String,
    )
}
