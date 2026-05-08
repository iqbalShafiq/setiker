package presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.stringResource
import domain.model.Sticker
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.EmojiPickerBottomSheet
import presentation.components.LoadingIndicator
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.accessibility_text
import setiker.composeapp.generated.resources.accessibility_text_example
import setiker.composeapp.generated.resources.accessibility_text_placeholder
import setiker.composeapp.generated.resources.add
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.crop
import setiker.composeapp.generated.resources.edit_sticker_title
import setiker.composeapp.generated.resources.editor_action_hint
import setiker.composeapp.generated.resources.remove_bg
import setiker.composeapp.generated.resources.remove_emoji
import setiker.composeapp.generated.resources.save_sticker
import setiker.composeapp.generated.resources.select_image
import setiker.composeapp.generated.resources.sticker_preview
import setiker.composeapp.generated.resources.tags_with_count

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    state: EditorState,
    onIntent: (EditorIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.edit_sticker_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeubrutalBg
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Preview (Neubrutal Frame)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .neubrutalShadow(
                            offsetX = 4.dp,
                            offsetY = 4.dp,
                            cornerRadius = 20.dp,
                            color = NeubrutalBlack
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .background(NeubrutalWhite)
                        .border(
                            width = 2.dp,
                            color = NeubrutalBlack,
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imagePath.isNotBlank()) {
                        android.util.Log.d("EditorScreen", "Loading image: ${state.imagePath}")
                        key(state.imagePath) {
                            Image(
                                painter = rememberAsyncImagePainter(state.imagePath),
                                contentDescription = stringResource(Res.string.sticker_preview),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.select_image),
                            style = MaterialTheme.typography.bodyLarge,
                            color = NeubrutalGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Image Actions (Neubrutal Circles)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    NeubrutalCircleActionButton(
                        icon = Icons.Default.Edit,
                        label = stringResource(Res.string.crop),
                        onClick = { onIntent(EditorIntent.NavigateToCrop) }
                    )
                    NeubrutalCircleActionButton(
                        icon = Icons.Default.Delete,
                        label = stringResource(Res.string.remove_bg),
                        onClick = { onIntent(EditorIntent.NavigateToBackgroundRemover) }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    text = stringResource(Res.string.editor_action_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalGray
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Emoji Tags
                Text(
                    text = stringResource(Res.string.tags_with_count, state.emojis.size, Sticker.MAX_EMOJIS),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeubrutalBlack
                )

                Spacer(modifier = Modifier.height(10.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    state.emojis.forEachIndexed { index, emoji ->
                        EmojiChip(
                            emoji = emoji,
                            onRemove = { onIntent(EditorIntent.RemoveEmoji(index)) }
                        )
                    }

                    if (state.emojis.size < Sticker.MAX_EMOJIS) {
                        NeubrutalOutlinedPill(
                            onClick = { onIntent(EditorIntent.ShowEmojiPicker) },
                            label = stringResource(Res.string.add)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Accessibility Text
                AppTextField(
                    value = state.accessibilityText,
                    onValueChange = { onIntent(EditorIntent.UpdateAccessibilityText(it)) },
                    label = stringResource(Res.string.accessibility_text),
                    placeholder = stringResource(Res.string.accessibility_text_placeholder),
                    singleLine = false,
                    maxLines = 3
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(Res.string.accessibility_text_example),
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalGray
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save Button
                AppPrimaryButton(
                    text = stringResource(Res.string.save_sticker),
                    onClick = { onIntent(EditorIntent.SaveSticker) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onBackClick
                )
            }
        }
    }

    if (state.showEmojiPicker) {
        EmojiPickerBottomSheet(
            recentEmojis = state.recentEmojis,
            onEmojiSelected = { emoji ->
                onIntent(EditorIntent.AddEmoji(emoji))
                onIntent(EditorIntent.HideEmojiPicker)
            },
            onDismiss = { onIntent(EditorIntent.HideEmojiPicker) }
        )
    }
}

@Composable
private fun NeubrutalCircleActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 28.dp,
                    color = NeubrutalBlack
                )
                .clip(CircleShape)
                .background(AccentCoralLight)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = AccentCoral
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = NeubrutalGray
        )
    }
}

@Composable
private fun EmojiChip(
    emoji: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(AccentCoralLight)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = CircleShape
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(18.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.remove_emoji),
                modifier = Modifier.size(12.dp),
                tint = NeubrutalGray
            )
        }
    }
}

@Composable
private fun NeubrutalOutlinedPill(
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(NeubrutalWhite)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = AccentCoral,
            fontWeight = FontWeight.Medium
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun EditorScreenPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(
                imagePath = "",
                emojis = listOf("😂", "🐱", "❤️"),
                accessibilityText = "A laughing cat sticker",
                showEmojiPicker = false
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun EditorScreenEmptyPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun EditorScreenLoadingPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}
