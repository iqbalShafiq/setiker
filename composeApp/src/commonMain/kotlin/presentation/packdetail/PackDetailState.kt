package presentation.packdetail

import data.remote.model.CloudStickerPackShareLink
import data.remote.model.PackCollaborator
import data.remote.model.UserSearchResult
import domain.model.StickerPack

data class PackDetailState(
    // Default to true so the first composition shows the loading indicator
    // instead of the "Pack not found" empty state. Initial render happens
    // before LoadPack reaches the ViewModel, and we never want to flash the
    // not-found copy on a pack we're literally about to load.
    val isLoading: Boolean = true,
    val pack: StickerPack? = null,
    val error: String? = null,
    val isDeleting: Boolean = false,
    val isAddedToWhatsApp: Boolean = false,
    val stickerImportQueue: List<String> = emptyList(),
    /** Original multi-select count; used for success copy when the queue is finished. */
    val stickerImportBatchTotal: Int = 0,
    val cloudShareSheetOpen: Boolean = false,
    val cloudShareLinksLoading: Boolean = false,
    val cloudShareLinks: List<CloudStickerPackShareLink> = emptyList(),
    val collaboratorsSheetOpen: Boolean = false,
    val collaboratorsLoading: Boolean = false,
    val collaborators: List<PackCollaborator> = emptyList(),
    val collaboratorSearchQuery: String = "",
    val collaboratorSearchResults: List<UserSearchResult> = emptyList(),
    val collaboratorInvitePermission: String = "view",
    val visibilityDialog: VisibilityDialog? = null,
    val isUpdatingVisibility: Boolean = false
)

enum class VisibilityDialog {
    MakePublic,
    Unpublish
}
