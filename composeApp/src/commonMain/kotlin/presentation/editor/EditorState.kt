package presentation.editor

import domain.model.StickerDecoration

data class EditorState(
    val imagePath: String = "",
    val emojis: List<String> = emptyList(),
    val accessibilityText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val showEmojiPicker: Boolean = false,
    val recentEmojis: List<String> = emptyList(),
    val packId: String = "",
    val stickerIndex: Int? = null,
    val isBackgroundRemoverSheetOpen: Boolean = false,
    val isBackgroundRemoving: Boolean = false,
    val backgroundRemoverPreviewPath: String? = null,
    val decorations: List<StickerDecoration> = emptyList(),
    val selectedDecorationId: String? = null,
    val showDecorationEmojiPicker: Boolean = false,
    val decorationEmojiPickerTargetId: String? = null,
    val isTextDecorationSheetOpen: Boolean = false
)