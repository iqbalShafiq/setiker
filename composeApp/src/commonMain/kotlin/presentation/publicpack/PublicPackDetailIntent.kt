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
    data object ShowReportSheet : PublicPackDetailIntent
    data object DismissReportSheet : PublicPackDetailIntent
    data class SelectReportReason(val reason: String) : PublicPackDetailIntent
    data class UpdateReportDetails(val value: String) : PublicPackDetailIntent
    data object SubmitReport : PublicPackDetailIntent
    data object ShowBlockCreatorConfirm : PublicPackDetailIntent
    data object DismissBlockCreatorConfirm : PublicPackDetailIntent
    data object ConfirmBlockCreator : PublicPackDetailIntent
    data object NavigateBack : PublicPackDetailIntent
    data class OpenCreator(val userId: String) : PublicPackDetailIntent
}
