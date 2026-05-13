package presentation.sync

sealed class SyncIntent {
    data object LoadOperations : SyncIntent()
    data object SyncNow : SyncIntent()
    data class RetryOperation(val operationId: String) : SyncIntent()
    data class CancelOperation(val operationId: String) : SyncIntent()
    data object ClearCompleted : SyncIntent()
}