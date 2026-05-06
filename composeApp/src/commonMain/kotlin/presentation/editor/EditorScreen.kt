package presentation.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import domain.model.Sticker
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.EmojiPickerBottomSheet
import presentation.components.LoadingIndicator

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
                title = "Edit Sticker",
                onBackClick = onBackClick
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Image Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.imagePath.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(state.imagePath),
                            contentDescription = "Sticker preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = "Select an image",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { onIntent(EditorIntent.NavigateToCrop) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Crop"
                        )
                    }
                    IconButton(
                        onClick = { onIntent(EditorIntent.NavigateToBackgroundRemover) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove background"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Emoji Tags
                Text(
                    text = "Tags (${state.emojis.size}/${Sticker.MAX_EMOJIS})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.emojis.forEachIndexed { index, emoji ->
                        EmojiChip(
                            emoji = emoji,
                            onRemove = { onIntent(EditorIntent.RemoveEmoji(index)) }
                        )
                    }
                    
                    if (state.emojis.size < Sticker.MAX_EMOJIS) {
                        FilterChip(
                            selected = false,
                            onClick = { onIntent(EditorIntent.ShowEmojiPicker) },
                            label = { Text("+ Add") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Accessibility Text
                AppTextField(
                    value = state.accessibilityText,
                    onValueChange = { onIntent(EditorIntent.UpdateAccessibilityText(it)) },
                    label = "Accessibility Text",
                    placeholder = "Describe this sticker for screen readers",
                    singleLine = false,
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button
                AppPrimaryButton(
                    text = "Save Sticker",
                    onClick = { onIntent(EditorIntent.SaveSticker) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AppSecondaryButton(
                    text = "Cancel",
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
private fun EmojiChip(
    emoji: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.bodyLarge
        )
        IconButton(
            onClick = onRemove,
            modifier = Modifier.size(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove emoji",
                modifier = Modifier.size(12.dp)
            )
        }
    }
}


