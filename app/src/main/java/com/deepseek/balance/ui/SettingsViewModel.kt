package com.deepseek.balance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.deepseek.balance.DeepSeekBalanceApp
import com.deepseek.balance.data.local.BalanceCache
import com.deepseek.balance.data.local.KeyStorage
import com.deepseek.balance.data.local.SettingsStore
import com.deepseek.balance.data.model.ApiKey
import com.deepseek.balance.data.repository.BalanceRepository
import com.deepseek.balance.worker.BalanceSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 手动刷新状态 */
enum class RefreshState { Idle, Refreshing, Success, Error }

class SettingsViewModel(
    private val keyStorage: KeyStorage,
    private val balanceCache: BalanceCache,
    private val settingsStore: SettingsStore,
    private val repository: BalanceRepository,
    private val workManager: WorkManager,
) : ViewModel() {

    val keys: StateFlow<List<ApiKey>> = keyStorage.keysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), keyStorage.getKeys())

    val selectedKeyId: StateFlow<String?> = keyStorage.selectedKeyIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), keyStorage.getSelectedKeyId())

    val refreshIntervalMinutes: StateFlow<Long> = settingsStore.refreshIntervalMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsStore.DEFAULT_INTERVAL_MINUTES)

    val cachedBalance: StateFlow<BalanceCache.CachedBalance?> = balanceCache.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _refreshState = MutableStateFlow(RefreshState.Idle)
    val refreshState: StateFlow<RefreshState> = _refreshState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch {
            schedulePeriodic(settingsStore.getRefreshIntervalMinutes())
        }
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(WORK_REFRESH_NOW)
                .collectLatest { infos ->
                    val info = infos.firstOrNull()
                    when (info?.state) {
                        WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                            _refreshState.value = RefreshState.Refreshing
                            _errorMessage.value = null
                        }
                        WorkInfo.State.SUCCEEDED -> {
                            _refreshState.value = RefreshState.Success
                            _errorMessage.value = null
                        }
                        WorkInfo.State.FAILED -> {
                            _refreshState.value = RefreshState.Error
                            _errorMessage.value = info.outputData.getString(BalanceSyncWorker.KEY_ERROR)
                                ?: "刷新失败"
                        }
                        else -> Unit
                    }
                }
        }
    }

    fun addKey(label: String, key: String) {
        if (label.isBlank() || key.isBlank()) return
        keyStorage.addKey(label, key)
        // 首次添加 key 时自动刷新余额
        refreshNow()
    }

    fun deleteKey(id: String) {
        val wasSelected = keyStorage.getSelectedKeyId() == id
        keyStorage.deleteKey(id)
        // 删除的是当前选中 key 则切换到另一个后刷新
        if (wasSelected) refreshNow()
    }

    fun selectKey(id: String) {
        keyStorage.setSelectedKeyId(id)
        // 切换 key 后立即刷新余额
        refreshNow()
    }

    fun setRefreshInterval(minutes: Long) {
        viewModelScope.launch {
            settingsStore.setRefreshIntervalMinutes(minutes)
            schedulePeriodic(minutes)
        }
    }

    fun refreshNow() {
        if (keyStorage.getKeys().isEmpty()) {
            _refreshState.value = RefreshState.Error
            _errorMessage.value = "请先添加 API Key"
            return
        }
        workManager.enqueueUniqueWork(
            WORK_REFRESH_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BalanceSyncWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }

    fun dismissError() {
        _errorMessage.value = null
        _refreshState.value = RefreshState.Idle
    }

    private fun schedulePeriodic(minutes: Long) {
        val request = PeriodicWorkRequestBuilder<BalanceSyncWorker>(
            minutes.coerceAtLeast(15), TimeUnit.MINUTES,
        ).build()
        workManager.enqueueUniquePeriodicWork(
            WORK_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    companion object {
        const val WORK_REFRESH_NOW = "refresh_now"
        private const val WORK_PERIODIC = "periodic_sync"

        fun factory(app: DeepSeekBalanceApp): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    keyStorage = app.keyStorage,
                    balanceCache = app.balanceCache,
                    settingsStore = app.settingsStore,
                    repository = app.balanceRepository,
                    workManager = app.workManager,
                )
            }
        }
    }
}
