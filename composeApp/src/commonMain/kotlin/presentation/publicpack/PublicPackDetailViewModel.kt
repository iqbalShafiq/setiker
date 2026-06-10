package presentation.publicpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.remote.CloudStickerRepository
import data.remote.ExploreApiRepository
import data.remote.model.applySocialUpdate
import data.remote.model.userHasLiked
import data.remote.model.userHasSaved
import data.storage.StickerFileStorage
import domain.model.Sticker
import domain.model.StickerPack
import domain.model.AiQuotaOperation
import domain.repository.AiQuotaRepository
import domain.repository.StickerRepository
import presentation.common.AiQuotaGate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import presentation.common.UiText

class PublicPackDetailViewModel(
    private val exploreApiRepository: ExploreApiRepository,
    private val cloudStickerRepository: CloudStickerRepository,
    private val stickerFileStorage: StickerFileStorage,
    private val stickerRepository: StickerRepository,
    private val authManager: AuthManager,
    private val aiQuotaRepository: AiQuotaRepository
) : ViewModel() {
    private val _state = MutableStateFlow(PublicPackDetailState())
    val state: StateFlow<PublicPackDetailState> = _state.asStateFlow()

    private val _effect = Channel<PublicPackDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var currentPackId: String = ""

    fun onIntent(intent: PublicPackDetailIntent) {
        when (intent) {
            is PublicPackDetailIntent.Load -> load(intent.packId)
            PublicPackDetailIntent.ToggleLike -> toggleLike()
            PublicPackDetailIntent.ToggleSave -> toggleSave()
            PublicPackDetailIntent.ToggleFollowCreator -> toggleFollow()
            PublicPackDetailIntent.ImportPack -> prepareImport()
            PublicPackDetailIntent.ConfirmImport -> importPack()
            PublicPackDetailIntent.DismissImportDialog -> {
                _state.update { it.copy(showImportDialog = false) }
            }
            PublicPackDetailIntent.NavigateBack -> viewModelScope.launch { _effect.send(PublicPackDetailEffect.NavigateBack) }
            is PublicPackDetailIntent.OpenCreator -> viewModelScope.launch {
                _effect.send(PublicPackDetailEffect.NavigateToCreator(intent.userId))
            }
        }
    }

    private fun load(packId: String) {
        if (_state.value.pack?.id == packId && !_state.value.isLoading) return
        currentPackId = packId
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, loadFailed = false, error = null) }
            runCatching {
                exploreApiRepository.getPublicPackDetail(packId)
            }.onSuccess { pack ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = false,
                        pack = pack,
                        isLiked = pack.userHasLiked(),
                        isSaved = pack.userHasSaved(),
                        isFollowingCreator = pack.isFollowingOwner ?: pack.following ?: false
                    )
                }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        loadFailed = true,
                        error = error.message ?: "Failed to load pack"
                    )
                }
            }
        }
    }

    private fun requireAuth(action: suspend () -> Unit) {
        viewModelScope.launch {
            if (!authManager.isAuthenticated()) {
                _effect.send(PublicPackDetailEffect.NavigateToLogin)
                return@launch
            }
            action()
        }
    }

    private fun toggleLike() = requireAuth {
        val snapshot = _state.value
        if (snapshot.isActionLoading) return@requireAuth
        _state.update { it.copy(isActionLoading = true) }
        val result = runCatching {
            if (snapshot.isLiked) exploreApiRepository.unlikePack(currentPackId)
            else exploreApiRepository.likePack(currentPackId)
        }
        result.onSuccess { social ->
            _state.update {
                val updatedPack = it.pack?.applySocialUpdate(social)
                it.copy(
                    isActionLoading = false,
                    isLiked = updatedPack?.userHasLiked() ?: it.isLiked,
                    isSaved = updatedPack?.userHasSaved() ?: it.isSaved,
                    pack = updatedPack
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(isActionLoading = false) }
            _effect.send(PublicPackDetailEffect.ShowMessage(UiText.DynamicString(error.message ?: "Like action failed")))
        }
    }

    private fun toggleSave() = requireAuth {
        val snapshot = _state.value
        if (snapshot.isActionLoading) return@requireAuth
        _state.update { it.copy(isActionLoading = true) }
        val result = runCatching {
            if (snapshot.isSaved) exploreApiRepository.unsavePack(currentPackId)
            else exploreApiRepository.savePack(currentPackId)
        }
        result.onSuccess { social ->
            _state.update {
                val updatedPack = it.pack?.applySocialUpdate(social)
                it.copy(
                    isActionLoading = false,
                    isLiked = updatedPack?.userHasLiked() ?: it.isLiked,
                    isSaved = updatedPack?.userHasSaved() ?: it.isSaved,
                    pack = updatedPack
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(isActionLoading = false) }
            _effect.send(PublicPackDetailEffect.ShowMessage(UiText.DynamicString(error.message ?: "Save action failed")))
        }
    }

    private fun toggleFollow() = requireAuth {
        val snapshot = _state.value
        val ownerId = snapshot.pack?.owner?.id ?: return@requireAuth
        if (snapshot.isActionLoading) return@requireAuth
        _state.update { it.copy(isActionLoading = true) }
        runCatching {
            if (snapshot.isFollowingCreator) exploreApiRepository.unfollowUser(ownerId)
            else exploreApiRepository.followUser(ownerId)
        }.onSuccess { followState ->
            _state.update {
                val owner = it.pack?.owner
                it.copy(
                    isActionLoading = false,
                    isFollowingCreator = followState.following,
                    pack = it.pack?.copy(
                        owner = owner?.copy(
                            followerCount = followState.followerCount,
                            followingCount = followState.followingCount
                        )
                    )
                )
            }
        }.onFailure { error ->
            _state.update { it.copy(isActionLoading = false) }
            _effect.send(PublicPackDetailEffect.ShowMessage(UiText.DynamicString(error.message ?: "Follow action failed")))
        }
    }

    private fun prepareImport() = requireAuth {
        viewModelScope.launch {
            val gateError = AiQuotaGate.checkCanStart(aiQuotaRepository, AiQuotaOperation.PACK_IMPORT, forceRefresh = true)
            if (gateError != null) {
                _effect.send(PublicPackDetailEffect.ShowMessage(gateError))
                return@launch
            }
            val usage = aiQuotaRepository.getUsage(forceRefresh = false)
            val cost = usage?.costFor(AiQuotaOperation.PACK_IMPORT) ?: 0
            _state.update {
                it.copy(
                    showImportDialog = true,
                    importPointCost = cost,
                    pointsRemaining = usage?.pointsRemaining ?: 0,
                    importOwnerCredit = cost
                )
            }
        }
    }

    private fun importPack() = requireAuth {
        val pack = _state.value.pack ?: return@requireAuth
        _state.update { it.copy(isActionLoading = true, showImportDialog = false) }
        runCatching {
            val result = exploreApiRepository.importPublicPack(pack.id)
            val localPack = buildLocalPack(result.pack)
            stickerRepository.savePack(localPack)
            exploreApiRepository.trackDownload(pack.id)
            Triple(localPack.identifier, result.ownerCredited, Unit)
        }.onSuccess { (localPackId, ownerCredited, _) ->
            _state.update { it.copy(isActionLoading = false, importOwnerCredit = ownerCredited) }
            _effect.send(PublicPackDetailEffect.NavigateToLocalPack(localPackId))
        }.onFailure { error ->
            _state.update { it.copy(isActionLoading = false) }
            _effect.send(PublicPackDetailEffect.ShowMessage(UiText.DynamicString(error.message ?: "Import failed")))
        }
    }

    private suspend fun buildLocalPack(pack: data.remote.model.CloudStickerPack): StickerPack {
        val stickers = mutableListOf<Sticker>()
        pack.stickers.sortedBy { it.order }.forEachIndexed { index, relation ->
            val sticker = relation.sticker ?: return@forEachIndexed
            val bytes = cloudStickerRepository.downloadBytes(sticker.url)
            val stickerPath = stickerFileStorage.saveBytes(
                bytes = bytes,
                fileName = "imported_${pack.id}_${index}_${Clock.System.now().toEpochMilliseconds()}.png"
            )
            stickers += Sticker(
                imageFile = stickerPath,
                sourceImageFile = stickerPath,
                emojis = listOf("\u2B50")
            )
        }
        val trayPath = stickers.firstOrNull()?.imageFile.orEmpty()
        return StickerPack(
            identifier = "public_${pack.id}_${Clock.System.now().toEpochMilliseconds()}",
            name = pack.name,
            publisher = pack.owner?.displayName ?: pack.owner?.username ?: "Creator",
            trayImageFile = trayPath,
            stickers = stickers,
            cloudId = pack.id,
            visibility = "PRIVATE",
            cloudOwnerId = pack.ownerId
        )
    }
}
