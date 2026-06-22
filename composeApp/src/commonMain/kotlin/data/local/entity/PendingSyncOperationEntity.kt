package data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sync_queue",
    indices = [
        Index(value = ["status"]),
        Index(value = ["targetId"]),
        Index(value = ["createdAt"])
    ]
)
data class PendingSyncOperationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val targetId: String,
    val payload: String,
    val status: String = "PENDING",
    val errorMessage: String? = null,
    val retryCount: Int = 0,
    val createdAt: Long,
    val completedAt: Long? = null,
    val priority: Int = 0
)
