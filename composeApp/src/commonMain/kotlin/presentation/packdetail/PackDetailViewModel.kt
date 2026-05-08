package presentation.packdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.storage.StickerFileStorage
import domain.actions.PackActions
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
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_add_pack
import setiker.composeapp.generated.resources.error_failed_add_stickers
import setiker.composeapp.generated.resources.error_failed_delete_pack
import setiker.composeapp.generated.resources.error_failed_delete_sticker
import setiker.composeapp.generated.resources.error_failed_share_pack
import setiker.composeapp.generated.resources.error_pack_min_stickers_share
import setiker.composeapp.generated.resources.error_pack_min_stickers_whatsapp
import setiker.composeapp.generated.resources.success_pack_shared
import setiker.composeapp.generated.resources.success_stickers_added

class PackDetailViewModel(
    private val repository: StickerRepository,
    private val packActions: PackActions,
    private val fileStorage: StickerFileStorage
) : ViewModel() {

    private val _state = MutableStateFlow(PackDetailState())
    val state: StateFlow<PackDetailState> = _state.asStateFlow()

    private val _effect = Channel<PackDetailEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: PackDetailIntent) {
        when (intent) {
            is PackDetailIntent.LoadPack -> loadPack(intent.packId)
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
            is PackDetailIntent.AddMultipleStickers -> addMultipleStickers(intent.imagePaths)
            is PackDetailIntent.EditSticker -> {
                viewModelScope.launch {
                    _effect.send(PackDetailEffect.NavigateToEditSticker(intent.index))
                }
            }
            is PackDetailIntent.SharePack -> sharePack(intent.packId)
        }
    }

    private fun loadPack(packId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val pack = repository.getPack(packId)
                _state.update { it.copy(isLoading = false, pack = pack) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
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

    private fun sharePack(packId: String) {
        viewModelScope.launch {
            try {
                val pack = repository.getPack(packId)
                if (pack.stickers.size < StickerPack.MIN_STICKERS) {
                    _effect.send(
                        PackDetailEffect.ShowError(
                            UiText.StringRes(
                                Res.string.error_pack_min_stickers_share,
                                listOf(StickerPack.MIN_STICKERS)
                            )
                        )
                    )
                    return@launch
                }
                packActions.sharePack(packId)
                _effect.send(PackDetailEffect.ShowSuccess(UiText.StringRes(Res.string.success_pack_shared)))
            } catch (e: Exception) {
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_share_pack)
                    )
                )
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                _effect.send(PackDetailEffect.NavigateBack)
            } catch (e: Exception) {
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
            try {
                val pack = _state.value.pack ?: return@launch
                repository.removeStickerFromPack(pack.identifier, index)
                loadPack(pack.identifier)
            } catch (e: Exception) {
                _effect.send(
                    PackDetailEffect.ShowError(
                        e.toUiText(Res.string.error_failed_delete_sticker)
                    )
                )
            }
        }
    }

    private fun addMultipleStickers(imagePaths: List<String>) {
        viewModelScope.launch {
            try {
                val pack = _state.value.pack ?: return@launch
                
                var successCount = 0
                imagePaths.forEach { imagePath ->
                    try {
                        val fileName = "sticker_${pack.identifier}_${System.currentTimeMillis()}_${successCount}.webp"
                        val savedPath = fileStorage.saveStickerImage(imagePath, fileName)
                        
                        val sticker = Sticker(
                            imageFile = savedPath,
                            emojis = listOf("\u2b50"),
                            accessibilityText = null
                        )
                        
                        repository.addStickerToPack(pack.identifier, sticker)
                        successCount++
                    } catch (e: Exception) {
                        android.util.Log.e("PackDetailViewModel", "Failed to add sticker: ${e.message}")
                    }
                }
                
                if (successCount > 0) {
                    loadPack(pack.identifier)
                    _effect.send(
                        PackDetailEffect.ShowSuccess(
                            UiText.StringRes(Res.string.success_stickers_added, listOf(successCount))
                        )
                    )
                } else {
                    _effect.send(PackDetailEffect.ShowError(UiText.StringRes(Res.string.error_failed_add_stickers)))
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
