package presentation.publicpack

import data.remote.model.CloudStickerPack
data class PublicPackDetailState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val errorDialogMessage: String? = null,
    val pack: CloudStickerPack? = null,
    val isLiked: Boolean = false,
    val isSaved: Boolean = false,
    val isFollowingCreator: Boolean = false,
    val isOwnPack: Boolean = false,
    val showImportDialog: Boolean = false,
    val importPointCost: Int = 0,
    val pointsRemaining: Int = 0,
    val importOwnerCredit: Int = 0,
    val showReportSheet: Boolean = false,
    val reportReason: String? = null,
    val reportDetails: String = "",
    val isSubmittingReport: Boolean = false,
    val showBlockCreatorConfirm: Boolean = false,
    val isBlockingCreator: Boolean = false
)
