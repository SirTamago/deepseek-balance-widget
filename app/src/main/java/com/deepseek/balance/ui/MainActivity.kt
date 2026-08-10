package com.deepseek.balance.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.deepseek.balance.DeepSeekBalanceApp
import com.deepseek.balance.ui.screen.SettingsScreen
import com.deepseek.balance.ui.theme.DeepSeekBalanceTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as DeepSeekBalanceApp
        setContent {
            DeepSeekBalanceTheme {
                val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
                SettingsScreen(viewModel)
            }
        }
    }
}
