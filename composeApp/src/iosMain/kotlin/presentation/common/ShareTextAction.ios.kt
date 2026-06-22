package presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberShareTextAction(): (String) -> Unit {
    return remember {
        { text ->
            val rootController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return@remember
            val activity = UIActivityViewController(
                activityItems = listOf(text),
                applicationActivities = null
            )
            rootController.presentViewController(activity, animated = true, completion = null)
        }
    }
}
