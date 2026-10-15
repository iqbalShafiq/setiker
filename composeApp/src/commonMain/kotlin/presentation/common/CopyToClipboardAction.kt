package presentation.common

import androidx.compose.runtime.Composable

/**
 * Returns an action that copies a [String] to the platform clipboard.
 *
 * Compose's multiplatform `Clipboard` API cannot build the platform `ClipEntry` from common
 * code (see CMP-7624), so the entry construction lives in the platform actuals.
 */
@Composable
expect fun rememberCopyToClipboardAction(): (String) -> Unit
