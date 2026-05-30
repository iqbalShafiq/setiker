package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow

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

    val bg = neubrutalScreenBackground()
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val onSurface = neubrutalOnSurface()
    val muted = neubrutalMutedOnSurface()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = bg,
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
        ) {
            Text(
                text = "Select Emoji",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                ),
                color = onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                containerColor = bg,
                contentColor = AccentCoral
            ) {
                categories.forEachIndexed { index, (title, _) ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selectedTab == index) AccentCoral else muted
                            )
                        }
                    )
                }
            }

            val currentEmojis = categories[selectedTab].second

            if (currentEmojis.isEmpty() && selectedTab == 0) {
                Text(
                    text = "No recent emojis",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(8),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentEmojis) { emoji ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(surface)
                                .neubrutalBorderWithGloss(
                                    color = border,
                                    width = 2.dp,
                                    cornerRadius = 10.dp,
                                    highlightColor = neubrutalGlossyHighlightColor()
                                )
                                .clickable { onEmojiSelected(emoji) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Previews

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun EmojiPickerBottomSheetPreview() {
    MaterialTheme {
        EmojiPickerBottomSheet(
            recentEmojis = listOf("😂", "🐱", "❤️", "🔥", "👍"),
            onEmojiSelected = {},
            onDismiss = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun EmojiPickerBottomSheetEmptyRecentPreview() {
    MaterialTheme {
        EmojiPickerBottomSheet(
            recentEmojis = emptyList(),
            onEmojiSelected = {},
            onDismiss = {}
        )
    }
}
