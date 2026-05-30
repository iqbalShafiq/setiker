package domain.model.aijob

import kotlinx.serialization.Serializable

enum class AiJobType {
    GENERATE_STICKERS,
    GENERATE_PACK,
    IMPROVE_STICKERS,
    GRID_SPLIT,
    REMOVE_BACKGROUND,
    VIDEO_PACK,
    ANIMATED_ENCODE
}

enum class AiJobStatus {
    QUEUED,
    RUNNING,
    WAITING_FOR_NETWORK,
    CHECKPOINTED,
    COMPLETED,
    APPLIED,
    FAILED_RETRYABLE,
    FAILED_FINAL,
    CANCEL_REQUESTED,
    CANCELLED
}

enum class AiJobFailureKind {
    NONE,
    NETWORK,
    SERVER,
    VALIDATION,
    MISSING_FILE,
    LOCAL_PROCESSOR,
    UNSUPPORTED_PLATFORM,
    CANCELLED,
    UNKNOWN
}

enum class AiJobOrigin {
    HOME_SHEET,
    CREATE_PACK,
    EDITOR,
    VIDEO_STICKER_PACK,
    ANIMATED_EDITOR
}

enum class WorkspaceDraftKind {
    GENERATE_PACK,
    CREATE_PACK_SESSION,
    EDITOR_SESSION,
    VIDEO_STICKER_PACK,
    ANIMATED_EDITOR
}

enum class WorkspaceDraftStatus {
    ACTIVE,
    PROCESSING,
    READY_TO_REVIEW,
    APPLIED,
    NEEDS_ATTENTION,
    CANCELLED
}

data class AiJobProgress(
    val stepKey: String,
    val stepLabel: String,
    val fraction: Float = 0f
)

data class AiJob(
    val id: String,
    val workspaceDraftId: String,
    val type: AiJobType,
    val status: AiJobStatus,
    val origin: AiJobOrigin,
    val payloadJson: String,
    val resultJson: String? = null,
    val checkpointJson: String? = null,
    val progress: AiJobProgress? = null,
    val attemptCount: Int = 0,
    val attemptGroupId: String,
    val parentJobId: String? = null,
    val failureKind: AiJobFailureKind = AiJobFailureKind.NONE,
    val failureMessage: String? = null,
    val requiresNetwork: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAttemptAt: Long? = null,
    val nextRetryAt: Long? = null,
    val completedAt: Long? = null
)

data class WorkspaceDraft(
    val id: String,
    val kind: WorkspaceDraftKind,
    val status: WorkspaceDraftStatus,
    val origin: AiJobOrigin,
    val originRoute: String? = null,
    val packId: String? = null,
    val stickerIndex: Int? = null,
    val displayTitle: String,
    val contextJson: String,
    val lastJobId: String? = null,
    val lastCompletedJobId: String? = null,
    val lastFailedJobId: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
