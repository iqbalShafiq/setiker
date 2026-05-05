package com.setiker.app

import androidx.compose.runtime.Composable
import di.appModule
import di.platformModule
import org.koin.compose.KoinApplication
import presentation.navigation.AppNavigation
import presentation.theme.SetikerTheme

@Composable
fun App() {
    KoinApplication(application = {
        modules(platformModule() + appModule)
    }) {
        SetikerTheme {
            AppNavigation()
        }
    }
}
