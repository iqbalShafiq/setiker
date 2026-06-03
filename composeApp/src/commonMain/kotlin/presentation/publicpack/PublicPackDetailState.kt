package presentation.publicpack

import data.remote.model.CloudStickerPack

data class PublicPackDetailState(
    val isLoading: Boolean = true,
    val isActionLoading: Boolean = false,
    val error: String? = null,
    val pack: CloudStickerPack? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val showImportDialog: Boolean = false,
    val importPointCost: Int = 0,
    val pointsRemaining: Int = 0
)
