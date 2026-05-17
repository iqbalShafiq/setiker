package data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiPaginationMeta(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    val totalPages: Int = 0
)

@Serializable
data class ApiMetaWithPagination(
    val timestamp: String? = null,
    @SerialName("requestId") val requestId: String? = null,
    val pagination: ApiPaginationMeta? = null
)

@Serializable
data class ApiPaginatedSuccessEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val meta: ApiMetaWithPagination? = null
)

@Serializable
data class PackSocialStateData(
    val liked: Boolean? = null,
    val saved: Boolean? = null,
    val downloaded: Boolean? = null,
    val likeCount: Int? = null,
    val saveCount: Int? = null,
    val downloadCount: Int? = null
)

@Serializable
data class UserFollowStateData(
    val following: Boolean,
    val followerCount: Int,
    val followingCount: Int
)

@Serializable
data class SharePreviewPackData(
    val resourceType: String,
    val permission: String? = null,
    val expiresAt: String? = null,
    val maxUses: Int? = null,
    val usesCount: Int = 0,
    val stickerPack: CloudStickerPack
)

@Serializable
data class SharePreviewStickerData(
    val resourceType: String,
    val permission: String? = null,
    val expiresAt: String? = null,
    val maxUses: Int? = null,
    val usesCount: Int = 0,
    val sticker: CloudSticker
)

@Serializable
data class AcceptSharedPackData(
    val resourceType: String,
    val stickerPack: CloudStickerPack
)

@Serializable
data class AcceptSharedStickerData(
    val resourceType: String,
    val sticker: CloudSticker
)

@Serializable
data class ProcessingHistoryOutputFile(
    val url: String,
    val path: String? = null,
    val filename: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class ProcessingHistoryItem(
    val id: String,
    val userId: String,
    val type: String,
    val inputData: kotlinx.serialization.json.JsonObject? = null,
    val outputFiles: List<ProcessingHistoryOutputFile> = emptyList(),
    val expiresAt: String,
    val createdAt: String
)

@Serializable
data class DeleteCountData(
    val deletedCount: Int = 0
)

@Serializable
data class CreateStickerPackLinkRequest(
    val permission: String = "view",
    val maxUses: Int? = null,
    val expiresAt: String? = null
)
