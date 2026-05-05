package presentation.createpack

sealed interface CreatePackEffect {
    data class PackSaved(val packId: String) : CreatePackEffect
    data object NavigateBack : CreatePackEffect
    data class ShowError(val message: String) : CreatePackEffect
}
