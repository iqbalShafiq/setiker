package data.sync

import data.local.entity.PendingSyncOperationEntity
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType

/**
 * Shared SyncOperation ↔ Room-entity mapping (single source; previously duplicated as private
 * extensions inside `SyncManagerImpl`).
 */
internal fun PendingSyncOperationEntity.toSyncOperation(): SyncOperation = SyncOperation(
    id = id,
    type = SyncOperationType.valueOf(type),
    targetId = targetId,
    payload = payload,
    status = SyncOperationStatus.valueOf(status),
    errorMessage = errorMessage,
    retryCount = retryCount,
    createdAt = createdAt,
    completedAt = completedAt,
    priority = priority,
)

internal fun SyncOperation.toPendingSyncEntity(): PendingSyncOperationEntity = PendingSyncOperationEntity(
    id = id,
    type = type.name,
    targetId = targetId,
    payload = payload,
    status = status.name,
    errorMessage = errorMessage,
    retryCount = retryCount,
    createdAt = createdAt,
    completedAt = completedAt,
    priority = priority,
)
