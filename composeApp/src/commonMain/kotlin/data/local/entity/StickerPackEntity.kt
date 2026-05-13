package data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val isAnimated: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val cloudId: String? = null,
    val syncState: String = "LOCAL_ONLY",
    val lastSyncAt: Long? = null,
    val visibility: String = "PRIVATE",
    val cloudOwnerId: String? = null
)
