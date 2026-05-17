package presentation.sharepreview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthManager
import data.remote.CloudStickerRepository
import data.remote.ExploreApiRepository
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
import presentation.common.UiText

class SharePreviewViewModel(
    private val exploreApiRepository: ExploreApiRepository,
    private val cloudStickerRepository: CloudStickerRepository,
    private val stickerFileStorage: StickerFileStorage,
    private val stickerRepository: StickerRepository,
    private val authManager: AuthManager
) : ViewModel() {
    private val _state = MutableStateFlow(SharePreviewState())
    val state: StateFlow<SharePreviewState> = _state.asStateFlow()

    private val _effect = Channel<SharePreviewEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var token: String = ""

    fun onIntent(intent: SharePreviewIntent) {
        when (intent) {
            is SharePreviewIntent.Load -> load(intent.kind, intent.token)
            SharePreviewIntent.Accept -> accept()
            SharePreviewIntent.NavigateBack -> viewModelScope.launch { _effect.send(SharePreviewEffect.NavigateBack) }
        }
    }

    private fun load(kind: String, token: String) {
        if (this.token == token && !_state.value.isLoading) return
        this.token = token
        _state.update { it.copy(isLoading = true, error = null, kind = kind) }
        viewModelScope.launch {
            val result = runCatching {
                if (kind == "sticker") exploreApiRepository.previewStickerShare(token)
                else exploreApiRepository.previewPackShare(token)
            }
            result.onSuccess { preview ->
                if (preview is data.remote.model.SharePreviewStickerData) {
                    _state.update { it.copy(isLoading = false, stickerPreview = preview, packPreview = null) }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            packPreview = preview as data.remote.model.SharePreviewPackData,
                            stickerPreview = null
                        )
                    }
                }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load share preview") }
            }
        }
    }

    private fun accept() {
        viewModelScope.launch {
            if (!authManager.isAuthenticated()) {
                _effect.send(SharePreviewEffect.NavigateToLogin)
                return@launch
            }
            _state.update { it.copy(isAccepting = true) }
            val kind = _state.value.kind
            val result = runCatching {
                if (kind == "sticker") {
                    val accepted = exploreApiRepository.acceptStickerShare(token)
                    val sticker = accepted.sticker
                    val bytes = cloudStickerRepository.downloadBytes(sticker.url)
                    val stickerPath = stickerFileStorage.saveBytes(
                        bytes = bytes,
                        fileName = "shared_sticker_${sticker.id}_${Clock.System.now().toEpochMilliseconds()}.png"
                    )
                    val pack = StickerPack(
                        identifier = "shared_${sticker.id}_${Clock.System.now().toEpochMilliseconds()}",
                        name = "Shared Sticker",
                        publisher = sticker.owner?.displayName ?: sticker.owner?.username ?: "Shared",
                        trayImageFile = stickerPath,
                        stickers = listOf(Sticker(imageFile = stickerPath, sourceImageFile = stickerPath, emojis = listOf("\u2B50"))),
                        visibility = "PRIVATE"
                    )
                    stickerRepository.savePack(pack)
                    pack.identifier
                } else {
                    val accepted = exploreApiRepository.acceptPackShare(token)
                    val pack = accepted.stickerPack
                    val stickers = mutableListOf<Sticker>()
                    pack.stickers.sortedBy { it.order }.forEachIndexed { index, relation ->
                        val remoteSticker = relation.sticker ?: return@forEachIndexed
                        val bytes = cloudStickerRepository.downloadBytes(remoteSticker.url)
                        val path = stickerFileStorage.saveBytes(
                            bytes = bytes,
                            fileName = "shared_pack_${pack.id}_${index}_${Clock.System.now().toEpochMilliseconds()}.png"
                        )
                        stickers += Sticker(imageFile = path, sourceImageFile = path, emojis = listOf("\u2B50"))
                    }
                    val localPack = StickerPack(
                        identifier = "shared_pack_${pack.id}_${Clock.System.now().toEpochMilliseconds()}",
                        name = pack.name,
                        publisher = pack.owner?.displayName ?: pack.owner?.username ?: "Shared",
                        trayImageFile = stickers.firstOrNull()?.imageFile.orEmpty(),
                        stickers = stickers,
                        cloudId = pack.id,
                        cloudOwnerId = pack.ownerId,
                        visibility = "PRIVATE"
                    )
                    stickerRepository.savePack(localPack)
                    localPack.identifier
                }
            }
            result.onSuccess { localPackId ->
                _state.update { it.copy(isAccepting = false) }
                _effect.send(SharePreviewEffect.NavigateToLocalPack(localPackId))
            }.onFailure { error ->
                _state.update { it.copy(isAccepting = false) }
                _effect.send(SharePreviewEffect.ShowMessage(UiText.DynamicString(error.message ?: "Failed to accept share")))
            }
        }
    }
}
