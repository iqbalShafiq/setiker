package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.SortOrder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.sort_by
import setiker.composeapp.generated.resources.sort_name_asc
import setiker.composeapp.generated.resources.sort_name_desc
import setiker.composeapp.generated.resources.sort_newest
import setiker.composeapp.generated.resources.sort_oldest
import setiker.composeapp.generated.resources.sort_stickers_asc
import setiker.composeapp.generated.resources.sort_stickers_desc

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
        containerColor = neubrutalCardSurface(),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(Res.string.sort_by),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)) {
                options.forEach { option ->
                    val isSelected = option == currentSort
                    SortOptionItem(
                        option = option,
                        isSelected = isSelected,
                        onClick = { onSortSelected(option); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SortOptionItem(
    option: SortOrder,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        androidx.compose.ui.graphics.Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeubrutalSmallRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(
                when (option) {
                    SortOrder.NAME_ASC -> Res.string.sort_name_asc
                    SortOrder.NAME_DESC -> Res.string.sort_name_desc
                    SortOrder.STICKER_COUNT_ASC -> Res.string.sort_stickers_asc
                    SortOrder.STICKER_COUNT_DESC -> Res.string.sort_stickers_desc
                    SortOrder.NEWEST -> Res.string.sort_newest
                    SortOrder.OLDEST -> Res.string.sort_oldest
                }
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = neubrutalOnSurface(),
            modifier = Modifier.weight(1f)
        )

        if (isSelected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
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
