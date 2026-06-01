package util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberUrlLauncher(): (String) -> Unit
