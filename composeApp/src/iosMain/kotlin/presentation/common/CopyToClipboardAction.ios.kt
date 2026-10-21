package presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIPasteboard

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberCopyToClipboardAction(): (String) -> Unit {
    return remember {
        { text ->
            UIPasteboard.generalPasteboard.string = text
        }
    }
}
