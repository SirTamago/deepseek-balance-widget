package com.deepseek.balance.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.deepseek.balance.DeepSeekBalanceApp
import com.deepseek.balance.widget.BalanceWidget
import com.deepseek.balance.widget.updateAll
import java.io.IOException

/**
 * 余额同步任务:
 * 拉取余额 → 写缓存 → 刷新所有小组件实例
 *
 * 网络错误自动重试, 其余错误 (Key 无效等) 直接失败并携带错误信息
 */
class BalanceSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DeepSeekBalanceApp
        val result = app.balanceRepository.refresh()

        result.onSuccess {
            BalanceWidget().updateAll(applicationContext)
        }

        return when {
            result.isSuccess -> Result.success()
            result.exceptionOrNull() is IOException -> Result.retry()
            else -> {
                val message = app.balanceRepository.friendlyMessage(result.exceptionOrNull())
                Result.failure(workDataOf(KEY_ERROR to message))
            }
        }
    }

    companion object {
        const val KEY_ERROR = "error"
    }
}
