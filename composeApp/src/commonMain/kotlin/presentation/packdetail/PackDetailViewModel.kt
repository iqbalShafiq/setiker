package presentation.packdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import domain.actions.PackActions
import domain.model.StickerPack
import domain.repository.StickerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PackDetailViewModel(
    private val repository: StickerRepository,
    private val packActions: PackActions
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
                            "Pack must have at least ${StickerPack.MIN_STICKERS} stickers"
                        )
                    )
                    return@launch
                }
                _effect.send(PackDetailEffect.LaunchAddToWhatsApp(packId, pack.name))
            } catch (e: Exception) {
                _effect.send(PackDetailEffect.ShowError(e.message ?: "Failed to add pack"))
            }
        }
    }

    private fun sharePack(packId: String) {
        viewModelScope.launch {
            try {
                packActions.sharePack(packId)
                _effect.send(PackDetailEffect.ShowSuccess("Pack shared successfully"))
            } catch (e: Exception) {
                _effect.send(PackDetailEffect.ShowError(e.message ?: "Failed to share pack"))
            }
        }
    }

    private fun deletePack(packId: String) {
        viewModelScope.launch {
            try {
                repository.deletePack(packId)
                _effect.send(PackDetailEffect.NavigateBack)
            } catch (e: Exception) {
                _effect.send(PackDetailEffect.ShowError(e.message ?: "Failed to delete pack"))
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
                _effect.send(PackDetailEffect.ShowError(e.message ?: "Failed to delete sticker"))
            }
        }
    }
}
