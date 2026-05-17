package presentation.sharepreview

import data.remote.model.SharePreviewPackData
import data.remote.model.SharePreviewStickerData

data class SharePreviewState(
    val isLoading: Boolean = true,
    val isAccepting: Boolean = false,
    val error: String? = null,
    val kind: String = "pack",
    val packPreview: SharePreviewPackData? = null,
    val stickerPreview: SharePreviewStickerData? = null
)
