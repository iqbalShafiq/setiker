package presentation.preview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.backgroundremover.BackgroundRemoverScreen
import presentation.backgroundremover.BackgroundRemoverState
import presentation.components.EmojiPickerBottomSheet
import presentation.crop.CropScreen
import presentation.crop.CropState
import presentation.editor.EditorScreen
import presentation.editor.EditorState
import presentation.home.HomeScreen
import presentation.home.HomeState
import presentation.createpack.CreatePackScreen
import presentation.createpack.CreatePackState
import presentation.packdetail.PackDetailScreen
import presentation.packdetail.PackDetailState

// MARK: - Mock Data

private val mockStickerPack = StickerPack(
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

private val mockPacks = listOf(
    mockStickerPack,
    StickerPack(
        identifier = "pack_preview_2",
        name = "Dogs",
        publisher = "DogLover",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""))
    ),
    StickerPack(
        identifier = "pack_preview_3",
        name = "Reactions",
        publisher = "MemeMaster",
        trayImageFile = "",
        stickers = listOf(
            Sticker(imageFile = ""),
            Sticker(imageFile = ""),
            Sticker(imageFile = "")
        )
    )
)

// MARK: - HomeScreen Previews

@Preview
@Composable
private fun HomeScreenLoadingPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(isLoading = true),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenWithPacksPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(packs = mockPacks),
            onIntent = {},
            onPackClick = {}
        )
    }
}

// MARK: - PackDetailScreen Previews

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
            state = PackDetailState(pack = mockStickerPack),
            onIntent = {},
            onBackClick = {},
            onEditPack = {},
            onAddSticker = {},
            onEditSticker = {}
        )
    }
}

// MARK: - CreatePackScreen Previews

@Preview
@Composable
private fun CreatePackScreenPreview() {
    MaterialTheme {
        CreatePackScreen(
            state = CreatePackState(
                name = "Funny Cats",
                publisher = "CatLover",
                trayImagePath = "",
                stickers = listOf("", "", "")
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
                stickers = listOf("", "", "", "", ""),
                isEditing = true
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

// MARK: - EditorScreen Previews

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
private fun EditorScreenWithDataPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(
                imagePath = "",
                emojis = listOf("😂", "🐱"),
                accessibilityText = "Laughing cat sticker"
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun EditorScreenWithEmojiPickerPreview() {
    MaterialTheme {
        EditorScreen(
            state = EditorState(
                imagePath = "",
                emojis = listOf("😂"),
                showEmojiPicker = true,
                recentEmojis = listOf("😂", "🐱", "❤️", "🔥")
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

// MARK: - CropScreen Previews

@Preview
@Composable
private fun CropScreenPreview() {
    MaterialTheme {
        CropScreen(
            state = CropState(imagePath = ""),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun CropScreenProcessingPreview() {
    MaterialTheme {
        CropScreen(
            state = CropState(
                imagePath = "",
                isProcessing = true,
                rotation = 45f,
                scale = 1.5f
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

// MARK: - BackgroundRemoverScreen Previews

@Preview
@Composable
private fun BackgroundRemoverScreenPreview() {
    MaterialTheme {
        BackgroundRemoverScreen(
            state = BackgroundRemoverState(imagePath = ""),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun BackgroundRemoverScreenProcessingPreview() {
    MaterialTheme {
        BackgroundRemoverScreen(
            state = BackgroundRemoverState(
                imagePath = "",
                isProcessing = true,
                paths = listOf()
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

// MARK: - EmojiPickerBottomSheet Preview

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