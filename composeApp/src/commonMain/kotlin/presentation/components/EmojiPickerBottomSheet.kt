package presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val EMOJI_CATEGORIES = listOf(
    "Recent" to emptyList<String>(),
    "Smileys" to (0x1F600..0x1F64F).map { String(Character.toChars(it)) },
    "Animals" to (0x1F400..0x1F4FF).map { String(Character.toChars(it)) },
    "Food" to (0x1F32D..0x1F37F).map { String(Character.toChars(it)) },
    "Activities" to (0x1F3C0..0x1F3FF).map { String(Character.toChars(it)) },
    "Travel" to (0x1F680..0x1F6FF).map { String(Character.toChars(it)) },
    "Objects" to (0x1F4E0..0x1F4FF).map { String(Character.toChars(it)) },
    "Symbols" to (0x1F300..0x1F5FF).map { String(Character.toChars(it)) }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerBottomSheet(
    recentEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val categories = remember(recentEmojis) {
        listOf(
            "Recent" to recentEmojis,
            *EMOJI_CATEGORIES.drop(1).toTypedArray()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp)
        ) {
            Text(
                text = "Select Emoji",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEachIndexed { index, (title, _) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, maxLines = 1) }
                    )
                }
            }

            val currentEmojis = categories[selectedTab].second

            if (currentEmojis.isEmpty() && selectedTab == 0) {
                Text(
                    text = "No recent emojis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentEmojis) { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier
                                .clickable { onEmojiSelected(emoji) }
                                .padding(4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}