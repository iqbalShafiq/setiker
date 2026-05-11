package presentation.createpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
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
import presentation.common.UiText
import presentation.common.toUiText
import kotlin.random.Random
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_generate_sticker
import setiker.composeapp.generated.resources.error_failed_save_pack
import setiker.composeapp.generated.resources.error_failed_split_grid
import setiker.composeapp.generated.resources.error_grid_image_required
import setiker.composeapp.generated.resources.error_pack_max_stickers
import setiker.composeapp.generated.resources.error_pack_name_required
import setiker.composeapp.generated.resources.error_partial_generate_not_added
import setiker.composeapp.generated.resources.error_partial_split_not_added
import setiker.composeapp.generated.resources.error_prompt_required
import setiker.composeapp.generated.resources.error_publisher_required
import setiker.composeapp.generated.resources.error_sticker_limit_reached
import setiker.composeapp.generated.resources.error_tray_icon_required

class CreatePackViewModel(
    private val repository: StickerRepository,
    private val fileStorage: StickerFileStorage,
    private val apiRepository: StickerApiRepository
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
                _state.update { current ->
                    if (current.stickers.size >= StickerPack.MAX_STICKERS) current
                    else current.copy(stickers = current.stickers + DraftSticker(intent.imagePath))
                }
            }
            is CreatePackIntent.AddAnimatedDraft -> {
                _state.update { current ->
                    if (current.stickers.size >= StickerPack.MAX_STICKERS) current
                    else current.copy(stickers = current.stickers + intent.draft)
                }
            }
            is CreatePackIntent.RemoveSticker -> {
                _state.update {
                    it.copy(stickers = it.stickers.filterIndexed { index, _ -> index != intent.index })
                }
            }
            is CreatePackIntent.UpdateGeneratePrompt -> {
                _state.update { it.copy(generatePrompt = intent.prompt) }
            }
            is CreatePackIntent.ToggleGenerateAsGrid -> {
                _state.update { it.copy(generateAsGrid = intent.enabled) }
            }
            is CreatePackIntent.UpdateGridLayout -> {
                _state.update { it.copy(gridLayout = intent.layout) }
            }
            is CreatePackIntent.ToggleNormalize -> {
                _state.update { it.copy(normalizeOutput = intent.enabled) }
            }
            is CreatePackIntent.UpdateGenerateInputImage -> {
                _state.update { it.copy(generateInputImage = intent.path) }
            }
            is CreatePackIntent.UpdateGridSplitSource -> {
                _state.update { it.copy(gridSplitSourcePath = intent.path) }
            }
            is CreatePackIntent.GenerateStickers -> generateStickers()
            is CreatePackIntent.ToggleGeneratedSelection -> {
                _state.update {
                    val current = it.selectedGeneratedPreview
                    val next = if (current.contains(intent.index)) {
                        current - intent.index
                    } else {
                        current + intent.index
                    }
                    it.copy(selectedGeneratedPreview = next)
                }
            }
            is CreatePackIntent.AddSelectedGeneratedToPack -> addSelectedGeneratedToPack()
            is CreatePackIntent.CloseGeneratedSheet -> {
                _state.update {
                    it.copy(
                        generatedPreview = emptyList(),
                        selectedGeneratedPreview = emptySet()
                    )
                }
            }
            is CreatePackIntent.RunGridSplit -> runGridSplit()
            is CreatePackIntent.ToggleSplitSelection -> {
                _state.update {
                    val current = it.selectedSplitPreview
                    val next = if (current.contains(intent.index)) {
                        current - intent.index
                    } else {
                        current + intent.index
                    }
                    it.copy(selectedSplitPreview = next)
                }
            }
            is CreatePackIntent.AddSelectedSplitToPack -> addSelectedSplitToPack()
            is CreatePackIntent.ClearSplitPreview -> {
                _state.update {
                    it.copy(splitPreview = emptyList(), selectedSplitPreview = emptySet())
                }
            }
            is CreatePackIntent.OpenAiGenerateSheet -> {
                _state.update { it.copy(aiGenerateSheetOpen = true) }
            }
            is CreatePackIntent.CloseAiGenerateSheet -> {
                _state.update { it.copy(aiGenerateSheetOpen = false) }
            }
            is CreatePackIntent.OpenGridConfirmSheet -> {
                _state.update { it.copy(gridSplitSheetPhase = GridSplitSheetPhase.ConfirmPick) }
            }
            is CreatePackIntent.CloseGridSheet -> {
                _state.update {
                    it.copy(
                        gridSplitSheetPhase = GridSplitSheetPhase.Hidden,
                        generatedPreview = emptyList(),
                        selectedGeneratedPreview = emptySet(),
                        splitPreview = emptyList(),
                        selectedSplitPreview = emptySet(),
                        gridSplitSourcePath = ""
                    )
                }
            }
            is CreatePackIntent.SavePack -> savePack()
            is CreatePackIntent.LoadPack -> loadPack(intent.packId)
            is CreatePackIntent.StageStickerGalleryPick -> {
                _state.update { it.copy(pendingStickerGalleryPath = intent.path) }
            }
            is CreatePackIntent.DismissStickerGalleryCropPrompt -> {
                _state.update { it.copy(pendingStickerGalleryPath = null) }
            }
            is CreatePackIntent.StageTrayGalleryPick -> {
                _state.update { it.copy(pendingTrayGalleryPath = intent.path) }
            }
            is CreatePackIntent.DismissTrayGalleryCropPrompt -> {
                _state.update { it.copy(pendingTrayGalleryPath = null) }
            }
        }
    }

    private fun loadPack(packId: String) {
        viewModelScope.launch {
            val current = _state.value
            if (current.isEditing && current.packId == packId) {
                return@launch
            }
            _state.update { it.copy(isLoading = true) }
            try {
                val pack = repository.getPack(packId)
                val fromServer = pack.stickers.map { sticker ->
                    DraftSticker(
                        imagePath = if (sticker.isAnimated) sticker.imageFile else (sticker.sourceImageFile ?: sticker.imageFile),
                        decorations = sticker.decorations,
                        isAnimated = sticker.isAnimated,
                        sourceVideoFile = sticker.sourceVideoFile,
                        frameDecorations = sticker.frameDecorations
                    )
                }
                val serverPaths = pack.stickers
                    .flatMap { listOfNotNull(it.sourceImageFile, it.imageFile) }
                    .toSet()
                val previousSession = _state.value.stickers.filter { it.imagePath.isNotBlank() }
                val mergedStickers = buildList {
                    addAll(fromServer)
                    for (local in previousSession) {
                        if (local.imagePath !in serverPaths && none { it.imagePath == local.imagePath }) {
                            add(local)
                        }
                    }
                }
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = pack.name,
                        publisher = pack.publisher,
                        trayImagePath = pack.trayImageFile,
                        stickers = mergedStickers,
                        isEditing = true,
                        packId = pack.identifier,
                        pendingStickerGalleryPath = null,
                        pendingTrayGalleryPath = null
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
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_pack_name_required)))
                return@launch
            }
            
            if (currentState.publisher.isBlank()) {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_publisher_required)))
                return@launch
            }
            
            if (currentState.trayImagePath.isBlank()) {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_tray_icon_required)))
                return@launch
            }

            if (currentState.stickers.size > StickerPack.MAX_STICKERS) {
                _effect.send(
                    CreatePackEffect.ShowError(
                        UiText.StringRes(Res.string.error_pack_max_stickers, listOf(StickerPack.MAX_STICKERS))
                    )
                )
                return@launch
            }

            try {
                val identifier = if (currentState.isEditing && currentState.packId.isNotBlank()) {
                    currentState.packId
                } else {
                    sanitizePackIdentifier(currentState.name, Random.nextInt(1000, 9999))
                }
                
                // Use a unique filename on every save so AsyncImage/Coil and the
                // WhatsApp content provider see a new URI when the tray icon is
                // changed. Reusing tray_<identifier>.png overwrote bytes at the
                // same path, which made the UI look like the update failed due to
                // image caching.
                val trayFileName = "tray_${identifier}_${System.currentTimeMillis()}.png"
                val trayPath = fileStorage.saveTrayImage(currentState.trayImagePath, trayFileName)
                
                // Pack is animated whenever it contains at least one animated sticker. WhatsApp
                // packs cannot mix static + animated WebP files, so when the pack ends up animated
                // we re-encode any static drafts as 1-frame animated WebP.
                val packIsAnimated = currentState.containsAnimated

                val stickers = currentState.stickers.mapIndexed { index, draft ->
                    when {
                        draft.isAnimated -> Sticker(
                            imageFile = draft.imagePath,
                            sourceImageFile = null,
                            emojis = listOf("⭐"),
                            decorations = draft.decorations,
                            isAnimated = true,
                            sourceVideoFile = draft.sourceVideoFile,
                            frameDecorations = draft.frameDecorations
                        )
                        packIsAnimated -> {
                            val animatedFileName = "sticker_${identifier}_${index}_anim.webp"
                            val animatedPath = fileStorage.encodeSingleFrameAnimatedWebP(
                                sourcePath = draft.imagePath,
                                fileName = animatedFileName,
                                decorations = draft.decorations
                            )
                            Sticker(
                                imageFile = animatedPath,
                                sourceImageFile = null,
                                emojis = listOf("⭐"),
                                decorations = draft.decorations,
                                isAnimated = true
                            )
                        }
                        else -> {
                            val baseFileName = "sticker_${identifier}_${index}_base.webp"
                            val basePath = fileStorage.saveStickerImage(
                                sourcePath = draft.imagePath,
                                fileName = baseFileName
                            )
                            val previewPath = if (draft.decorations.isEmpty()) {
                                basePath
                            } else {
                                fileStorage.saveStickerImageWithDecorations(
                                    sourcePath = basePath,
                                    fileName = "sticker_${identifier}_${index}_preview.webp",
                                    decorations = draft.decorations
                                )
                            }
                            Sticker(
                                imageFile = previewPath,
                                sourceImageFile = basePath,
                                emojis = listOf("⭐"),
                                decorations = draft.decorations
                            )
                        }
                    }
                }

                val pack = StickerPack(
                    identifier = identifier,
                    name = currentState.name,
                    publisher = currentState.publisher,
                    trayImageFile = trayPath,
                    stickers = stickers,
                    isAnimated = packIsAnimated
                )
                
                repository.savePack(pack)
                _effect.send(CreatePackEffect.PackSaved(identifier))
            } catch (e: Exception) {
                _effect.send(
                    CreatePackEffect.ShowError(
                        e.toUiText(Res.string.error_failed_save_pack)
                    )
                )
            }
        }
    }

    private fun generateStickers() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.generatePrompt.isBlank()) {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_prompt_required)))
                return@launch
            }
            if (currentState.stickers.size >= StickerPack.MAX_STICKERS) {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_sticker_limit_reached)))
                return@launch
            }

            _state.update { it.copy(isApiLoading = true, error = null) }
            try {
                val generated = apiRepository.generate(
                    prompt = currentState.generatePrompt,
                    grid = currentState.generateAsGrid,
                    layout = if (currentState.generateAsGrid) currentState.gridLayout else null,
                    normalize = if (currentState.generateAsGrid) currentState.normalizeOutput else null,
                    inputImagePath = currentState.generateInputImage
                )
                _state.update {
                    it.copy(
                        isApiLoading = false,
                        aiGenerateSheetOpen = false,
                        generatedPreview = generated,
                        selectedGeneratedPreview = generated.indices.toSet()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isApiLoading = false, error = e.message) }
                _effect.send(
                    CreatePackEffect.ShowError(
                        e.toUiText(Res.string.error_failed_generate_sticker)
                    )
                )
            }
        }
    }

    private fun runGridSplit() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.gridSplitSourcePath.isBlank()) {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_grid_image_required)))
                return@launch
            }

            _state.update { it.copy(isApiLoading = true, error = null) }
            try {
                val splitImages = apiRepository.splitGrid(
                    imagePath = currentState.gridSplitSourcePath
                )
                val splitDrafts = splitImages.map { file ->
                    DraftSticker(imagePath = file.localPath, decorations = file.decorations)
                }
                _state.update {
                    it.copy(
                        isApiLoading = false,
                        gridSplitSheetPhase = GridSplitSheetPhase.Results,
                        splitPreview = splitDrafts,
                        selectedSplitPreview = splitDrafts.indices.toSet()
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isApiLoading = false,
                        gridSplitSheetPhase = GridSplitSheetPhase.ConfirmPick,
                        error = e.message ?: "Split failed. Please try again."
                    )
                }
                _effect.send(
                    CreatePackEffect.ShowError(
                        e.toUiText(Res.string.error_failed_split_grid)
                    )
                )
            }
        }
    }

    private fun addSelectedSplitToPack() {
        val current = _state.value
        if (current.selectedSplitPreview.isEmpty()) return
        if (current.stickers.size >= StickerPack.MAX_STICKERS) {
            viewModelScope.launch {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_sticker_limit_reached)))
            }
            return
        }

        val selected = current.splitPreview.filterIndexed { index, _ ->
            current.selectedSplitPreview.contains(index)
        }
        _state.update {
            it.copy(
                stickers = (it.stickers + selected).take(StickerPack.MAX_STICKERS),
                splitPreview = emptyList(),
                selectedSplitPreview = emptySet(),
                gridSplitSheetPhase = GridSplitSheetPhase.Hidden,
                gridSplitSourcePath = ""
            )
        }
        if (current.stickers.size + selected.size > StickerPack.MAX_STICKERS) {
            viewModelScope.launch {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_partial_split_not_added)))
            }
        }
    }

    /**
     * Produce a pack identifier that satisfies WhatsApp's `StickerPackValidator.checkStringValidity`:
     * only `[a-zA-Z0-9_\-.,'\s]` is allowed, and the identifier must not contain `..`. Any other
     * character (emoji, accented letter, slash, …) is replaced with `_` so users can name packs
     * freely without breaking the export. We also collapse the result so it stays under the
     * 128-char `CHAR_COUNT_MAX` limit even after appending the random suffix.
     */
    private fun sanitizePackIdentifier(rawName: String, suffix: Int): String {
        val allowed = Regex("[^A-Za-z0-9_\\-.,' ]")
        val cleaned = rawName.lowercase()
            .replace(allowed, "_")
            .replace("..", "_")
            .replace(Regex("_+"), "_")
            .trim('_', ' ')
        val base = cleaned.ifBlank { "pack" }.take(110)
        return "${base}_$suffix"
    }

    private fun addSelectedGeneratedToPack() {
        val current = _state.value
        if (current.selectedGeneratedPreview.isEmpty()) return
        if (current.stickers.size >= StickerPack.MAX_STICKERS) {
            viewModelScope.launch {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_sticker_limit_reached)))
            }
            return
        }

        val selected = current.generatedPreview.filterIndexed { index, _ ->
            current.selectedGeneratedPreview.contains(index)
        }.map { DraftSticker(it) }
        _state.update {
            it.copy(
                stickers = (it.stickers + selected).take(StickerPack.MAX_STICKERS),
                generatedPreview = emptyList(),
                selectedGeneratedPreview = emptySet()
            )
        }
        if (current.stickers.size + selected.size > StickerPack.MAX_STICKERS) {
            viewModelScope.launch {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_partial_generate_not_added)))
            }
        }
    }
}
