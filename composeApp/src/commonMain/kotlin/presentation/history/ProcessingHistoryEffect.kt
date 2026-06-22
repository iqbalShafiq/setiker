package presentation.history

import presentation.common.UiText

sealed interface ProcessingHistoryEffect {
    data object NavigateBack : ProcessingHistoryEffect
    data class ShowMessage(val message: UiText) : ProcessingHistoryEffect
}
