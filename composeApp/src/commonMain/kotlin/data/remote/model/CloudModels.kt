package data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateStickerPackRequest(
    val name: String,
    val description: String? = null,
    val visibility: String = "PRIVATE",
    val stickers: List<StickerPackStickerInput> = emptyList()
)

@Serializable
data class UpdateStickerPackRequest(
    val name: String? = null,
    val description: String? = null,
    val visibility: String? = null,
)

@Serializable
data class DeleteStickerSyncPayload(
    val stickerPackId: String,
    val stickerId: String,
)

@Serializable
data class StickerPackStickerInput(
    val name: String,
    val filename: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val order: Int = 0
)

@Serializable
data class CloudStickerPack(
    val id: String,
    val ownerId: String,
    val name: String,
    val description: String? = null,
    val visibility: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String? = null,
    val owner: CloudOwner? = null,
    val stickers: List<CloudStickerPackSticker> = emptyList(),
    val likeCount: Int = 0,
    val saveCount: Int = 0,
    val downloadCount: Int = 0,
    val liked: Boolean? = null,
    val saved: Boolean? = null,
    val downloaded: Boolean? = null,
    val following: Boolean? = null,
    @kotlinx.serialization.SerialName("isLiked") val isLiked: Boolean? = null,
    @kotlinx.serialization.SerialName("isSaved") val isSaved: Boolean? = null,
    @kotlinx.serialization.SerialName("isFollowingOwner") val isFollowingOwner: Boolean? = null,
)

@Serializable
data class CloudOwner(
    val id: String,
    val username: String? = null,
    val displayName: String? = null,
    val followerCount: Int? = null,
    val followingCount: Int? = null,
)

@Serializable
data class CloudStickerPackSticker(
    val id: String? = null,
    val stickerPackId: String? = null,
    val stickerId: String,
    val order: Int = 0,
    val sticker: CloudSticker? = null,
)

@Serializable
data class CloudSticker(
    val id: String,
    val ownerId: String? = null,
    val name: String,
    val filename: String? = null,
    val url: String,
    val visibility: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val fileSize: Int? = null,
    val mimeType: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null,
    val owner: CloudOwner? = null,
)

@Serializable
data class CloudDeletedRef(
    val id: String,
    val deletedAt: String? = null,
)

@Serializable
data class SyncData(
    val stickerPacks: SyncStickerPackDelta? = null,
    val stickers: SyncStickerDelta? = null,
    val syncToken: String? = null
)

@Serializable
data class SyncStickerPackDelta(
    val created: List<CloudStickerPack> = emptyList(),
    val updated: List<CloudStickerPack> = emptyList(),
    val deleted: List<CloudDeletedRef> = emptyList()
)

@Serializable
data class SyncStickerDelta(
    val created: List<CloudSticker> = emptyList(),
    val updated: List<CloudSticker> = emptyList(),
    val deleted: List<CloudDeletedRef> = emptyList()
)

@Serializable
data class UploadData(
    val stickerPackId: String? = null,
    val stickers: List<CloudSticker> = emptyList(),
    val message: String? = null,
)

@Serializable
data class CloudStickerPackShareLink(
    val id: String,
    val stickerPackId: String,
    val token: String,
    val permission: String? = null,
    val maxUses: Int? = null,
    val usesCount: Int = 0,
    val isActive: Boolean = true,
    val expiresAt: String? = null,
    val createdAt: String,
    val shareUrl: String? = null,
    val deepLinkUrl: String? = null,
    val webFallbackUrl: String? = null
)
