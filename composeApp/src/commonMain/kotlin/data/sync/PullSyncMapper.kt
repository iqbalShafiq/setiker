package data.sync

import data.local.entity.PendingSyncOperationEntity
import domain.model.SyncOperationStatus
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Instant

fun encodeLastSyncAt(timestampMillis: Long): String =
    Instant.fromEpochMilliseconds(timestampMillis).toString().replace(".000Z", "Z")

@OptIn(ExperimentalEncodingApi::class)
fun decodeSyncTokenToEpochMillis(syncToken: String): Long? = runCatching {
    val decoded = Base64.decode(syncToken).decodeToString()
    Instant.parse(decoded).toEpochMilliseconds()
}.getOrNull()

fun hasBlockingLocalOperation(
    operations: List<PendingSyncOperationEntity>,
    targetId: String,
): Boolean = operations.any { operation ->
    operation.targetId == targetId && operation.status in setOf(
        SyncOperationStatus.PENDING.name,
        SyncOperationStatus.IN_PROGRESS.name,
        SyncOperationStatus.FAILED.name,
    )
}

fun shouldProcessAfterReconciliation(
    createdOperations: Int,
    isOnline: Boolean,
    isAuthenticated: Boolean,
): Boolean = createdOperations > 0 && isOnline && isAuthenticated

fun shouldProcessAfterAuthChange(
    isOnline: Boolean,
    hasAccessToken: Boolean,
): Boolean = isOnline && hasAccessToken
