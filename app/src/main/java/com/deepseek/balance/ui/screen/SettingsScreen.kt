package com.deepseek.balance.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.deepseek.balance.data.local.BalanceCache
import com.deepseek.balance.data.model.ApiKey
import com.deepseek.balance.ui.RefreshState
import com.deepseek.balance.ui.SettingsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val keys by viewModel.keys.collectAsState()
    val selectedKeyId by viewModel.selectedKeyId.collectAsState()
    val interval by viewModel.refreshIntervalMinutes.collectAsState()
    val balance by viewModel.cachedBalance.collectAsState()
    val refreshState by viewModel.refreshState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ApiKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DeepSeek余额") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("添加 Key") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── 余额总览 ──
            item { BalanceCard(balance, refreshState, viewModel::refreshNow) }

            errorMessage?.let { message ->
                item {
                    AssistChip(
                        onClick = viewModel::dismissError,
                        label = { Text("✕  $message") },
                    )
                }
            }

            // ── API Key 列表 ──
            item { SectionTitle("API Keys") }
            if (keys.isEmpty()) {
                item {
                    Text(
                        "还没有 API Key，点击右下角添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(keys, key = { it.id }) { key ->
                KeyCard(
                    key, selected = key.id == selectedKeyId,
                    onSelect = { viewModel.selectKey(key.id) },
                    onDelete = { pendingDelete = key },
                )
            }

            // ── 设置 ──
            item { SectionTitle("设置") }
            item { IntervalCard(interval, viewModel::setRefreshInterval) }
        }
    }

    if (showAddDialog) {
        AddKeyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, key -> viewModel.addKey(label, key); showAddDialog = false },
        )
    }
    pendingDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除 Key") },
            text = { Text("确定删除「${key.label}」吗？") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteKey(key.id); pendingDelete = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

// ── 余额总览卡片 ──

@Composable
private fun BalanceCard(balance: BalanceCache.CachedBalance?, refreshState: RefreshState, onRefresh: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("当前余额", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                balance?.let { formatCny(it.cnyBalance) } ?: "——",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("上次更新：${balance?.let { formatTime(it.updatedAt) } ?: "暂无数据"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRefresh, enabled = refreshState != RefreshState.Refreshing) {
                    if (refreshState == RefreshState.Refreshing) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (refreshState == RefreshState.Refreshing) "刷新中" else "立即刷新")
                }
            }
        }
    }
}

// ── Key 列表项 ──

@Composable
private fun KeyCard(key: ApiKey, selected: Boolean, onSelect: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onSelect)
            .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected, onClick = onSelect)
            Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(key.label.ifBlank { "未命名" }, style = MaterialTheme.typography.titleMedium)
                Text(key.maskedKey(), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── 刷新间隔 ──

@Composable
private fun IntervalCard(interval: Long, onSelect: (Long) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        var menuOpen by remember { mutableStateOf(false) }
        Box {
            Row(Modifier.fillMaxWidth().clickable { menuOpen = true }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("自动刷新间隔", style = MaterialTheme.typography.bodyLarge)
                Text(intervalLabel(interval), color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium)
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                INTERVAL_OPTIONS.forEach { (m, label) ->
                    DropdownMenuItem(text = { Text(label) },
                        trailingIcon = { if (m == interval) Text("✓", color = MaterialTheme.colorScheme.primary) },
                        onClick = { onSelect(m); menuOpen = false })
                }
            }
        }
    }
}

private val INTERVAL_OPTIONS = listOf(
    15L to "15 分钟", 30L to "30 分钟", 60L to "1 小时", 360L to "6 小时", 1440L to "24 小时",
)
private fun intervalLabel(minutes: Long) =
    INTERVAL_OPTIONS.firstOrNull { it.first == minutes }?.second ?: "$minutes 分钟"

// ── 添加 Key 对话框 ──

@Composable
private fun AddKeyDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var label by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加 API Key") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("标签（如：工作号）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(key, { key = it }, label = { Text("API Key（sk-...）") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(label.trim(), key.trim()) },
                enabled = label.isNotBlank() && key.isNotBlank()) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

private fun formatCny(raw: String): String = runCatching {
    val f = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    "¥" + f.format(raw.toBigDecimal())
}.getOrDefault(raw)

private fun formatTime(ts: Long) =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
