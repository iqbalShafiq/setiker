package data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workspace_drafts",
    indices = [
        Index(value = ["status"]),
        Index(value = ["updatedAt"])
    ]
)
data class WorkspaceDraftEntity(
    @PrimaryKey val id: String,
    val kind: String,
    val status: String,
    val origin: String,
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
