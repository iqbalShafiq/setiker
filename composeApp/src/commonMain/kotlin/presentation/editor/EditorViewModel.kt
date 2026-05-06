package presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

class EditorViewModel(
    private val repository: StickerRepository,
    private val emojiPreferences: EmojiPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var packId: String = ""
    private var stickerIndex: Int? = null

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.UpdateImagePath -> {
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
        this.packId = packId
        this.stickerIndex = index

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val pack = repository.getPack(packId)
                val sticker = pack.stickers.getOrNull(index)

                if (sticker != null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            imagePath = sticker.imageFile,
                            emojis = sticker.emojis,
                            accessibilityText = sticker.accessibilityText ?: ""
                        )
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
                _effect.send(EditorEffect.ShowError("Please select an image"))
                return@launch
            }

            if (currentState.emojis.isEmpty()) {
                _effect.send(EditorEffect.ShowError("Please add at least one emoji"))
                return@launch
            }

            try {
                val sticker = Sticker(
                    imageFile = currentState.imagePath,
                    emojis = currentState.emojis,
                    accessibilityText = currentState.accessibilityText.ifBlank { null }
                )

                // Save recent emojis
                currentState.emojis.forEach { emoji ->
                    emojiPreferences.addRecentEmoji(emoji)
                }

                repository.addStickerToPack(packId, sticker)
                _effect.send(EditorEffect.StickerSaved)
            } catch (e: Exception) {
                _effect.send(EditorEffect.ShowError(e.message ?: "Failed to save sticker"))
            }
        }
    }
}