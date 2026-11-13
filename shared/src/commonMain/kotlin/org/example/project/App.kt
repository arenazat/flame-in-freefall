package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.ui.MainAdaptiveScreen
import org.example.project.ui.theme.FlameFallTheme
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        FlameFallTheme {
            MainAdaptiveScreen()
        }
    }
}