package presentation.editor

data class EditorState(
    val isLoading: Boolean = false,
    val imagePath: String = "",
    val emojis: List<String> = emptyList(),
    val accessibilityText: String = "",
    val error: String? = null
)
