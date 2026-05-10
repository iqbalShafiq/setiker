package com.setiker.app

import androidx.compose.runtime.Composable
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import presentation.navigation.AppNavigation
import presentation.theme.SetikerTheme
import util.buildAppImageLoader

@OptIn(ExperimentalCoilApi::class)
@Composable
fun App(
    onAddToWhatsApp: ((String, String) -> Unit)? = null
) {
    setSingletonImageLoaderFactory { context -> buildAppImageLoader(context) }
    SetikerTheme {
        AppNavigation(onAddToWhatsApp = onAddToWhatsApp)
    }
}
