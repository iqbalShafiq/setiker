package presentation.legal

import domain.model.LegalDocument
import domain.model.LegalSection
import presentation.common.UiText

data class LegalDocumentState(
    val isLoading: Boolean = true,
    val document: LegalDocument? = null,
    val error: UiText? = null
)

sealed interface LegalDocumentIntent {
    data object Load : LegalDocumentIntent
    data object NavigateBack : LegalDocumentIntent
}

sealed interface LegalDocumentEffect {
    data object NavigateBack : LegalDocumentEffect
}
