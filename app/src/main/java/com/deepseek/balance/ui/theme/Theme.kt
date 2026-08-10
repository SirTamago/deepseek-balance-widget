package com.deepseek.balance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// DeepSeek 品牌蓝
private val DeepSeekBlue = Color(0xFF4D6BFE)
private val DeepSeekBlueDark = Color(0xFFB9C6FF)

private val LightColors = lightColorScheme(
    primary = DeepSeekBlue,
    secondary = Color(0xFF6070A8),
)

private val DarkColors = darkColorScheme(
    primary = DeepSeekBlueDark,
    secondary = Color(0xFFBCC6EA),
)

@Composable
fun DeepSeekBalanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
