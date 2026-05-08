package presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import domain.model.Sticker
import domain.repository.StickerRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_save_sticker
import setiker.composeapp.generated.resources.error_pack_id_missing
import setiker.composeapp.generated.resources.error_select_image

class EditorViewModel(
    private val repository: StickerRepository,
    private val emojiPreferences: EmojiPreferences,
    private val fileStorage: StickerFileStorage
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var packId: String = ""
    private var stickerIndex: Int? = null
    private var loadedPackId: String? = null
    private var loadedStickerIndex: Int? = null

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.UpdateImagePath -> {
                android.util.Log.d("EditorViewModel", "UpdateImagePath: ${intent.path}")
                _state.update { it.copy(imagePath = intent.path) }
            }
            is EditorIntent.AddEmoji -> {
                if (_state.value.emojis.size < Sticker.MAX_EMOJIS) {
                    _state.update { it.copy(emojis = it.emojis + intent.emoji) }
                }
            }
            is EditorIntent.RemoveEmoji -> {
                _state.update {
                    it.copy(emojis = it.emojis.filterIndexed { index, _ -> index != intent.index })
                }
            }
            is EditorIntent.UpdateAccessibilityText -> {
                _state.update { it.copy(accessibilityText = intent.text) }
            }
            is EditorIntent.SaveSticker -> saveSticker()
            is EditorIntent.NavigateToCrop -> {
                viewModelScope.launch {
                    _effect.send(EditorEffect.NavigateToCrop(_state.value.imagePath))
                }
            }
            is EditorIntent.NavigateToBackgroundRemover -> {
                viewModelScope.launch {
                    _effect.send(EditorEffect.NavigateToBackgroundRemover(_state.value.imagePath))
                }
            }
            is EditorIntent.SetPackId -> {
                this.packId = intent.packId
                intent.stickerIndex?.let { this.stickerIndex = it }
                _state.update { it.copy(packId = intent.packId, stickerIndex = intent.stickerIndex) }
                android.util.Log.d("EditorViewModel", "SetPackId: ${intent.packId}, stickerIndex: ${intent.stickerIndex}")
            }
            is EditorIntent.LoadSticker -> loadSticker(intent.stickerIndex, intent.packId)
            is EditorIntent.ShowEmojiPicker -> {
                viewModelScope.launch {
                    val recent = emojiPreferences.getRecentEmojis()
                    _state.update { it.copy(showEmojiPicker = true, recentEmojis = recent) }
                }
            }
            is EditorIntent.HideEmojiPicker -> {
                _state.update { it.copy(showEmojiPicker = false) }
            }
            is EditorIntent.LoadRecentEmojis -> {
                _state.update { it.copy(recentEmojis = intent.emojis) }
            }
        }
    }

    private fun loadSticker(index: Int, packId: String) {
        // Prevent reloading the same sticker to avoid overwriting edited image paths
        if (loadedPackId == packId && loadedStickerIndex == index) {
            android.util.Log.d("EditorViewModel", "Sticker already loaded: index=$index, packId=$packId")
            return
        }

        this.packId = packId
        this.stickerIndex = index
        this.loadedPackId = packId
        this.loadedStickerIndex = index

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, stickerIndex = index) }
            try {
                val pack = repository.getPack(packId)
                val sticker = pack.stickers.getOrNull(index)

                if (sticker != null) {
                    val currentImagePath = _state.value.imagePath
                    val isPathModified = currentImagePath.isNotBlank() && currentImagePath != sticker.imageFile

                    _state.update {
                        it.copy(
                            isLoading = false,
                            imagePath = if (isPathModified) currentImagePath else sticker.imageFile,
                            emojis = sticker.emojis,
                            accessibilityText = sticker.accessibilityText ?: ""
                        )
                    }
                    if (isPathModified) {
                        android.util.Log.d("EditorViewModel", "Preserving modified image path: $currentImagePath")
                    } else {
                        android.util.Log.d("EditorViewModel", "Loaded sticker: ${sticker.imageFile}")
                    }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun saveSticker() {
        viewModelScope.launch {
            val currentState = _state.value

            if (currentState.imagePath.isBlank()) {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
                return@launch
            }

            val effectivePackId = currentState.packId.ifBlank { packId }
            
            if (effectivePackId.isBlank()) {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_pack_id_missing)))
                android.util.Log.e("EditorViewModel", "Cannot save sticker: packId is blank! state.packId='${currentState.packId}', field.packId='$packId'")
                return@launch
            }

            try {
                android.util.Log.d("EditorViewModel", "Saving sticker with packId: '$effectivePackId'")
                
                // Save image to stickers directory (512x512, WebP, <100KB)
                val fileName = "sticker_${effectivePackId}_${System.currentTimeMillis()}.webp"
                val savedPath = fileStorage.saveStickerImage(currentState.imagePath, fileName)
                
                val sticker = Sticker(
                    imageFile = savedPath,
                    emojis = currentState.emojis,
                    accessibilityText = currentState.accessibilityText.ifBlank { null }
                )

                // Save recent emojis
                currentState.emojis.forEach { emoji ->
                    emojiPreferences.addRecentEmoji(emoji)
                }

                // Update existing sticker or add new one
                val index = currentState.stickerIndex
                android.util.Log.d("EditorViewModel", "Saving sticker - index from state: $index, field: $stickerIndex")
                if (index != null) {
                    repository.updateStickerInPack(effectivePackId, index, sticker)
                    android.util.Log.d("EditorViewModel", "Updated sticker at index $index")
                } else {
                    repository.addStickerToPack(effectivePackId, sticker)
                    android.util.Log.d("EditorViewModel", "Added new sticker")
                }
                _effect.send(EditorEffect.StickerSaved)
            } catch (e: Exception) {
                android.util.Log.e("EditorViewModel", "Failed to save sticker: ${e.message}", e)
                _effect.send(
                    EditorEffect.ShowError(
                        e.message?.let(UiText::DynamicString)
                            ?: UiText.StringRes(Res.string.error_failed_save_sticker)
                    )
                )
            }
        }
    }
}