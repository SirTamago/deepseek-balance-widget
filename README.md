# DeepSeek 余额小组件

Android 桌面小组件，实时显示 DeepSeek API 账户余额。

## 功能

- 桌面小组件显示 DeepSeek 账户余额（¥ 与 USD）
- 定时自动刷新（WorkManager 周期同步）
- API Key 仅保存在手机本地，不出设备、不进仓库

## 技术方案

| 模块 | 方案 |
|------|------|
| 小组件 | Jetpack Glance（Compose 风格） |
| 数据同步 | WorkManager 周期任务 + DataStore 缓存 |
| 网络请求 | Retrofit / OkHttp |
| 余额接口 | `GET https://api.deepseek.com/user/balance`（`Authorization: Bearer <API_KEY>`） |

## 项目结构（规划）

```
app/
├── src/main/java/.../
│   ├── data/          # DeepSeek API 客户端、余额数据模型、DataStore 缓存
│   ├── widget/        # Glance 小组件定义
│   ├── worker/        # WorkManager 周期同步任务
│   └── ui/            # 设置页（输入 API Key）
└── src/main/AndroidManifest.xml
```

## 开发计划

- [ ] Gradle 工程骨架 + Glance 依赖
- [ ] 余额 API 客户端 + 数据模型
- [ ] 小组件渲染 + 定时刷新
- [ ] 设置页（API Key 输入与加密存储）
- [ ] 打包安装调试

## 安全说明

- API Key 通过 Android Keystore 加密存储，仅本机使用
- `.gitignore` 已排除 `local.properties`、签名文件等敏感内容
- 仓库中不包含任何真实密钥
