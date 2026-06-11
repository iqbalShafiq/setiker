package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Horizontal inset for full-screen auth / form scroll layouts. */
val FormScreenHorizontalPadding = 24.dp

/**
 * Scrollable centered column for full-screen forms (login, register, etc.).
 */
@Composable
fun FormScreenScrollColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = FormScreenHorizontalPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .keyboardAwareScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}
