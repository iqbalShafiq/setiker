package presentation.legal

import domain.model.LegalDocument
import presentation.common.UiText

data class LegalDocumentState(
    val isLoading: Boolean = true,
    val document: LegalDocument? = null,
    val error: UiText? = null,
    val showDeletionForm: Boolean = false,
    val deletionEmail: String = "",
    val deletionReason: String = "",
    val deletionConfirmed: Boolean = false,
    val isSubmittingDeletion: Boolean = false,
    val deletionSubmitError: UiText? = null,
    val deletionSubmitSuccessMessage: UiText? = null
)

sealed interface LegalDocumentIntent {
    data object Load : LegalDocumentIntent
    data object NavigateBack : LegalDocumentIntent
    data class UpdateDeletionEmail(val email: String) : LegalDocumentIntent
    data class UpdateDeletionReason(val reason: String) : LegalDocumentIntent
    data class SetDeletionConfirmed(val confirmed: Boolean) : LegalDocumentIntent
    data object SubmitDeletionRequest : LegalDocumentIntent
}

sealed interface LegalDocumentEffect {
    data object NavigateBack : LegalDocumentEffect
}
