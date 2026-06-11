package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import presentation.components.scaffoldBottomBarScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.components.FormScreenHorizontalPadding

/**
 * Scroll body for [AuthFormScaffold].
 *
 * Uses [presentation.components.scaffoldBottomBarScroll] — see that modifier for
 * why [presentation.components.keyboardAwareInsets] must not be applied here.
 */
@Composable
internal fun AuthFormScrollColumn(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = FormScreenHorizontalPadding,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .scaffoldBottomBarScroll(rememberScrollState())
            .padding(horizontal = horizontalPadding)
            .padding(top = 8.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        content = content
    )
}
