package presentation.common

import androidx.compose.runtime.Composable

@Composable
expect fun rememberShareTextAction(): (String) -> Unit
