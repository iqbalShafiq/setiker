package com.setiker.app

import androidx.compose.runtime.Composable
import presentation.navigation.AppNavigation
import presentation.theme.SetikerTheme

@Composable
fun App() {
    SetikerTheme {
        AppNavigation()
    }
}
