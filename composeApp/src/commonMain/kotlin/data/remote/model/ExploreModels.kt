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

/** Whether the current user has liked this pack (supports API field aliases). */
fun CloudStickerPack.userHasLiked(): Boolean = isLiked ?: liked ?: false

/** Whether the current user has saved this pack (supports API field aliases). */
fun CloudStickerPack.userHasSaved(): Boolean = isSaved ?: saved ?: false

/**
 * Merges a partial social-action API response without clearing unrelated flags
 * (e.g. save response must not reset an existing liked state).
 */
fun CloudStickerPack.withOptimisticLike(liked: Boolean): CloudStickerPack {
    val delta = when {
        userHasLiked() == liked -> 0
        liked -> 1
        else -> -1
    }
    return copy(
        liked = liked,
        isLiked = liked,
        likeCount = (likeCount + delta).coerceAtLeast(0)
    )
}

fun CloudStickerPack.withOptimisticSave(saved: Boolean): CloudStickerPack {
    val delta = when {
        userHasSaved() == saved -> 0
        saved -> 1
        else -> -1
    }
    return copy(
        saved = saved,
        isSaved = saved,
        saveCount = (saveCount + delta).coerceAtLeast(0)
    )
}

fun CloudStickerPack.applySocialUpdate(social: PackSocialStateData): CloudStickerPack {
    val resolvedLiked = social.liked ?: isLiked ?: liked
    val resolvedSaved = social.saved ?: isSaved ?: saved
    return copy(
        likeCount = social.likeCount ?: likeCount,
        saveCount = social.saveCount ?: saveCount,
        downloadCount = social.downloadCount ?: downloadCount,
        liked = resolvedLiked,
        saved = resolvedSaved,
        downloaded = social.downloaded ?: downloaded,
        isLiked = resolvedLiked,
        isSaved = resolvedSaved
    )
}

@Serializable
data class UserFollowStateData(
    val following: Boolean,
    val followerCount: Int,
    val followingCount: Int
)

fun PublicUserProfile.withOptimisticFollow(following: Boolean): PublicUserProfile {
    val delta = when {
        isFollowing == following -> 0
        following -> 1
        else -> -1
    }
    return copy(
        isFollowing = following,
        followerCount = (followerCount + delta).coerceAtLeast(0)
    )
}

fun PublicUserProfile.applyFollowUpdate(followState: UserFollowStateData): PublicUserProfile {
    return copy(
        isFollowing = followState.following,
        followerCount = followState.followerCount,
        followingCount = followState.followingCount
    )
}

fun CloudStickerPack.userIsFollowingOwner(): Boolean = isFollowingOwner ?: following ?: false

fun CloudStickerPack.withOptimisticFollowOwner(following: Boolean): CloudStickerPack {
    val wasFollowing = userIsFollowingOwner()
    val delta = when {
        wasFollowing == following -> 0
        following -> 1
        else -> -1
    }
    val updatedOwner = owner?.copy(
        followerCount = ((owner.followerCount ?: 0) + delta).coerceAtLeast(0)
    )
    return copy(
        following = following,
        isFollowingOwner = following,
        owner = updatedOwner
    )
}

fun CloudStickerPack.applyFollowUpdate(followState: UserFollowStateData): CloudStickerPack {
    val updatedOwner = owner?.copy(
        followerCount = followState.followerCount,
        followingCount = followState.followingCount
    )
    return copy(
        following = followState.following,
        isFollowingOwner = followState.following,
        owner = updatedOwner
    )
}

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
