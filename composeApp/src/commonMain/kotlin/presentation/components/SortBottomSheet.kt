package presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.SortOrder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.sort_by
import setiker.composeapp.generated.resources.sort_name_asc
import setiker.composeapp.generated.resources.sort_name_desc
import setiker.composeapp.generated.resources.sort_newest
import setiker.composeapp.generated.resources.sort_oldest
import setiker.composeapp.generated.resources.sort_stickers_asc
import setiker.composeapp.generated.resources.sort_stickers_desc
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = SortOrder.entries

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        contentWindowInsets = { zeroBottomSheetWindowInsets() }
    ) {
        BottomSheetScrollColumn {
            Text(
                text = stringResource(Res.string.sort_by),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    val isSelected = option == currentSort
                    BottomSheetOptionRow(
                        label = stringResource(option.toLabelRes()),
                        selected = isSelected,
                        onClick = { onSortSelected(option); onDismiss() }
                    )
                }
            }
        }
    }
}

private fun SortOrder.toLabelRes() = when (this) {
    SortOrder.NAME_ASC -> Res.string.sort_name_asc
    SortOrder.NAME_DESC -> Res.string.sort_name_desc
    SortOrder.STICKER_COUNT_ASC -> Res.string.sort_stickers_asc
    SortOrder.STICKER_COUNT_DESC -> Res.string.sort_stickers_desc
    SortOrder.NEWEST -> Res.string.sort_newest
    SortOrder.OLDEST -> Res.string.sort_oldest
}

// MARK: - Preview

@Preview
@Composable
private fun SortBottomSheetPreview() {
    MaterialTheme {
        SortBottomSheet(
            currentSort = SortOrder.NAME_ASC,
            onSortSelected = {},
            onDismiss = {}
        )
    }
}
