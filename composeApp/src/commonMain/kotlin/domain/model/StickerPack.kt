package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class StickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val stickers: List<Sticker>,
    val imageDataVersion: String = "1",
    val isAnimated: Boolean = false,
    val iosAppStoreLink: String? = null,
    val androidPlayStoreLink: String? = null,
    val publisherEmail: String? = null,
    val publisherWebsite: String? = null,
    val privacyPolicyWebsite: String? = null,
    val licenseAgreementWebsite: String? = null
) {
    companion object {
        const val MIN_STICKERS = 3
        const val MAX_STICKERS = 30
        const val TRAY_ICON_SIZE = 96
        const val STICKER_SIZE = 512
        const val MAX_STICKER_FILE_SIZE = 100 * 1024
        const val MAX_ANIMATED_STICKER_FILE_SIZE = 500 * 1024
        const val MAX_ANIMATION_DURATION_MS = 10_000L
        const val MIN_FRAME_DURATION_MS = 8L
    }
}
