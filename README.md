# DeepSeek 余额小组件

Android 桌面小组件，实时显示 DeepSeek API 账户余额（人民币）。宿主 App 提供设置页，小组件 2×1 可拉伸。

> 完整开发/构建文档见 [CLAUDE.md](CLAUDE.md)（AI 工具可直接参考）。

## 功能

- **桌面小组件**（Glance，2×1 可拉伸，Material Design）：
  - 显示人民币余额 + 上次更新时间
  - 点击小组件主体 → 立即刷新余额
  - 点击齿轮 → 打开设置页
  - 明暗双主题适配
- **设置页**（Compose Material 3）：
  - API Key 列表（标签 + 加密存储）
  - 选择小组件显示哪个 Key 的余额（切换后自动刷新）
  - 自动刷新间隔：15 分钟 / 30 分钟 / 1 小时 / 6 小时 / 24 小时
  - 手动刷新按钮

## 安装

从 GitHub Releases 下载 APK 直接安装：
https://github.com/SirTamago/deepseek-balance-widget/releases

- 当前版本：**v1.0.4**（黑白鲸鱼图标，v1 原版功能）
- debug 签名，可覆盖安装；首次使用需添加 API Key（`sk-...`）

## 技术方案

| 模块 | 方案 |
|------|------|
| 小组件 | Jetpack Glance 1.1.1（Compose 风格） |
| UI | Compose Material 3（BOM 2024.12.01） |
| 数据同步 | WorkManager 周期任务（后台）+ 手动直连刷新 |
| 缓存 | DataStore Preferences（余额 JSON + 设置） |
| Key 存储 | EncryptedSharedPreferences（Android Keystore 加密） |
| 网络 | Retrofit 2.11 + Moshi + OkHttp |
| 余额接口 | `GET https://api.deepseek.com/user/balance`（`Authorization: Bearer <API_KEY>`） |

## 项目结构

```
app/src/main/java/com/deepseek/balance/
├── data/
│   ├── local/          # KeyStorage(加密)、BalanceCache、SettingsStore
│   ├── model/          # ApiKey、BalanceResponse
│   ├── remote/         # DeepSeekApi (Retrofit)
│   └── repository/     # BalanceRepository: 拉取 → 写缓存
├── ui/
│   ├── MainActivity.kt
│   ├── SettingsViewModel.kt
│   ├── screen/         # SettingsScreen (Compose M3)
│   └── theme/
├── widget/             # BalanceWidget (Glance) + RefreshCallback
└── worker/             # BalanceSyncWorker (周期同步)
```

## 开发

```bash
export JAVA_HOME=~/jdk17 && export PATH=$JAVA_HOME/bin:$PATH
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

版本号每次构建自动递增（`version.properties`，本地记录，不入库）。详见 CLAUDE.md。

## 安全说明

- API Key 通过 Android Keystore 加密存储，仅本机使用
- `.gitignore` 已排除 `local.properties`、签名文件、构建产物、版本记录
- 仓库中不包含任何真实密钥
