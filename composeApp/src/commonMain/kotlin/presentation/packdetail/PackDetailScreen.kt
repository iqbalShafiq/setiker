package presentation.packdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppDialog
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTopBar
import presentation.components.ClayFab
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.StickerCard
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow

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
            ClayFab(onClick = onAddSticker)
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeubrutalBg
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
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Pack Info Card (Neubrutal)
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = pack.trayImageFile,
                contentDescription = pack.name,
                modifier = Modifier
                    .size(96.dp)
                    .neubrutalShadow(
                        offsetX = 2.dp,
                        offsetY = 2.dp,
                        cornerRadius = 16.dp,
                        color = NeubrutalBlack
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeubrutalWhite)
                    .border(
                        width = 2.dp,
                        color = NeubrutalBlack,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentScale = ContentScale.Crop
            )

            Column(
                modifier = Modifier
                    .padding(start = 20.dp)
                    .weight(1f)
            ) {
                Text(
                    text = pack.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NeubrutalBlack
                )
                Text(
                    text = pack.publisher,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeubrutalGray
                )
                Text(
                    text = "${pack.stickers.size} stickers",
                    style = MaterialTheme.typography.bodySmall,
                    color = NeubrutalGray
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons
        AppPrimaryButton(
            text = "Add to WhatsApp",
            onClick = { onIntent(PackDetailIntent.AddToWhatsApp(pack.identifier)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppSecondaryButton(
            text = "Share Pack",
            onClick = { onIntent(PackDetailIntent.SharePack(pack.identifier)) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Stickers Grid
        Text(
            text = "Stickers",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = NeubrutalBlack
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (pack.stickers.isEmpty()) {
            EmptyState(
                title = "No Stickers",
                description = "Add stickers to this pack",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
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

// MARK: - Previews

private val mockPack = StickerPack(
    identifier = "pack_preview",
    name = "Funny Cats",
    publisher = "CatLover",
    trayImageFile = "",
    stickers = listOf(
        Sticker(imageFile = "", emojis = listOf("😂", "🐱")),
        Sticker(imageFile = "", emojis = listOf("😻")),
        Sticker(imageFile = "", emojis = listOf("🙀", "❤️")),
        Sticker(imageFile = "", emojis = listOf("😹"))
    )
)

@Preview
@Composable
private fun PackDetailScreenLoadingPreview() {
    MaterialTheme {
        PackDetailScreen(
            state = PackDetailState(isLoading = true),
            onIntent = {},
            onBackClick = {},
            onEditPack = {},
            onAddSticker = {},
            onEditSticker = {}
        )
    }
}

@Preview
@Composable
private fun PackDetailScreenPreview() {
    MaterialTheme {
        PackDetailScreen(
            state = PackDetailState(pack = mockPack),
            onIntent = {},
            onBackClick = {},
            onEditPack = {},
            onAddSticker = {},
            onEditSticker = {}
        )
    }
}
