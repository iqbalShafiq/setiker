package presentation.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.Modifier

actual fun Modifier.keyboardAwareInsets(): Modifier =
    navigationBarsPadding()
        .imePadding()
