package presentation.aijobs



sealed interface AiJobsEffect {
    data object NavigateBack : AiJobsEffect
    data class NavigateToDraft(val draftId: String, val originRoute: String?) : AiJobsEffect
    data class NavigateToPack(val packId: String) : AiJobsEffect
    /** A failure. The screen only reports failures this way. */
    data class ShowError(val message: presentation.common.UiText) : AiJobsEffect
}
