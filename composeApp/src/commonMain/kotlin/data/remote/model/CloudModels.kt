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
    val updatedAt: String
)

@Serializable
data class SyncData(
    val stickerPacks: SyncDelta? = null,
    val stickers: SyncDelta? = null,
    val syncToken: String? = null
)

@Serializable
data class SyncDelta(
    val created: List<CloudStickerPack> = emptyList(),
    val updated: List<CloudStickerPack> = emptyList(),
    val deleted: List<CloudStickerPack> = emptyList()
)

@Serializable
data class SyncResponseData(
    val success: Boolean,
    val data: SyncData? = null
)
