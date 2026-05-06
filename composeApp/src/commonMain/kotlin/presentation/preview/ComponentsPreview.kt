package presentation.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppDialog
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.components.AppTextField
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.StickerCard
import presentation.components.StickerPackCard

// MARK: - Button Previews

@Preview
@Composable
private fun AppPrimaryButtonPreview() {
    MaterialTheme {
        AppPrimaryButton(
            text = "Save Pack",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun AppSecondaryButtonPreview() {
    MaterialTheme {
        AppSecondaryButton(
            text = "Cancel",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun AppButtonsCombinedPreview() {
    MaterialTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            AppPrimaryButton(text = "Primary Button", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            AppSecondaryButton(text = "Secondary Button", onClick = {})
            Spacer(modifier = Modifier.height(8.dp))
            AppPrimaryButton(text = "Disabled", onClick = {}, enabled = false)
        }
    }
}

// MARK: - TopBar Previews

@Preview
@Composable
private fun AppTopBarPreview() {
    MaterialTheme {
        AppTopBar(title = "My Stickers")
    }
}

@Preview
@Composable
private fun AppTopBarWithBackPreview() {
    MaterialTheme {
        AppTopBar(
            title = "Pack Details",
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun AppTopBarWithActionsPreview() {
    MaterialTheme {
        AppTopBar(
            title = "Settings",
            onBackClick = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
    }
}

// MARK: - TextField Previews

@Preview
@Composable
private fun AppTextFieldPreview() {
    MaterialTheme {
        AppTextField(
            value = "My Sticker Pack",
            onValueChange = {},
            label = "Pack Name",
            placeholder = "Enter pack name"
        )
    }
}

@Preview
@Composable
private fun AppTextFieldErrorPreview() {
    MaterialTheme {
        AppTextField(
            value = "",
            onValueChange = {},
            label = "Pack Name",
            placeholder = "Enter pack name",
            isError = true
        )
    }
}

// MARK: - Dialog Preview

@Preview
@Composable
private fun AppDialogPreview() {
    MaterialTheme {
        AppDialog(
            title = "Delete Pack",
            message = "Are you sure you want to delete this pack? This action cannot be undone.",
            confirmText = "Delete",
            onConfirm = {},
            onDismiss = {}
        )
    }
}

// MARK: - EmptyState Preview

@Preview
@Composable
private fun EmptyStatePreview() {
    MaterialTheme {
        EmptyState(
            title = "No Stickers Yet",
            description = "Create your first sticker pack to get started"
        )
    }
}

@Preview
@Composable
private fun EmptyStateWithActionPreview() {
    MaterialTheme {
        EmptyState(
            title = "No Stickers Yet",
            description = "Create your first sticker pack to get started",
            action = {
                AppPrimaryButton(text = "Create Pack", onClick = {})
            }
        )
    }
}

// MARK: - LoadingIndicator Preview

@Preview
@Composable
private fun LoadingIndicatorPreview() {
    MaterialTheme {
        LoadingIndicator()
    }
}

// MARK: - StickerPackCard Preview

@Preview
@Composable
private fun StickerPackCardPreview() {
    val mockPack = StickerPack(
        identifier = "pack_1",
        name = "Funny Cats",
        publisher = "CatLover",
        trayImageFile = "",
        stickers = listOf(
            Sticker(imageFile = ""),
            Sticker(imageFile = ""),
            Sticker(imageFile = "")
        )
    )
    MaterialTheme {
        StickerPackCard(
            pack = mockPack,
            onClick = {}
        )
    }
}

// MARK: - StickerCard Preview

@Preview
@Composable
private fun StickerCardPreview() {
    val mockSticker = Sticker(
        imageFile = "",
        emojis = listOf("😂", "🐱"),
        accessibilityText = "Laughing cat"
    )
    MaterialTheme {
        StickerCard(
            sticker = mockSticker,
            onClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview
@Composable
private fun StickerCardWithoutDeletePreview() {
    val mockSticker = Sticker(
        imageFile = "",
        emojis = listOf("😂")
    )
    MaterialTheme {
        StickerCard(
            sticker = mockSticker,
            onClick = {}
        )
    }
}