package presentation.createpack

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import coil3.compose.AsyncImage
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackScreen(
    state: CreatePackState,
    onIntent: (CreatePackIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showImagePicker by remember { mutableStateOf(false) }
    var isPickingTrayIcon by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.isEditing) "Edit Pack" else "Create Pack",
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
                // Pack Name
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(CreatePackIntent.UpdateName(it)) },
                    label = "Pack Name",
                    placeholder = "Enter pack name"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Publisher
                AppTextField(
                    value = state.publisher,
                    onValueChange = { onIntent(CreatePackIntent.UpdatePublisher(it)) },
                    label = "Publisher",
                    placeholder = "Enter publisher name"
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Tray Icon
                Text(
                    text = "Tray Icon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TrayIconSelector(
                    imagePath = state.trayImagePath,
                    onClick = {
                        isPickingTrayIcon = true
                        showImagePicker = true
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stickers (${state.stickers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    IconButton(
                        onClick = {
                            isPickingTrayIcon = false
                            showImagePicker = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add sticker"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Stickers Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(state.stickers) { index, stickerPath ->
                        StickerPreviewItem(
                            imagePath = stickerPath,
                            onRemove = { onIntent(CreatePackIntent.RemoveSticker(index)) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Save Button
                AppPrimaryButton(
                    text = if (state.isEditing) "Update Pack" else "Create Pack",
                    onClick = { onIntent(CreatePackIntent.SavePack) }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                AppSecondaryButton(
                    text = "Cancel",
                    onClick = onBackClick
                )
            }
        }
    }
}

@Composable
private fun TrayIconSelector(
    imagePath: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (imagePath.isNotBlank()) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Tray icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Select tray icon",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StickerPreviewItem(
    imagePath: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(80.dp)
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Sticker preview",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove sticker",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}
