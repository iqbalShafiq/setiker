package presentation.createpack

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.ImagePickerLauncher
import presentation.components.LoadingIndicator
import presentation.components.rememberImagePicker
import presentation.theme.AccentCoral
import presentation.theme.AccentCoralLight
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBg
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalShadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePackScreen(
    state: CreatePackState,
    onIntent: (CreatePackIntent) -> Unit,
    onBackClick: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    // Image picker for tray icon
    val trayIconPicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.UpdateTrayImage(it)) }
    }

    // Image picker for adding stickers
    val stickerPicker = rememberImagePicker { path ->
        path?.let { onIntent(CreatePackIntent.AddSticker(it)) }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.isEditing) "Edit Pack" else "Create Pack",
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
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(CreatePackIntent.UpdateName(it)) },
                    label = "Pack Name",
                    placeholder = "Enter pack name"
                )

                Spacer(modifier = Modifier.height(20.dp))

                AppTextField(
                    value = state.publisher,
                    onValueChange = { onIntent(CreatePackIntent.UpdatePublisher(it)) },
                    label = "Publisher",
                    placeholder = "Enter publisher name"
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Tray Icon",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = NeubrutalBlack
                )

                Spacer(modifier = Modifier.height(10.dp))

                TrayIconSelector(
                    imagePath = state.trayImagePath,
                    onClick = { trayIconPicker.launch() }
                )

                Spacer(modifier = Modifier.height(28.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stickers (${state.stickers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = NeubrutalBlack
                    )

                    IconButton(
                        onClick = { stickerPicker.launch() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add sticker",
                            tint = AccentCoral
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    itemsIndexed(state.stickers) { index, stickerPath ->
                        StickerPreviewItem(
                            imagePath = stickerPath,
                            onRemove = { onIntent(CreatePackIntent.RemoveSticker(index)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                AppPrimaryButton(
                    text = if (state.isEditing) "Update Pack" else "Create Pack",
                    onClick = { onIntent(CreatePackIntent.SavePack) }
                )

                Spacer(modifier = Modifier.height(12.dp))

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
    if (imagePath.isNotBlank()) {
        Box(
            modifier = modifier
                .size(96.dp)
                .neubrutalShadow(
                    offsetX = 3.dp,
                    offsetY = 3.dp,
                    cornerRadius = 16.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(16.dp))
                .background(NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = "Tray icon",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    } else {
        Box(
            modifier = modifier
                .size(96.dp)
                .neubrutalShadow(
                    offsetX = 3.dp,
                    offsetY = 3.dp,
                    cornerRadius = 16.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(16.dp))
                .background(AccentCoralLight)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Select tray icon",
                modifier = Modifier.size(32.dp),
                tint = NeubrutalBlack
            )
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
        modifier = modifier.size(72.dp)
    ) {
        AsyncImage(
            model = imagePath,
            contentDescription = "Sticker preview",
            modifier = Modifier
                .fillMaxSize()
                .neubrutalShadow(
                    offsetX = 2.dp,
                    offsetY = 2.dp,
                    cornerRadius = 12.dp,
                    color = NeubrutalBlack
                )
                .clip(RoundedCornerShape(12.dp))
                .background(NeubrutalWhite)
                .border(
                    width = 2.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(2.dp),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(ErrorRed)
                .border(
                    width = 1.5.dp,
                    color = NeubrutalBlack,
                    shape = RoundedCornerShape(6.dp)
                )
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove sticker",
                modifier = Modifier.size(14.dp),
                tint = NeubrutalWhite
            )
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun CreatePackScreenPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(
                name = "My Awesome Pack",
                publisher = "StickerFan",
                trayImagePath = "",
                stickers = listOf("", "", ""),
                isEditing = false
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun CreatePackScreenEditingPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(
                name = "Funny Cats",
                publisher = "CatLover",
                trayImagePath = "",
                stickers = listOf("", ""),
                isEditing = true
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun CreatePackScreenLoadingPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}
