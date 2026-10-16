package presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Applies [keyboardAwareInsets], [imeNestedScrollCompat], and [verticalScroll] in the
 * recommended order for text-input screens.
 */
@Composable
fun Modifier.keyboardAwareScroll(scrollState: ScrollState): Modifier =
    keyboardAwareInsets()
        .imeNestedScrollCompat()
        .verticalScroll(scrollState)

/**
 * Scroll/list modifiers for scaffold bodies that already include [PackBottomBar].
 *
 * The bar applies [androidx.compose.foundation.layout.imePadding]; content must not
 * add [keyboardAwareInsets] or the form is clipped and a gap appears above the bar.
 */
@Composable
fun Modifier.scaffoldBottomBarScroll(scrollState: ScrollState): Modifier =
    imeNestedScrollCompat()
        .verticalScroll(scrollState)

@Composable
fun Modifier.scaffoldBottomBarLazyList(): Modifier =
    imeNestedScrollCompat()

@Composable
fun ScaffoldBottomBarLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier.scaffoldBottomBarLazyList(),
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}
