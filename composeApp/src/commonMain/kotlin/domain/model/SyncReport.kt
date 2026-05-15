package domain.model

import kotlin.time.Clock

sealed class SyncResult {
    data object Success : SyncResult()
    data class Failed(val error: String) : SyncResult()
    data object SkippedOffline : SyncResult()
    data object SkippedNotAuthenticated : SyncResult()
}

data class SyncReport(
    val operationsProcessed: Int = 0,
    val operationsSucceeded: Int = 0,
    val operationsFailed: Int = 0,
    val packsSynced: Int = 0,
    val stickersSynced: Int = 0,
    val result: SyncResult = SyncResult.Success,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
