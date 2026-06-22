package presentation.common

import data.remote.model.CloudStickerPackShareLink

fun CloudStickerPackShareLink.preferredShareText(): String {
    return deepLinkUrl?.takeIf { it.isNotBlank() }
        ?: webFallbackUrl?.takeIf { it.isNotBlank() }
        ?: shareUrl?.takeIf { it.isNotBlank() }
        ?: token
}
