package presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun ConfigureSystemBars(
    statusBarColor: Color,
    navigationBarColor: Color,
    darkTheme: Boolean
)
