package presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
import data.storage.StickerFileStorage
import data.util.EmojiPreferences
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.repository.StickerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_apply_removal
import setiker.composeapp.generated.resources.error_failed_save_sticker
import setiker.composeapp.generated.resources.error_pack_id_missing
import setiker.composeapp.generated.resources.error_select_image

class EditorViewModel(
    private val repository: StickerRepository,
    private val emojiPreferences: EmojiPreferences,
    private val fileStorage: StickerFileStorage,
    private val apiRepository: StickerApiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var packId: String = ""
    private var stickerIndex: Int? = null
    private var loadedPackId: String? = null
    private var loadedStickerIndex: Int? = null
    private var backgroundRemovalJob: Job? = null

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.UpdateImagePath -> {
                android.util.Log.d("EditorViewModel", "UpdateImagePath: ${intent.path}")
                _state.update {
                    it.copy(
                        imagePath = intent.path,
                        decorations = emptyList(),
                        selectedDecorationId = null
                    )
                }
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
            is EditorIntent.RemoveBackground -> removeBackground()
            is EditorIntent.DismissBackgroundRemoverSheet -> dismissBackgroundRemoverSheet()
            is EditorIntent.ConfirmBackgroundRemoval -> confirmBackgroundRemoval()
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
            is EditorIntent.AddImageDecorationFromGallery -> addImageDecoration(intent.path)
            is EditorIntent.AddTextDecoration -> addTextDecoration(intent.text, intent.font)
            is EditorIntent.AddEmojiDecoration -> addEmojiDecoration(intent.emoji)
            is EditorIntent.UpdateDecorationTransform -> updateDecorationTransform(
                id = intent.id,
                centerX = intent.centerX,
                centerY = intent.centerY,
                scale = intent.scale
            )
            is EditorIntent.SelectDecoration -> {
                _state.update { it.copy(selectedDecorationId = intent.id) }
            }
            is EditorIntent.RemoveDecoration -> {
                _state.update {
                    it.copy(
                        decorations = it.decorations.filterNot { decoration -> decoration.id == intent.id },
                        selectedDecorationId = if (it.selectedDecorationId == intent.id) null else it.selectedDecorationId
                    )
                }
            }
            is EditorIntent.ShowDecorationEmojiPicker -> {
                viewModelScope.launch {
                    val recent = emojiPreferences.getRecentEmojis()
                    _state.update {
                        it.copy(
                            showDecorationEmojiPicker = true,
                            decorationEmojiPickerTargetId = intent.targetDecorationId,
                            recentEmojis = recent
                        )
                    }
                }
            }
            is EditorIntent.HideDecorationEmojiPicker -> {
                _state.update {
                    it.copy(
                        showDecorationEmojiPicker = false,
                        decorationEmojiPickerTargetId = null
                    )
                }
            }
            is EditorIntent.ShowTextDecorationSheet -> {
                _state.update { it.copy(isTextDecorationSheetOpen = true) }
            }
            is EditorIntent.HideTextDecorationSheet -> {
                _state.update { it.copy(isTextDecorationSheetOpen = false) }
            }
            is EditorIntent.UpdateTextDecorationText -> updateTextDecorationText(intent.id, intent.text)
            is EditorIntent.UpdateTextDecorationFont -> updateTextDecorationFont(intent.id, intent.font)
            is EditorIntent.UpdateTextDecorationFontWeight -> updateTextDecorationFontWeight(intent.id, intent.fontWeight)
            is EditorIntent.UpdateTextDecorationColor -> updateTextDecorationColor(intent.id, intent.colorArgb)
            is EditorIntent.UpdateEmojiDecorationValue -> updateEmojiDecorationValue(intent.id, intent.emoji)
            is EditorIntent.UpdateImageDecorationPath -> updateImageDecorationPath(intent.id, intent.imagePath)
        }
    }

    fun addImageDecoration(path: String) {
        if (path.isBlank()) return
        _state.update {
            val decoration = ImageDecoration(
                id = nextDecorationId(),
                imagePath = path,
                centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                centerY = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
            )
            it.copy(
                decorations = it.decorations + decoration,
                selectedDecorationId = decoration.id
            )
        }
    }

    private fun addTextDecoration(text: String, font: DecorationFont) {
        if (text.isBlank()) return
        _state.update {
            val decoration = TextDecoration(
                id = nextDecorationId(),
                text = text.trim(),
                font = font,
                fontWeight = DecorationFontWeight.Regular,
                textColorArgb = 0xFFFFFFFFL,
                centerX = 0.5f,
                centerY = 0.5f
            )
            it.copy(
                decorations = it.decorations + decoration,
                selectedDecorationId = decoration.id,
                isTextDecorationSheetOpen = false
            )
        }
    }

    private fun addEmojiDecoration(emoji: String) {
        if (emoji.isBlank()) return
        _state.update {
            val decoration = EmojiDecoration(
                id = nextDecorationId(),
                emoji = emoji,
                centerX = 0.5f,
                centerY = 0.5f
            )
            it.copy(
                decorations = it.decorations + decoration,
                selectedDecorationId = decoration.id,
                showDecorationEmojiPicker = false
            )
        }
    }

    private fun updateTextDecorationText(id: String, text: String) {
        if (text.isBlank()) return
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(text = text.trim())
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateTextDecorationFont(id: String, font: DecorationFont) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(font = font)
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateTextDecorationColor(id: String, colorArgb: Long) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(textColorArgb = colorArgb)
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateTextDecorationFontWeight(id: String, fontWeight: DecorationFontWeight) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(fontWeight = fontWeight)
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateEmojiDecorationValue(id: String, emoji: String) {
        if (emoji.isBlank()) return
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is EmojiDecoration && decoration.id == id) {
                        decoration.copy(emoji = emoji)
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateImageDecorationPath(id: String, imagePath: String) {
        if (imagePath.isBlank()) return
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is ImageDecoration && decoration.id == id) {
                        decoration.copy(imagePath = imagePath)
                    } else {
                        decoration
                    }
                }
            )
        }
    }

    private fun updateDecorationTransform(
        id: String,
        centerX: Float,
        centerY: Float,
        scale: Float
    ) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration.id != id) return@map decoration
                    decoration.withTransform(
                        centerX = centerX.coerceIn(0f, 1f),
                        centerY = centerY.coerceIn(0f, 1f),
                        scale = scale.coerceIn(0.3f, 4f)
                    )
                }
            )
        }
    }

    private fun removeBackground() {
        val path = _state.value.imagePath
        if (path.isBlank()) {
            viewModelScope.launch {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
            }
            return
        }

        backgroundRemovalJob?.cancel()
        backgroundRemovalJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    isBackgroundRemoverSheetOpen = true,
                    isBackgroundRemoving = true,
                    backgroundRemoverPreviewPath = null
                )
            }

            try {
                val resultPath = apiRepository.removeBackground(path)
                if (!isActive) return@launch
                _state.update {
                    it.copy(
                        isBackgroundRemoving = false,
                        backgroundRemoverPreviewPath = resultPath
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isActive) return@launch
                _state.update {
                    it.copy(
                        isBackgroundRemoverSheetOpen = false,
                        isBackgroundRemoving = false,
                        backgroundRemoverPreviewPath = null
                    )
                }
                _effect.send(
                    EditorEffect.ShowError(
                        e.toUiText(Res.string.error_failed_apply_removal)
                    )
                )
            } finally {
                backgroundRemovalJob = null
            }
        }
    }

    private fun dismissBackgroundRemoverSheet() {
        backgroundRemovalJob?.cancel()
        backgroundRemovalJob = null
        _state.update {
            it.copy(
                isBackgroundRemoverSheetOpen = false,
                isBackgroundRemoving = false,
                backgroundRemoverPreviewPath = null
            )
        }
    }

    private fun confirmBackgroundRemoval() {
        val preview = _state.value.backgroundRemoverPreviewPath ?: return
        _state.update {
            it.copy(
                imagePath = preview,
                isBackgroundRemoverSheetOpen = false,
                isBackgroundRemoving = false,
                backgroundRemoverPreviewPath = null
            )
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
                    val editableImagePath = sticker.sourceImageFile ?: sticker.imageFile
                    val currentImagePath = _state.value.imagePath
                    val isPathModified = currentImagePath.isNotBlank() && currentImagePath != editableImagePath

                    _state.update {
                        it.copy(
                            isLoading = false,
                            imagePath = if (isPathModified) currentImagePath else editableImagePath,
                            emojis = sticker.emojis,
                            accessibilityText = sticker.accessibilityText ?: "",
                            decorations = sticker.decorations,
                            selectedDecorationId = null
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
                
                // Save editable base image and flattened preview image separately
                val time = System.currentTimeMillis()
                val baseFileName = "sticker_${effectivePackId}_${time}_base.webp"
                val basePath = fileStorage.saveStickerImage(
                    sourcePath = currentState.imagePath,
                    fileName = baseFileName
                )
                val flattenedPath = if (currentState.decorations.isEmpty()) {
                    basePath
                } else {
                    fileStorage.saveStickerImageWithDecorations(
                        sourcePath = basePath,
                        fileName = "sticker_${effectivePackId}_${time}_preview.webp",
                        decorations = currentState.decorations
                    )
                }
                
                val sticker = Sticker(
                    imageFile = flattenedPath,
                    sourceImageFile = basePath,
                    emojis = currentState.emojis,
                    accessibilityText = currentState.accessibilityText.ifBlank { null },
                    decorations = currentState.decorations
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
                        e.toUiText(Res.string.error_failed_save_sticker)
                    )
                )
            }
        }
    }

    private fun StickerDecoration.withTransform(
        centerX: Float,
        centerY: Float,
        scale: Float
    ): StickerDecoration = when (this) {
        is TextDecoration -> copy(centerX = centerX, centerY = centerY, scale = scale)
        is EmojiDecoration -> copy(centerX = centerX, centerY = centerY, scale = scale)
        is ImageDecoration -> copy(centerX = centerX, centerY = centerY, scale = scale)
    }

    private fun nextDecorationId(): String = "dec_${System.currentTimeMillis()}_${Random.nextInt(1000, 9999)}"
}