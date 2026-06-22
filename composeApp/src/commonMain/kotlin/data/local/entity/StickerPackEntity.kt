package data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "sticker_packs")
data class StickerPackEntity(
    @PrimaryKey
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val isAnimated: Boolean = false,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val cloudId: String? = null,
    val syncState: String = "LOCAL_ONLY",
    val lastSyncAt: Long? = null,
    val visibility: String = "PRIVATE",
    val cloudOwnerId: String? = null
)
