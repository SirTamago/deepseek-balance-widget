# CLAUDE.md

DeepSeek 余额小组件 —— Android 项目技术文档（供 AI 工具/开发者参考）。

## 项目一句话

宿主 App（设置页）+ 2×1 Glance 桌面小组件，显示 DeepSeek API 人民币余额。当前为 v1 原版功能（多 Key、自动/手动刷新），图标为官方黑白鲸鱼。

## 构建环境（WSL2 Ubuntu 24.04，必须照做）

- **JDK 17**：`~/jdk17`。非交互 shell 不会自动加载 JAVA_HOME，每次构建先：
  ```bash
  export JAVA_HOME=~/jdk17 && export PATH=$JAVA_HOME/bin:$PATH
  ```
- **Android SDK**：`~/Android/Sdk`（platform-tools / platforms android-35,36 / build-tools 36.1.0）。adb 用 `export PATH=$HOME/Android/Sdk/platform-tools:$PATH`
- **网络镜像**（dl.google.com 不可达，必须用镜像）：
  - Gradle 发行版：`gradle/wrapper/gradle-wrapper.properties` → `mirrors.cloud.tencent.com/AndroidSDK/`
  - 依赖仓库：`settings.gradle.kts` → `maven.aliyun.com`（google / central / plugin 全部走阿里云）
  - 不要改回官方源，否则构建会卡死在下载
- **构建**：`./gradlew assembleDebug`（产物 `app/build/outputs/apk/debug/app-debug.apk`）

## 技术栈

Kotlin 2.1.0 · AGP 8.7.3 · Gradle 8.9 · compileSdk/targetSdk 35 · minSdk 26
Compose BOM 2024.12.01 (M3) · Glance 1.1.1 · WorkManager 2.10.0 · Retrofit 2.11.0 + Moshi + OkHttp 4.12.0 · DataStore 1.1.1 · security-crypto 1.1.0-alpha06

## 架构（手动服务定位，无 DI）

`DeepSeekBalanceApp`（Application）持有单例：

| 类 | 职责 |
|----|------|
| `data/local/KeyStorage` | EncryptedSharedPreferences；`keysFlow`/`selectedKeyIdFlow`（callbackFlow） |
| `data/local/BalanceCache` | DataStore 存 `CachedBalance(cnyBalance, updatedAt)` JSON（Moshi） |
| `data/local/SettingsStore` | DataStore 存刷新间隔 |
| `data/repository/BalanceRepository` | `refresh(): Result<Unit>`：选中 key → 拉取 → 写缓存 |
| `worker/BalanceSyncWorker` | 周期同步：成功 → `BalanceWidget().updateAll()`；IOException→retry，其余→failure |
| `widget/BalanceWidget` | Glance 渲染；`RefreshCallback` 点击刷新；`updateAll` 扩展 |
| `ui/SettingsViewModel` | 状态 + `refreshNow()`（WorkManager 一次性任务） |

**刷新链路**：手动刷新 = WorkManager 一次性任务 → worker → repository → cache → updateAll；切换/添加/删除 Key 也触发同一路径。

## 关键 API 细节（Glance 1.1.1 真实 API，已用 javap 验证，勿凭印象改）

- `ActionCallback` / `actionRunCallback`：`androidx.glance.appwidget.action`
- `clickable` / `ActionParameters`：`androidx.glance.action`
- `ColorProvider(day =, night =)`：`androidx.glance.color`
- `cornerRadius`：`androidx.glance.appwidget`
- `defaultWeight()`（没有 `weight(1f)`）
- **没有 `updateAll`**：自行实现 `GlanceAppWidgetManager(context).getGlanceIds(javaClass)` 循环 `update()`
- `ActionCallback.onAction` 在 `goAsync` 协程中运行（`Dispatchers.Default`，**无超时**）→ 回调里可直接做网络请求（已验证有效）
- **Moshi**：必须注册 `KotlinJsonAdapterFactory`；**不要**用 `@JsonClass(generateAdapter = true)`（项目未配置 KSP，会编译失败）

## 版本机制（重要）

`app/build.gradle.kts` 顶部：每次构建读 `version.properties` → `+1` 写回 → `versionCode = N`、`versionName = "1.0.N"`；`versions.txt` 追加记录。两文件均已 gitignore（本地记录）。

- 指定版本号：临时改 build.gradle.kts 的 `versionCode`/`versionName` 两行，构建后恢复
- 当前计数在 4（下一构建 = 1.0.5）

## 部署到手机（无线 adb + vivo）

- `adb devices` 经 mDNS 自动发现（`adb-xxx._adb-tls-connect._tcp`），无需 Windows
- **vivo 安装前提**：开发者选项 → 关闭「通过USB验证应用」，否则 `INSTALL_FAILED_USER_REJECTED`
- 降级安装：`adb install -r -d`
- 验证：`adb shell dumpsys package com.deepseek.balance | grep versionName`

## 已知问题 / 历史决策

- **vivo 等机型会延迟/拦截 WorkManager 后台一次性任务** → 曾导致小组件点击刷新「点了没反应」。已验证的解法：`ActionCallback` 内直连网络（goAsync 无超时，3 秒内完成）。当前 v1 仍是 WorkManager 路径（用户回滚后暂未合回）
- 小组件透明度/样式功能、直连刷新版均已**回滚**，当前代码 = v1 原版 + 黑白鲸鱼图标
- 版本历史：1.0.0(原始 v1) → 1.0.1(回滚+版本机制) → 1.0.2(透明度/刷新按钮，已弃) → 1.0.4(黑白鲸鱼图标，当前)

## 发布

```bash
gh release create v1.0.x app/build/outputs/apk/debug/app-debug.apk --title "..." --notes "..."
```

远程：`github.com/SirTamago/deepseek-balance-widget`（main 分支；当前 HEAD 对应已发布的 v1.0.4）。
