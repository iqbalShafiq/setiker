package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Horizontal inset for full-screen auth / form scroll layouts. */
val FormScreenHorizontalPadding = 24.dp

/**
 * Scrollable centered column for full-screen forms (login, register, etc.).
 *
 * Uses [formScreenScrollInsets] for keyboard + navigation-bar handling and
 * [imeNestedScroll] so focused fields stay visible while the IME animates.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FormScreenScrollColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = FormScreenHorizontalPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .formScreenScrollInsets()
            .imeNestedScroll()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        content = content
    )
}

/** Keyboard- and navigation-bar-aware insets for [FormScreenScrollColumn]. */
expect fun Modifier.formScreenScrollInsets(): Modifier
