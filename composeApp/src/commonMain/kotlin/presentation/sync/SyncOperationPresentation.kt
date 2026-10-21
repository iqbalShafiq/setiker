package presentation.sync

import domain.model.SyncOperationStatus
import domain.model.SyncOperationType

/**
 * Why a sync operation failed, in terms the user can act on.
 */
enum class SyncErrorReason {
    /** No usable connection to the server. */
    CONNECTION,

    /** The server did not answer in time. */
    TIMEOUT,

    /** The session is no longer valid; the user has to sign in again. */
    AUTHENTICATION,

    /** The server failed to handle the request. */
    SERVER
}

/**
 * Strips technical noise out of a raw error message.
 *
 * The raw message comes from the HTTP client and used to reach the UI verbatim, which leaked
 * internal host names, ports and timeout configuration into a consumer screen.
 *
 * @return the cleaned message, or `null` when nothing user-facing is left.
 */
internal fun sanitizeSyncError(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    var message = raw
    // Drop bracketed technical trailers such as "[url=http://host/path, connect_timeout=30000 ms]".
    message = message.replace(Regex("""\s*\[[^\]]*]"""), "")
    // Drop bare URLs (they expose internal infrastructure).
    message = message.replace(Regex("""https?://\S+"""), "")
    // Drop trailing separators left behind by the removals.
    message = message.replace(Regex("""\s*[-:,]\s*$"""), "")
    message = message.trim().trim('"', '\'')
    return message.ifBlank { null }
}

/**
 * Classifies a raw error message so the UI can show an actionable sentence.
 *
 * @return the reason, or `null` when the message does not match a known cause.
 */
internal fun syncErrorReason(raw: String?): SyncErrorReason? {
    if (raw.isNullOrBlank()) return null
    val message = raw.lowercase()
    return when {
        message.contains("timeout") ||
            message.contains("timed out") ||
            message.contains("timedout") -> SyncErrorReason.TIMEOUT

        message.contains("unknownhost") ||
            message.contains("unable to resolve host") ||
            message.contains("nodename nor servname") ||
            message.contains("network is unreachable") ||
            message.contains("offline") -> SyncErrorReason.CONNECTION

        message.contains("connectexception") ||
            message.contains("connection refused") ||
            message.contains("failed to connect") ||
            message.contains("connection reset") ||
            message.contains("econnrefused") -> SyncErrorReason.CONNECTION

        message.contains("unauthorized") ||
            message.contains("401") ||
            message.contains("token has expired") ||
            message.contains("forbidden") ||
            message.contains("403") -> SyncErrorReason.AUTHENTICATION

        message.contains("500") ||
            message.contains("502") ||
            message.contains("503") ||
            message.contains("internal server error") -> SyncErrorReason.SERVER

        else -> null
    }
}

/**
 * A short, human title for a sync operation.
 *
 * The operation type used to be printed raw ("UPDATE_PACK") next to a raw UUID, which reads like a
 * developer log rather than something a user can act on.
 */
internal fun syncOperationTitle(type: SyncOperationType): SyncOperationTypeTitle = when (type) {
    SyncOperationType.CREATE_PACK -> SyncOperationTypeTitle.CREATING_PACK
    SyncOperationType.UPDATE_PACK -> SyncOperationTypeTitle.UPDATING_PACK
    SyncOperationType.UPDATE_PACK_VISIBILITY -> SyncOperationTypeTitle.UPDATING_PACK
    SyncOperationType.DELETE_PACK -> SyncOperationTypeTitle.DELETING_PACK
    SyncOperationType.ADD_STICKER -> SyncOperationTypeTitle.ADDING_STICKER
    SyncOperationType.UPDATE_STICKER -> SyncOperationTypeTitle.UPDATING_STICKER
    SyncOperationType.DELETE_STICKER -> SyncOperationTypeTitle.DELETING_STICKER
    SyncOperationType.UPLOAD_IMAGE -> SyncOperationTypeTitle.UPLOADING_IMAGE
    SyncOperationType.REORDER_STICKERS -> SyncOperationTypeTitle.REORDERING_STICKERS
}

/** User-facing titles for sync operation types. */
enum class SyncOperationTypeTitle {
    CREATING_PACK,
    UPDATING_PACK,
    DELETING_PACK,
    ADDING_STICKER,
    UPDATING_STICKER,
    DELETING_STICKER,
    UPLOADING_IMAGE,
    REORDERING_STICKERS
}

/** User-facing labels for sync operation statuses. */
internal fun syncOperationStatusLabel(status: SyncOperationStatus): SyncOperationStatusLabel = when (status) {
    SyncOperationStatus.PENDING -> SyncOperationStatusLabel.PENDING
    SyncOperationStatus.IN_PROGRESS -> SyncOperationStatusLabel.RUNNING
    SyncOperationStatus.SUCCESS -> SyncOperationStatusLabel.DONE
    SyncOperationStatus.FAILED -> SyncOperationStatusLabel.FAILED
    SyncOperationStatus.CANCELLED -> SyncOperationStatusLabel.CANCELLED
}

/** User-facing labels for sync operation statuses. */
enum class SyncOperationStatusLabel {
    PENDING,
    RUNNING,
    DONE,
    FAILED,
    CANCELLED
}
