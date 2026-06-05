package presentation.packdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import data.remote.model.CreateStickerPackLinkRequest
import data.remote.model.SharePackWithUserRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import data.storage.StickerFileStorage
import domain.model.Sticker
import domain.model.StickerPack
import domain.repository.StickerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import presentation.common.preferredShareText
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_add_stickers
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.pack_collaborators_sync_required
import setiker.composeapp.generated.resources.error_failed_delete_sticker
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.error_tray_icon_required_whatsapp
import setiker.composeapp.generated.resources.pack_duplicate_success
import setiker.composeapp.generated.resources.success_stickers_added

class PackDetailViewModel(
    private val repository: StickerRepository,
    private val fileStorage: StickerFileStorage,
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PackDetailState())
    val state: StateFlow<PackDetailState> = _state.asStateFlow()

    private val _effect = Channel<PackDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: PackDetailIntent) {
        when (intent) {
            is PackDetailIntent.LoadPack -> loadPack(intent.packId, intent.silentRefresh)
            is PackDetailIntent.AddToWhatsApp -> addToWhatsApp(intent.packId)
            is PackDetailIntent.DeletePack -> deletePack(intent.packId)
            is PackDetailIntent.EditPack -> {
                viewModelScope.launch {
                    _effect.send(PackDetailEffect.NavigateToEditPack)
                }
            }
            is PackDetailIntent.DeleteSticker -> deleteSticker(intent.index)
            is PackDetailIntent.AddSticker -> {
                viewModelScope.launch {
                    _effect.send(PackDetailEffect.NavigateToAddSticker)
                }
            }
            is PackDetailIntent.StageStickerImports -> stageStickerImports(intent.imagePaths)
            is PackDetailIntent.DismissStickerImportSheet -> dismissStickerImport()
            is PackDetailIntent.ApplyCroppedStickerImport -> applyCroppedStickerImport(intent.croppedPath)
            is PackDetailIntent.EditSticker -> {
                viewModelScope.launch {
                    _effect.send(PackDetailEffect.NavigateToEditSticker(intent.index))
                }
            }
            PackDetailIntent.OpenCloudShareSheet -> {
                _state.update { it.copy(cloudShareSheetOpen = true) }
                refreshCloudShareLinks()
            }
            PackDetailIntent.DismissCloudShareSheet -> {
                _state.update { it.copy(cloudShareSheetOpen = false) }
            }
            PackDetailIntent.RefreshCloudShareLinks -> refreshCloudShareLinks()
            PackDetailIntent.CreateCloudShareLink -> createCloudShareLink()
            is PackDetailIntent.RevokeCloudShareLink -> revokeCloudShareLink(intent.linkId)
            PackDetailIntent.DuplicatePack -> duplicatePack()
            PackDetailIntent.OpenCollaboratorsSheet -> {
                val cloudId = _state.value.pack?.cloudId
                if (cloudId.isNullOrBlank()) {
                    viewModelScope.launch {
                        _effect.send(
                            PackDetailEffect.ShowError(
                                UiText.StringRes(Res.string.pack_collaborators_sync_required)
                            )
                        )
                    }
                    return
                }
                _state.update { it.copy(collaboratorsSheetOpen = true) }
                refreshCollaborators()
            }
            PackDetailIntent.DismissCollaboratorsSheet -> {
                _state.update { it.copy(collaboratorsSheetOpen = false) }
            }
            PackDetailIntent.RefreshCollaborators -> refreshCollaborators()
            is PackDetailIntent.CollaboratorSearchChanged -> onCollaboratorSearch(intent.query)
            is PackDetailIntent.InviteCollaborator -> inviteCollaborator(intent.userId)
            is PackDetailIntent.RemoveCollaborator -> removeCollaborator(intent.userId)
            is PackDetailIntent.CollaboratorPermissionChanged -> {
                _state.update { it.copy(collaboratorInvitePermission = intent.permission) }
            }
            PackDetailIntent.RequestMakePublic -> {
                _state.update { it.copy(visibilityDialog = VisibilityDialog.MakePublic) }
            }
            PackDetailIntent.RequestUnpublish -> {
                _state.update { it.copy(visibilityDialog = VisibilityDialog.Unpublish) }
            }
            PackDetailIntent.ConfirmVisibilityChange -> confirmVisibilityChange()
            PackDetailIntent.DismissVisibilityDialog -> {
                _state.update { it.copy(visibilityDialog = null) }
            }
        }
    }

    private var collaboratorSearchJob: Job? = null

    private fun onCollaboratorSearch(query: String) {
        _state.update { it.copy(collaboratorSearchQuery = query) }
        collaboratorSearchJob?.cancel()
        if (query.trim().length < 2) {
            _state.update { it.copy(collaboratorSearchResults = emptyList()) }
            return
        }
        collaboratorSearchJob = viewModelScope.launch {
            delay(300)
            runCatching { exploreApiRepository.searchUsers(query) }
                .onSuccess { results ->
                    _state.update { it.copy(collaboratorSearchResults = results) }
                }
        }
    }

    private fun refreshCollaborators() {
        val cloudId = _state.value.pack?.cloudId
        if (cloudId.isNullOrBlank()) {
            viewModelScope.launch {
                _effect.send(
                    PackDetailEffect.ShowError(UiText.StringRes(Res.string.pack_collaborators_sync_required))
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(collaboratorsLoading = true) }
            runCatching { exploreApiRepository.listPackCollaborators(cloudId) }
                .onSuccess { list ->
                    _state.update { it.copy(collaboratorsLoading = false, collaborators = list) }
                }
                .onFailure {
                    _state.update { it.copy(collaboratorsLoading = false) }
                }
        }
    }

    private fun inviteCollaborator(userId: String) {
        val cloudId = _state.value.pack?.cloudId
        if (cloudId.isNullOrBlank()) {
            viewModelScope.launch {
                _effect.send(
                    PackDetailEffect.ShowError(UiText.StringRes(Res.string.pack_collaborators_sync_required))
                )
            }
            return
        }
        val permission = _state.value.collaboratorInvitePermission
        viewModelScope.launch {
            runCatching {
                exploreApiRepository.sharePackWithUser(
                    cloudId,
                    SharePackWithUserRequest(userId = userId, permission = permission)
                )
            }.onSuccess {
                refreshCollaborators()
                _state.update { it.copy(collaboratorSearchResults = emptyList(), collaboratorSearchQuery = "") }
            }.onFailure { error ->
                _effect.send(PackDetailEffect.ShowError(error.toUiText(Res.string.error_failed_add_pack)))
            }
        }
    }

    private fun removeCollaborator(userId: String) {
        val cloudId = _state.value.pack?.cloudId
        if (cloudId.isNullOrBlank()) {
            viewModelScope.launch {
                _effect.send(
                    PackDetailEffect.ShowError(UiText.StringRes(Res.string.pack_collaborators_sync_required))
                )
            }
            return
        }
        viewModelScope.launch {
            runCatching { exploreApiRepository.removePackCollaborator(cloudId, userId) }
                .onSuccess { refreshCollaborators() }
                .onFailure { error ->
                    _effect.send(PackDetailEffect.ShowError(error.toUiText(Res.string.error_failed_delete_pack)))
                }
        }
    }

    private fun confirmVisibilityChange() {
        val dialog = _state.value.visibilityDialog ?: return
        val pack = _state.value.pack ?: return
        val makePublic = dialog == VisibilityDialog.MakePublic
        if (makePublic && pack.stickers.size < StickerPack.MIN_STICKERS) {
            viewModelScope.launch {
                _effect.send(
                    PackDetailEffect.ShowError(
                        UiText.StringRes(Res.string.error_pack_min_stickers_whatsapp)
                    )
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isUpdatingVisibility = true, visibilityDialog = null) }
            runCatching {
                val updated = pack.copy(visibility = if (makePublic) "PUBLIC" else "PRIVATE")
                repository.savePack(updated)
                repository.syncPack(pack.identifier)
                repository.getPack(pack.identifier)
            }.onSuccess { synced ->
                _state.update { it.copy(isUpdatingVisibility = false, pack = synced) }
                _effect.send(
                    PackDetailEffect.ShowSuccess(
                        UiText.DynamicString(if (makePublic) "Pack is now public on Explore" else "Pack removed from Explore")
                    )
                )
                if (makePublic && !synced.cloudId.isNullOrBlank()) {
                    _effect.send(PackDetailEffect.NavigateToPublicPack(synced.cloudId!!))
                }
            }.onFailure { error ->
                _state.update { it.copy(isUpdatingVisibility = false) }
                _effect.send(PackDetailEffect.ShowError(error.toUiText(Res.string.error_failed_add_pack)))
            }
        }
    }

    private fun duplicatePack() {
        val packId = _state.value.pack?.identifier ?: return
        viewModelScope.launch {
            runCatching { repository.duplicatePack(packId) }
                .onSuccess { newPackId ->
                    _effect.send(PackDetailEffect.ShowSuccess(UiText.StringRes(Res.string.pack_duplicate_success)))
                    _effect.send(PackDetailEffect.NavigateToDuplicatedPack(newPackId))
                }
                .onFailure { error ->
                    _effect.send(PackDetailEffect.ShowError(error.toUiText(Res.string.error_failed_add_pack)))
                }
        }
    }

    private fun loadPack(packId: String, silentRefresh: Boolean = false) {
        viewModelScope.launch {
            val canSkipBlockingLoader = silentRefresh && _state.value.pack?.identifier == packId
            _state.update { current ->
                if (canSkipBlockingLoader) {
                    current.copy(error = null)
                } else {
                    current.copy(isLoading = true, error = null)
                }
            }
            try {
                val pack = repository.getPack(packId)
                _state.update { it.copy(isLoading = false, isDeleting = false, pack = pack) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, isDeleting = false, error = e.message) }
            }
        }
    }

    private fun addToWhatsApp(packId: String) {
        viewModelScope.launch {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.size < StickerPack.MIN_STICKERS) {
                    _effect.send(
                        PackDetailEffect.ShowError(
                            UiText.StringRes(
                                Res.string.error_pack_min_stickers_whatsapp,
                                listOf(StickerPack.MIN_STICKERS)
                            )
                        )
                    )
                    return@launch
                }
                if (pack.trayImageFile.isBlank()) {
                    _effect.send(
                        PackDetailEffect.ShowError(
                            UiText.StringRes(Res.string.error_tray_icon_required_whatsapp)
                        )
                    )
                    return@launch
                }
                _effect.send(PackDetailEffect.LaunchAddToWhatsApp(packId, pack.name))
            } catch (e: Exception) {
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_add_pack)
                    )
                )
            }
        }
    }

    private fun refreshCloudShareLinks() {
        viewModelScope.launch {
            val pack = _state.value.pack ?: return@launch
            val cloudId = pack.cloudId
            if (cloudId.isNullOrBlank()) {
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString("Pack belum tersinkron ke cloud.")))
                return@launch
            }
            _state.update { it.copy(cloudShareLinksLoading = true) }
            runCatching {
                exploreApiRepository.getPackLinks(cloudId)
            }.onSuccess { links ->
                _state.update { it.copy(cloudShareLinksLoading = false, cloudShareLinks = links) }
            }.onFailure { error ->
                _state.update { it.copy(cloudShareLinksLoading = false) }
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to load cloud links")))
            }
        }
    }

    private fun createCloudShareLink() {
        viewModelScope.launch {
            val pack = _state.value.pack ?: return@launch
            val cloudId = pack.cloudId
            if (cloudId.isNullOrBlank()) {
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString("Pack belum tersinkron ke cloud.")))
                return@launch
            }
            _state.update { it.copy(cloudShareLinksLoading = true) }
            runCatching {
                exploreApiRepository.createPackLink(cloudId, CreateStickerPackLinkRequest())
            }.onSuccess { link ->
                val shareUrl = link.preferredShareText()
                _state.update {
                    it.copy(
                        cloudShareLinksLoading = false,
                        cloudShareLinks = listOf(link) + it.cloudShareLinks
                    )
                }
                _effect.send(PackDetailEffect.ShareText(shareUrl))
            }.onFailure { error ->
                _state.update { it.copy(cloudShareLinksLoading = false) }
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to create cloud link")))
            }
        }
    }

    private fun revokeCloudShareLink(linkId: String) {
        viewModelScope.launch {
            val pack = _state.value.pack ?: return@launch
            val cloudId = pack.cloudId
            if (cloudId.isNullOrBlank()) {
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString("Pack belum tersinkron ke cloud.")))
                return@launch
            }
            _state.update { it.copy(cloudShareLinksLoading = true) }
            runCatching {
                exploreApiRepository.revokePackLink(cloudId, linkId)
            }.onSuccess {
                _state.update {
                    it.copy(
                        cloudShareLinksLoading = false,
                        cloudShareLinks = it.cloudShareLinks.filterNot { link -> link.id == linkId }
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(cloudShareLinksLoading = false) }
                _effect.send(PackDetailEffect.ShowError(UiText.DynamicString(error.message ?: "Failed to revoke link")))
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            if (_state.value.isDeleting) return@launch
            try {
                _state.update { it.copy(isDeleting = true) }
                repository.deletePack(packId)
                _effect.send(PackDetailEffect.NavigateBack)
            } catch (e: Exception) {
                _state.update { it.copy(isDeleting = false) }
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_pack)
                    )
                )
            }
        }
    }

    private fun deleteSticker(index: Int) {
        viewModelScope.launch {
            if (_state.value.isDeleting) return@launch
            try {
                val pack = _state.value.pack ?: return@launch
                _state.update { it.copy(isDeleting = true) }
                repository.removeStickerFromPack(pack.identifier, index)
                loadPack(pack.identifier)
            } catch (e: Exception) {
                _state.update { it.copy(isDeleting = false) }
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_sticker)
                    )
                )
            }
        }
    }

    private fun stageStickerImports(imagePaths: List<String>) {
        val filtered = imagePaths.filter { it.isNotBlank() }
        if (filtered.isEmpty()) return
        _state.update {
            it.copy(
                stickerImportQueue = filtered,
                stickerImportBatchTotal = filtered.size
            )
        }
    }

    private fun dismissStickerImport() {
        _state.update {
            it.copy(
                stickerImportQueue = emptyList(),
                stickerImportBatchTotal = 0
            )
        }
    }

    private fun applyCroppedStickerImport(croppedPath: String) {
        viewModelScope.launch {
            val pack = _state.value.pack ?: return@launch
            val queue = _state.value.stickerImportQueue
            if (queue.isEmpty()) return@launch
            try {
                val fileName = "sticker_${pack.identifier}_${Clock.System.now().toEpochMilliseconds()}.webp"
                val savedPath = fileStorage.saveStickerImage(croppedPath, fileName)

                val sticker = Sticker(
                    imageFile = savedPath,
                    emojis = listOf("\u2b50"),
                    accessibilityText = null
                )

                repository.addStickerToPack(pack.identifier, sticker)
                val remaining = queue.drop(1)
                val batchTotal = _state.value.stickerImportBatchTotal
                _state.update { it.copy(stickerImportQueue = remaining) }
                loadPack(pack.identifier)
                if (remaining.isEmpty()) {
                    _state.update { it.copy(stickerImportBatchTotal = 0) }
                    _effect.send(
                        PackDetailEffect.ShowSuccess(
                            UiText.StringRes(Res.string.success_stickers_added, listOf(batchTotal))
                        )
                    )
                }
            } catch (e: Exception) {
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_add_stickers)
                    )
                )
            }
        }
    }
}
