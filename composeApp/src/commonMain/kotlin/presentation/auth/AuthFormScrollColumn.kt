package presentation.auth

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
import presentation.components.FormScreenHorizontalPadding

/**
 * Scroll body for [AuthFormScaffold].
 *
 * Does **not** apply [presentation.components.keyboardAwareInsets] — the scaffold
 * already reserves space for [presentation.components.PackBottomBar], and the bar
 * handles IME insets. Extra imePadding on the content would double-count and clip
 * or push the form off-screen when the keyboard opens.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AuthFormScrollColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = FormScreenHorizontalPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imeNestedScroll()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        content = content
    )
}
