package com.deepseek.balance.data.model

/**
 * 一个 API Key 及其用户自定义标签
 */
data class ApiKey(
    val id: String,
    val label: String,
    val key: String,
) {
    /** 列表中展示的遮罩值, 例如 sk-abc••••1234 */
    fun maskedKey(): String {
        if (key.length <= 10) return key.take(4) + "••••"
        return key.take(6) + "••••" + key.takeLast(4)
    }
}
