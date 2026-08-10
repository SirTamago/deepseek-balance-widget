package com.deepseek.balance.data.remote

import com.deepseek.balance.data.model.BalanceResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface DeepSeekApi {
    @GET("user/balance")
    suspend fun getBalance(
        @Header("Authorization") authorization: String,
    ): BalanceResponse
}
