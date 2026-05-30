package data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ai_jobs",
    indices = [
        Index(value = ["status"]),
        Index(value = ["workspaceDraftId"]),
        Index(value = ["attemptGroupId"]),
        Index(value = ["createdAt"])
    ]
)
data class AiJobEntity(
    @PrimaryKey val id: String,
    val workspaceDraftId: String,
    val type: String,
    val status: String,
    val origin: String,
    val payloadJson: String,
    val resultJson: String? = null,
    val checkpointJson: String? = null,
    val progressStepKey: String? = null,
    val progressStepLabel: String? = null,
    val progressFraction: Float = 0f,
    val attemptCount: Int = 0,
    val attemptGroupId: String,
    val parentJobId: String? = null,
    val failureKind: String = "NONE",
    val failureMessage: String? = null,
    val requiresNetwork: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAttemptAt: Long? = null,
    val nextRetryAt: Long? = null,
    val completedAt: Long? = null
)
