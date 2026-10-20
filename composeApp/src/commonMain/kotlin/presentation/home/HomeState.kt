package presentation.home

import domain.model.AiUsage
import domain.model.AuthState
import domain.model.SortOrder
import domain.model.StickerPack
import domain.model.User

data class HomeState(
    val isLoading: Boolean = false,
    val loadFailed: Boolean = false,
    val packs: List<StickerPack> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val error: String? = null,
    val currentUser: User? = null,
    /**
     * Whether the session has been resolved yet.
     *
     * [currentUser] alone cannot tell "signed out" from "not resolved yet", and the difference
     * decides whether an account action opens the profile or the login screen.
     */
    val authState: AuthState = AuthState.UNKNOWN,
    val isSyncing: Boolean = false,
    val pendingSyncCount: Int = 0,
    val isGeneratePackSheetOpen: Boolean = false,
    val generatePackPrompt: String = "",
    val generatePackName: String = "",
    val generatePackPublisher: String = "",
    val generatePackLayout: String = "4x4",
    val generatePackInputImagePath: String? = null,
    val isGeneratePackLoading: Boolean = false,
    val aiUsage: AiUsage? = null,
    val isLoadingAiUsage: Boolean = false,
    val aiUsageLoadFailed: Boolean = false,
    val activeAiJobCount: Int = 0,
    val aiJobsBadgeCount: Int = 0,
    val backgroundJobMessage: String? = null,
    val homeWorkspaceDraftId: String? = null,
    val processingPacks: List<HomeProcessingPack> = emptyList()
) {
    val filteredPacks: List<StickerPack>
        get() = packs
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedWith(sortOrder.comparator)

    val filteredProcessingPacks: List<HomeProcessingPack>
        get() = processingPacks
            .filter { it.name.contains(searchQuery, ignoreCase = true) }
            .sortedBy { it.name.lowercase() }

    /**
     * Whether the user owns any content at all.
     *
     * This is deliberately derived from the **unfiltered** lists: it decides between the
     * first-run empty state ("No Stickers Yet") and the list body. Deriving it from the filtered
     * lists made a search that matched nothing fall into the first-run empty state and left the
     * dedicated "No packs found" state unreachable.
     */
    val hasListContent: Boolean
        get() = packs.isNotEmpty() || processingPacks.isNotEmpty()

    /** True when a search is active and nothing matched it. */
    val hasSearchNoResults: Boolean
        get() = searchQuery.isNotEmpty() &&
            filteredPacks.isEmpty() &&
            filteredProcessingPacks.isEmpty()
}
