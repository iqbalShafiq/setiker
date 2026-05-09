package data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stickers",
    foreignKeys = [
        ForeignKey(
            entity = StickerPackEntity::class,
            parentColumns = ["identifier"],
            childColumns = ["packId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("packId")]
)
data class StickerEntity(
    @PrimaryKey
    val id: String,
    val packId: String,
    val imageFile: String,
    val sourceImageFile: String? = null,
    val emojis: String,
    val accessibilityText: String?,
    val decorationsJson: String? = null,
    val sortOrder: Int
)