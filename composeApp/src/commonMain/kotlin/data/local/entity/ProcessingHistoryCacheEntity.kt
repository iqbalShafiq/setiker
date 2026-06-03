package data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "processing_history_cache")
data class ProcessingHistoryCacheEntity(
    @PrimaryKey val id: String,
    val type: String,
    val outputCount: Int,
    val previewUrl: String?,
    val createdAt: Long,
    val syncedAt: Long
)
