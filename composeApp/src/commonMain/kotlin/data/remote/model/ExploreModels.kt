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
    val pagination: ApiPaginationMeta? = null,
    val unreadCount: Int? = null
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

@Serializable
data class PublicUserProfile(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val publicPackCount: Int = 0,
    val isFollowing: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class UserSearchResult(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val followerCount: Int = 0
)

@Serializable
data class ImportPublicPackResult(
    val pack: CloudStickerPack,
    val pointCost: Int = 0,
    val ownerCredited: Int = 0,
    val pointsRemaining: Int = 0
)

@Serializable
data class FeaturedTodayData(
    val pack: CloudStickerPack? = null,
    val score: Double = 0.0,
    val windowStart: String? = null,
    val windowEnd: String? = null
)

@Serializable
data class UserNotificationItem(
    val id: String,
    val userId: String,
    val type: String,
    val title: String,
    val body: String? = null,
    val payload: kotlinx.serialization.json.JsonObject? = null,
    val readAt: String? = null,
    val createdAt: String
)

@Serializable
data class PackCollaborator(
    val id: String,
    val stickerPackId: String,
    val sharedWithId: String,
    val permission: String,
    val createdAt: String,
    val expiresAt: String? = null,
    val sharedWith: CloudOwner? = null
)

@Serializable
data class SharePackWithUserRequest(
    val userId: String,
    val permission: String = "view",
    val expiresAt: String? = null
)

@Serializable
data class PromptPresetDto(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val referenceHint: String? = null,
    val sortOrder: Int = 0
)
