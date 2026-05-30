package presentation.createpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.aijob.AiJobManager
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.GenerateStickersPayload
import domain.model.aijob.GridSplitPayload
import domain.model.aijob.ImproveStickersPayload
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.WorkspaceDraftKind
import domain.model.aijob.AiJobStatus
import domain.repository.AiJobRepository
import domain.repository.StickerRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import presentation.aijob.AiJobEnqueueHelper
import presentation.aijob.DraftResultApplier
import presentation.aijob.WorkspaceDraftFactory
import presentation.aijob.toDraftSticker
import presentation.aijob.toSnapshot
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.PackIdentifierSanitizer
import presentation.common.UiText
import presentation.common.toUiText
import kotlin.random.Random
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_generate_sticker
import setiker.composeapp.generated.resources.error_failed_improve_sticker
import setiker.composeapp.generated.resources.error_no_static_stickers_to_improve
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

@OptIn(ExperimentalUuidApi::class)
class CreatePackViewModel(
    private val repository: StickerRepository,
    private val apiRepository: StickerApiRepository,
    private val draftSaver: StickerPackDraftSaver,
    private val aiJobManager: AiJobManager,
    private val enqueueHelper: AiJobEnqueueHelper,
    private val draftResultApplier: DraftResultApplier,
    private val jobRepository: AiJobRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreatePackState())
    val state: StateFlow<CreatePackState> = _state.asStateFlow()

    private val _effect = Channel<CreatePackEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        observeWorkspaceDraftResults()
    }

    private fun observeWorkspaceDraftResults() {
        viewModelScope.launch {
            _state
                .map { it.workspaceDraftId }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { draftId ->
                    combine(
                        draftResultApplier.observeDraft(draftId),
                        jobRepository.observeByDraft(draftId)
                    ) { draft, jobs ->
                        val active = jobs.firstOrNull {
                            it.status in setOf(
                                AiJobStatus.QUEUED,
                                AiJobStatus.RUNNING,
                                AiJobStatus.WAITING_FOR_NETWORK,
                                AiJobStatus.CHECKPOINTED
                            )
                        }
                        val completed = jobs.firstOrNull { it.status == AiJobStatus.COMPLETED }
                        Triple(draft, active, completed)
                    }.collect { (draft, active, completed) ->
                        if (draft == null) return@collect
                        _state.update { current ->
                            val withJob = draftResultApplier.applyToCreatePack(current, draft, completed)
                            withJob.copy(
                                isApiLoading = false,
                                backgroundJobMessage = active?.progress?.stepLabel
                                    ?: if (active != null) "Sedang diproses di background" else null
                            )
                        }
                    }
                }
        }
    }

    fun onIntent(intent: CreatePackIntent) {
        when (intent) {
            is CreatePackIntent.UpdateName -> {
                _state.update { it.copy(name = intent.name) }
            }
            is CreatePackIntent.UpdatePublisher -> {
                _state.update { it.copy(publisher = intent.publisher) }
            }
            is CreatePackIntent.UpdateVisibility -> {
                _state.update { it.copy(visibility = intent.visibility) }
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
            is CreatePackIntent.UpdateGenerateInputImage -> {
                _state.update { it.copy(generateInputImage = intent.path) }
            }
            is CreatePackIntent.UpdateGridSplitSource -> {
                _state.update { it.copy(gridSplitSourcePath = intent.path) }
            }
            is CreatePackIntent.GenerateStickers -> generateStickers()
            is CreatePackIntent.ImprovePackStickers -> improvePackStickers()
            is CreatePackIntent.ToggleGeneratedSelection -> {
                _state.update {
                    if (intent.index !in it.generatedPreview.indices) return@update it
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
            is CreatePackIntent.ReplacePackWithGenerated -> replacePackWithGenerated()
            is CreatePackIntent.CloseGeneratedSheet -> {
                _state.update {
                    it.copy(
                        generatedPreview = emptyList(),
                        selectedGeneratedPreview = emptySet(),
                        generatedPreviewMode = GeneratedPreviewMode.AddToPack
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
            is CreatePackIntent.RestoreWorkspaceDraft -> restoreWorkspaceDraft(intent.draftId)
        }
    }

    private fun restoreWorkspaceDraft(draftId: String) {
        viewModelScope.launch {
            val draft = aiJobManager.getDraft(draftId) ?: return@launch
            val context = WorkspaceDraftFactory.decodeContext(draft)
            _state.update {
                draftResultApplier.applyToCreatePack(
                    it.copy(
                        workspaceDraftId = draftId,
                        name = context.packName,
                        publisher = context.publisher,
                        visibility = context.visibility,
                        trayImagePath = context.trayImagePath,
                        stickers = context.stickers.map { sticker -> sticker.toDraftSticker() },
                        generatePrompt = context.prompt,
                        generateInputImage = context.inputImagePath,
                        gridSplitSourcePath = context.gridSplitSourcePath,
                        gridLayout = context.gridLayout ?: it.gridLayout
                    ),
                    draft,
                    null
                )
            }
        }
    }

    private suspend fun upsertCreatePackDraft(state: CreatePackState): domain.model.aijob.WorkspaceDraft {
        val context = WorkspaceDraftContext(
            prompt = state.generatePrompt,
            inputImagePath = state.generateInputImage,
            packName = state.name,
            publisher = state.publisher,
            visibility = state.visibility,
            trayImagePath = state.trayImagePath,
            gridLayout = state.gridLayout,
            gridSplitSourcePath = state.gridSplitSourcePath,
            stickers = state.stickers.map { it.toSnapshot() },
            generatedPreview = state.generatedPreview.map { it.toSnapshot() },
            selectedGeneratedPreview = state.selectedGeneratedPreview,
            generatedPreviewMode = state.generatedPreviewMode.name,
            splitPreview = state.splitPreview.map { it.toSnapshot() },
            selectedSplitPreview = state.selectedSplitPreview
        )
        val draft = WorkspaceDraftFactory.create(
            kind = WorkspaceDraftKind.CREATE_PACK_SESSION,
            origin = AiJobOrigin.CREATE_PACK,
            displayTitle = state.name.ifBlank { "Create pack" },
            context = context,
            originRoute = if (state.packId.isBlank()) "createPack" else "createPack?packId=${state.packId}",
            packId = state.packId.takeIf { it.isNotBlank() },
            id = state.workspaceDraftId ?: Uuid.random().toString()
        )
        aiJobManager.upsertDraft(draft)
        _state.update { it.copy(workspaceDraftId = draft.id) }
        return draft
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
                        visibility = pack.visibility,
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
            if (currentState.isSaving || currentState.isApiLoading) return@launch
            
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
                _state.update { it.copy(isSaving = true, error = null) }
                val identifier = if (currentState.isEditing && currentState.packId.isNotBlank()) {
                    currentState.packId
                } else {
                    PackIdentifierSanitizer.sanitize(currentState.name, Random.nextInt(1000, 9999))
                }
                
                val pack = draftSaver.buildDraftPack(
                    StickerDraftInput(
                        identifier = identifier,
                        name = currentState.name,
                        publisher = currentState.publisher,
                        visibility = currentState.visibility,
                        trayImagePath = currentState.trayImagePath,
                        stickers = currentState.stickers.map { draft ->
                            StickerDraftInput.StickerInput(
                                imagePath = draft.imagePath,
                                decorations = draft.decorations,
                                isAnimated = draft.isAnimated,
                                sourceVideoFile = draft.sourceVideoFile,
                                frameDecorations = draft.frameDecorations
                            )
                        }
                    )
                )
                
                repository.savePack(pack)
                _state.update { it.copy(isSaving = false) }
                _effect.send(CreatePackEffect.PackSaved(identifier))
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
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

            val draft = upsertCreatePackDraft(currentState)
            enqueueHelper.enqueueGenerateStickers(
                draft = draft,
                origin = AiJobOrigin.CREATE_PACK,
                payload = GenerateStickersPayload(
                    prompt = currentState.generatePrompt,
                    inputImagePath = currentState.generateInputImage
                )
            )
            _state.update {
                it.copy(
                    isApiLoading = false,
                    error = null,
                    backgroundJobMessage = "Sedang diproses di background"
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

            val draft = upsertCreatePackDraft(currentState)
            enqueueHelper.enqueueGridSplit(
                draft = draft,
                payload = GridSplitPayload(
                    imagePath = currentState.gridSplitSourcePath,
                    layout = currentState.gridLayout
                )
            )
            _state.update {
                it.copy(
                    isApiLoading = false,
                    error = null,
                    backgroundJobMessage = "Sedang diproses di background",
                    gridSplitSheetPhase = GridSplitSheetPhase.ConfirmPick
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
        }
        _state.update {
            it.copy(
                stickers = (it.stickers + selected).take(StickerPack.MAX_STICKERS),
                generatedPreview = emptyList(),
                selectedGeneratedPreview = emptySet(),
                generatedPreviewMode = GeneratedPreviewMode.AddToPack
            )
        }
        if (current.stickers.size + selected.size > StickerPack.MAX_STICKERS) {
            viewModelScope.launch {
                _effect.send(CreatePackEffect.ShowError(UiText.StringRes(Res.string.error_partial_generate_not_added)))
            }
        }
    }

    private fun improvePackStickers() {
        viewModelScope.launch {
            val currentState = _state.value
            val sourceStickers = currentState.stickers.filter {
                !it.isAnimated && it.imagePath.isNotBlank()
            }
            if (sourceStickers.isEmpty()) {
                _effect.send(
                    CreatePackEffect.ShowError(
                        UiText.StringRes(Res.string.error_no_static_stickers_to_improve)
                    )
                )
                return@launch
            }

            val draft = upsertCreatePackDraft(currentState)
            enqueueHelper.enqueueImproveStickers(
                draft = draft,
                origin = AiJobOrigin.CREATE_PACK,
                payload = ImproveStickersPayload(imagePaths = sourceStickers.map { it.imagePath })
            )
            _state.update {
                it.copy(
                    isApiLoading = false,
                    error = null,
                    backgroundJobMessage = "Sedang diproses di background"
                )
            }
        }
    }

    private fun replacePackWithGenerated() {
        val current = _state.value
        if (current.selectedGeneratedPreview.isEmpty()) return

        val selected = current.generatedPreview.filterIndexed { index, _ ->
            current.selectedGeneratedPreview.contains(index)
        }
        if (selected.isEmpty()) return
        val animatedDrafts = current.stickers.filter { it.isAnimated }
        _state.update {
            it.copy(
                stickers = (selected + animatedDrafts).take(StickerPack.MAX_STICKERS),
                generatedPreview = emptyList(),
                selectedGeneratedPreview = emptySet(),
                generatedPreviewMode = GeneratedPreviewMode.AddToPack
            )
        }
    }
}
