package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalScrim
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalFill

/**
 * The sheet state every app sheet uses.
 *
 * Material's default allows a half-expanded state, and one sheet (the emoji picker) used it while
 * every other sheet skipped it. That made the same gesture produce two different heights. The
 * app's sheets are short, scrollable surfaces; they should open fully and never stop halfway.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppSheetState(): SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

/** The app's sheet shape: a softer top curve than the default Material sheet. */
val AppSheetShape: Shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)

/**
 * The sheet wrapper: one shape, one drag handle and the app's surface colour.
 *
 * The sheet's slide and scrim stay Material's, because they are platform behaviour and fighting
 * the system gesture is worse than accepting it. The chrome around the content is ours.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberAppSheetState(),
    shape: Shape = AppSheetShape,
    containerColor: Color = neubrutalScreenBackground(),
    contentColor: Color = neubrutalOnSurface(),
    scrimColor: Color = NeubrutalScrim,
    contentWindowInsets: @Composable () -> WindowInsets = { WindowInsets.navigationBars },
    dragHandle: @Composable (() -> Unit)? = { AppSheetDragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        scrimColor = scrimColor,
        contentWindowInsets = contentWindowInsets,
        dragHandle = dragHandle,
        content = content
    )
}

/** A two-tone neubrutal handle: a thick black pill with a coral core. */
@Composable
fun AppSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(6.dp)
                .clip(CircleShape)
                .background(neubrutalBorderColor()),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .neubrutalFill(color = AccentCoral, cornerRadius = 99.dp)
            )
        }
    }
}
