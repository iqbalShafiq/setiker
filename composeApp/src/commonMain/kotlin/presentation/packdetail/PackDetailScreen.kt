package presentation.packdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import domain.model.StickerPack
import presentation.components.AppDialog
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.StickerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackDetailScreen(
    state: PackDetailState,
    onIntent: (PackDetailIntent) -> Unit,
    onBackClick: () -> Unit,
    onEditPack: () -> Unit,
    onAddSticker: () -> Unit,
    onEditSticker: (Int) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteStickerIndex by remember { mutableStateOf(-1) }

    LaunchedEffect(state.pack) {
        state.pack?.let { pack ->
            onIntent(PackDetailIntent.LoadPack(pack.identifier))
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.pack?.name ?: "Pack Details",
                onBackClick = onBackClick,
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete pack"
                        )
                    }
                    IconButton(onClick = onEditPack) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit pack"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSticker,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add sticker"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            state.pack == null -> {
                EmptyState(
                    title = "Pack Not Found",
                    description = "The sticker pack you're looking for doesn't exist",
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                PackDetailContent(
                    pack = state.pack,
                    onIntent = onIntent,
                    onEditSticker = onEditSticker,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AppDialog(
            title = "Delete Pack",
            message = "Are you sure you want to delete \"${state.pack?.name}\"? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {
                state.pack?.let { pack ->
                    onIntent(PackDetailIntent.DeletePack(pack.identifier))
                }
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (deleteStickerIndex >= 0) {
        AppDialog(
            title = "Delete Sticker",
            message = "Are you sure you want to delete this sticker?",
            confirmText = "Delete",
            onConfirm = {
                onIntent(PackDetailIntent.DeleteSticker(deleteStickerIndex))
                deleteStickerIndex = -1
            },
            onDismiss = { deleteStickerIndex = -1 }
        )
    }
}

@Composable
private fun PackDetailContent(
    pack: StickerPack,
    onIntent: (PackDetailIntent) -> Unit,
    onEditSticker: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Pack Info Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = pack.trayImageFile,
                contentDescription = pack.name,
                modifier = Modifier
                    .size(96.dp)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            
            Column(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
            ) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = pack.publisher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${pack.stickers.size} stickers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        AppPrimaryButton(
            text = "Add to WhatsApp",
            onClick = { onIntent(PackDetailIntent.AddToWhatsApp(pack.identifier)) }
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        AppSecondaryButton(
            text = "Share Pack",
            onClick = { onIntent(PackDetailIntent.SharePack(pack.identifier)) }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Stickers Grid
        Text(
            text = "Stickers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (pack.stickers.isEmpty()) {
            EmptyState(
                title = "No Stickers",
                description = "Add stickers to this pack",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = pack.stickers,
                    key = { index, _ -> index }
                ) { index, sticker ->
                    StickerCard(
                        sticker = sticker,
                        onClick = { onEditSticker(index) },
                        onDeleteClick = { onIntent(PackDetailIntent.DeleteSticker(index)) }
                    )
                }
            }
        }
    }
}
