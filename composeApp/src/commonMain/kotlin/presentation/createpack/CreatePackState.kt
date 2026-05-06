package presentation.createpack

data class CreatePackState(
    val isLoading: Boolean = false,
    val name: String = "",
    val publisher: String = "",
    val trayImagePath: String = "",
    val stickers: List<String> = emptyList(),
    val isEditing: Boolean = false,
    val packId: String = "",
    val error: String? = null
)
