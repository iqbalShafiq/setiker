package domain.model

import kotlin.time.Clock

sealed class SyncResult {
    data object Success : SyncResult()
    data class Failed(val error: String) : SyncResult()
    data object SkippedOffline : SyncResult()
    data object SkippedNotAuthenticated : SyncResult()
    data object SkippedInProgress : SyncResult()
}

enum class SyncStage {
    IDLE,
    PUSHING_LOCAL,
    PULLING_REMOTE,
    WAITING_FOR_INTERNET,
    SIGN_IN_REQUIRED
}

data class SyncReport(
    val operationsProcessed: Int = 0,
    val operationsSucceeded: Int = 0,
    val operationsFailed: Int = 0,
    val packsSynced: Int = 0,
    val stickersSynced: Int = 0,
    val remotePacksDownloaded: Int = 0,
    val remoteStickersDownloaded: Int = 0,
    val remoteItemsDeleted: Int = 0,
    val remoteDownloadFailures: Int = 0,
    val stage: SyncStage = SyncStage.IDLE,
    val result: SyncResult = SyncResult.Success,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
