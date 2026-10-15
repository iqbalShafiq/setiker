package presentation.common

import data.remote.model.CloudStickerPackShareLink
import data.remote.model.CloudStickerShareLink
import domain.model.ShareLinkUi
import domain.model.preferredShareText

fun CloudStickerPackShareLink.preferredShareText(): String = toShareLinkUi().preferredShareText()

fun CloudStickerPackShareLink.toShareLinkUi(): ShareLinkUi = ShareLinkUi(
    id = id,
    token = token,
    shareUrl = shareUrl,
    deepLinkUrl = deepLinkUrl,
    webFallbackUrl = webFallbackUrl,
    permission = permission,
    maxUses = maxUses,
    usesCount = usesCount,
    isActive = isActive,
    expiresAt = expiresAt,
    createdAt = createdAt
)

fun CloudStickerShareLink.toShareLinkUi(): ShareLinkUi = ShareLinkUi(
    id = id,
    token = token,
    shareUrl = shareUrl,
    deepLinkUrl = deepLinkUrl,
    webFallbackUrl = webFallbackUrl,
    permission = permission,
    maxUses = maxUses,
    usesCount = usesCount,
    isActive = isActive,
    expiresAt = expiresAt,
    createdAt = createdAt
)
