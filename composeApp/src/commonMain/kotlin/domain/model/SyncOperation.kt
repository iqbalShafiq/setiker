package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SyncOperation(
    val id: String,
    val type: SyncOperationType,
    val targetId: String,
    val payload: String,
    val status: SyncOperationStatus,
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long,
    val completedAt: Long? = null,
    val priority: Int = 0
)
