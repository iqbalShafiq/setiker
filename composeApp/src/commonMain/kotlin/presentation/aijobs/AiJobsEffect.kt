package presentation.aijobs

import presentation.common.UiText



sealed interface AiJobsEffect {
    data object NavigateBack : AiJobsEffect
    data class NavigateToDraft(val draftId: String, val originRoute: String?) : AiJobsEffect
    data class NavigateToPack(val packId: String) : AiJobsEffect
    /** A failure. The screen only reports failures this way. */
    data class ShowError(val message: UiText) : AiJobsEffect
}
