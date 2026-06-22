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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import data.remote.ExploreSort
import org.jetbrains.compose.resources.stringResource
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.explore_sort_downloads
import setiker.composeapp.generated.resources.explore_sort_likes
import setiker.composeapp.generated.resources.explore_sort_popular
import setiker.composeapp.generated.resources.explore_sort_recent
import setiker.composeapp.generated.resources.explore_sort_saves
import setiker.composeapp.generated.resources.explore_sort_title

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreSortBottomSheet(
    currentSort: ExploreSort,
    onSortSelected: (ExploreSort) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = neubrutalScreenBackground(),
        scrimColor = Color.Black.copy(alpha = 0.45f),
        contentWindowInsets = { zeroBottomSheetWindowInsets() }
    ) {
        BottomSheetScrollColumn {
            Text(
                text = stringResource(Res.string.explore_sort_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExploreSort.entries.forEach { option ->
                    BottomSheetOptionRow(
                        label = option.label(),
                        selected = option == currentSort,
                        onClick = { onSortSelected(option); onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExploreSort.label(): String = when (this) {
    ExploreSort.RECENT -> stringResource(Res.string.explore_sort_recent)
    ExploreSort.POPULAR -> stringResource(Res.string.explore_sort_popular)
    ExploreSort.DOWNLOADS -> stringResource(Res.string.explore_sort_downloads)
    ExploreSort.LIKES -> stringResource(Res.string.explore_sort_likes)
    ExploreSort.SAVES -> stringResource(Res.string.explore_sort_saves)
}
