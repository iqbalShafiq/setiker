package presentation.createpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlin.random.Random

class CreatePackViewModel(
    private val repository: StickerRepository,
    private val fileStorage: StickerFileStorage
) : ViewModel() {

    private val _state = MutableStateFlow(CreatePackState())
    val state: StateFlow<CreatePackState> = _state.asStateFlow()

    private val _effect = Channel<CreatePackEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: CreatePackIntent) {
        when (intent) {
            is CreatePackIntent.UpdateName -> {
                _state.update { it.copy(name = intent.name) }
            }
            is CreatePackIntent.UpdatePublisher -> {
                _state.update { it.copy(publisher = intent.publisher) }
            }
            is CreatePackIntent.UpdateTrayImage -> {
                _state.update { it.copy(trayImagePath = intent.imagePath) }
            }
            is CreatePackIntent.AddSticker -> {
                _state.update { it.copy(stickers = it.stickers + intent.imagePath) }
            }
            is CreatePackIntent.RemoveSticker -> {
                _state.update {
                    it.copy(stickers = it.stickers.filterIndexed { index, _ -> index != intent.index })
                }
            }
            is CreatePackIntent.SavePack -> savePack()
            is CreatePackIntent.LoadPack -> loadPack(intent.packId)
        }
    }

    private fun loadPack(packId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val pack = repository.getPack(packId)
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = pack.name,
                        publisher = pack.publisher,
                        trayImagePath = pack.trayImageFile,
                        stickers = pack.stickers.map { sticker -> sticker.imageFile },
                        isEditing = true,
                        packId = pack.identifier
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun savePack() {
        viewModelScope.launch {
            val currentState = _state.value
            
            if (currentState.name.isBlank()) {
                _effect.send(CreatePackEffect.ShowError("Pack name is required"))
                return@launch
            }
            
            if (currentState.publisher.isBlank()) {
                _effect.send(CreatePackEffect.ShowError("Publisher name is required"))
                return@launch
            }
            
            if (currentState.trayImagePath.isBlank()) {
                _effect.send(CreatePackEffect.ShowError("Tray icon is required"))
                return@launch
            }
            
            if (currentState.stickers.size < StickerPack.MIN_STICKERS) {
                _effect.send(CreatePackEffect.ShowError("Pack must have at least ${StickerPack.MIN_STICKERS} stickers"))
                return@launch
            }

            if (currentState.stickers.size > StickerPack.MAX_STICKERS) {
                _effect.send(CreatePackEffect.ShowError("Pack can have at most ${StickerPack.MAX_STICKERS} stickers"))
                return@launch
            }

            try {
                val identifier = if (currentState.isEditing && currentState.packId.isNotBlank()) {
                    currentState.packId
                } else {
                    "${currentState.name.lowercase().replace(" ", "_")}_${Random.nextInt(1000, 9999)}"
                }
                
                // Save tray image to stickers directory (96x96, PNG, <50KB)
                val trayFileName = "tray_${identifier}.png"
                val trayPath = fileStorage.saveTrayImage(currentState.trayImagePath, trayFileName)
                
                // Save stickers to stickers directory (512x512, WebP, <100KB)
                val stickers = currentState.stickers.mapIndexed { index, imagePath ->
                    val stickerFileName = "sticker_${identifier}_${index}.webp"
                    val stickerPath = fileStorage.saveStickerImage(imagePath, stickerFileName)
                    Sticker(
                        imageFile = stickerPath,
                        emojis = listOf("⭐") // WhatsApp requires at least 1 emoji per sticker
                    )
                }
                
                val pack = StickerPack(
                    identifier = identifier,
                    name = currentState.name,
                    publisher = currentState.publisher,
                    trayImageFile = trayPath,
                    stickers = stickers
                )
                
                repository.savePack(pack)
                _effect.send(CreatePackEffect.PackSaved(identifier))
            } catch (e: Exception) {
                _effect.send(CreatePackEffect.ShowError(e.message ?: "Failed to save pack"))
            }
        }
    }
}
