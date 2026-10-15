package domain.model

/**
 * UI-facing share link model shared by pack and sticker cloud share sheets.
 */
data class ShareLinkUi(
    val id: String,
    val token: String,
    val shareUrl: String? = null,
    val deepLinkUrl: String? = null,
    val webFallbackUrl: String? = null,
    val permission: String? = null,
    val maxUses: Int? = null,
    val usesCount: Int = 0,
    val isActive: Boolean = true,
    val expiresAt: String? = null,
    val createdAt: String = ""
)

fun ShareLinkUi.preferredShareText(): String {
    return deepLinkUrl?.takeIf { it.isNotBlank() }
        ?: webFallbackUrl?.takeIf { it.isNotBlank() }
        ?: shareUrl?.takeIf { it.isNotBlank() }
        ?: token
}
