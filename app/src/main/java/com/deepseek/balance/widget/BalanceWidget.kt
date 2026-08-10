package com.deepseek.balance.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.action.ActionParameters
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.deepseek.balance.DeepSeekBalanceApp
import com.deepseek.balance.R
import com.deepseek.balance.data.local.BalanceCache
import com.deepseek.balance.ui.MainActivity
import com.deepseek.balance.ui.SettingsViewModel
import com.deepseek.balance.worker.BalanceSyncWorker
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.first

class BalanceWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent(context) }
    }
}

@Composable
private fun WidgetContent(context: Context) {
    val app = context.applicationContext as DeepSeekBalanceApp

    val balance by produceState<BalanceCache.CachedBalance?>(initialValue = null) {
        value = app.balanceCache.balance.first()
    }
    val hasKeys by produceState(initialValue = false) {
        value = app.keyStorage.getKeys().isNotEmpty()
    }

    val mainAction = if (hasKeys) actionRunCallback<RefreshCallback>()
                     else actionStartActivity<MainActivity>()
    val openSettings = actionStartActivity<MainActivity>()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetBg)
            .cornerRadius(16.dp)
            .clickable(mainAction)
            .padding(12.dp),
    ) {
        Column(GlanceModifier.fillMaxSize()) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                Text(
                    text = "DeepSeek 余额",
                    style = TextStyle(fontSize = 11.sp, color = TextSecondary),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Image(
                    provider = ImageProvider(R.drawable.ic_settings),
                    contentDescription = "打开设置",
                    modifier = GlanceModifier
                        .size(16.dp)
                        .clickable(openSettings),
                )
            }

            Spacer(GlanceModifier.defaultWeight())

            val cached = balance
            if (cached == null) {
                Text(
                    text = if (hasKeys) "获取余额中…" else "未设置 Key，点此添加",
                    style = TextStyle(fontSize = 13.sp, color = TextSecondary),
                )
            } else {
                Text(
                    text = formatCny(cached.cnyBalance),
                    style = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Accent),
                )
                Text(
                    text = "更新于 ${formatTime(cached.updatedAt)}",
                    style = TextStyle(fontSize = 9.sp, color = TextSecondary),
                )
            }
        }
    }
}

class RefreshCallback : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            SettingsViewModel.WORK_REFRESH_NOW,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<BalanceSyncWorker>().build(),
        )
    }
}

suspend fun GlanceAppWidget.updateAll(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    manager.getGlanceIds(javaClass).forEach { update(context, it) }
}

// ── 明暗双主题颜色 ──

private val WidgetBg = ColorProvider(
    day = Color(0xFFF1F3FF),
    night = Color(0xFF1C1D24),
)
private val TextSecondary = ColorProvider(
    day = Color(0xFF49454F),
    night = Color(0xFFCAC4D0),
)
private val Accent = ColorProvider(
    day = Color(0xFF4D6BFE),
    night = Color(0xFFB9C6FF),
)

// ── 格式化 ──

private fun formatCny(raw: String): String = runCatching {
    val f = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }
    "¥" + f.format(raw.toBigDecimal())
}.getOrDefault(raw)

private fun formatTime(ts: Long) =
    SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
