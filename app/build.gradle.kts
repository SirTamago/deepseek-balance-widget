plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// ── 版本号自动递增 ──
val versionFile = rootProject.file("version.properties")
val prevNum = if (versionFile.exists())
    versionFile.readText().trim().substringAfter("versionCode=").substringBefore("\n").toIntOrNull() ?: 0
else 0
val buildNum = prevNum + 1
versionFile.writeText("versionCode=$buildNum\n")

// 版本日志 (无 JDK 依赖, 仅记 build 编号, 由 shell 脚本补时间戳)
val versionLog = rootProject.file("versions.txt")
versionLog.appendText("$buildNum\n")

android {
    namespace = "com.deepseek.balance"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deepseek.balance"
        minSdk = 26
        targetSdk = 35
        versionCode = buildNum
        versionName = "1.0.$buildNum"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose + Material 3
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    // Glance 小组件
    implementation(libs.glance)

    // WorkManager 周期同步
    implementation(libs.work.runtime)

    // 网络
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    // 存储
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)
}
