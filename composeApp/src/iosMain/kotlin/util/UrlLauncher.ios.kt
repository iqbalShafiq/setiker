package util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberUrlLauncher(): (String) -> Unit {
    return remember {
        { url ->
            val nsUrl = NSURL.URLWithString(url) ?: return@remember
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
