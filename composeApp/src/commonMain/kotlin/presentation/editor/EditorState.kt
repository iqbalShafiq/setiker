package presentation.editor

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
    val backgroundRemoverPreviewPath: String? = null
)