package presentation.publicpack

sealed interface PublicPackDetailIntent {
    data class Load(val packId: String) : PublicPackDetailIntent
    data object ToggleLike : PublicPackDetailIntent
    data object ToggleSave : PublicPackDetailIntent
    data object ToggleFollowCreator : PublicPackDetailIntent
    data object ImportPack : PublicPackDetailIntent
    data object ConfirmImport : PublicPackDetailIntent
    data object DismissImportDialog : PublicPackDetailIntent
    data object DismissErrorDialog : PublicPackDetailIntent
    data object NavigateBack : PublicPackDetailIntent
    data class OpenCreator(val userId: String) : PublicPackDetailIntent
}
