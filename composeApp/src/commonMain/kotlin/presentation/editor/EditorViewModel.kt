package presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.aijob.AiJobManager
import data.remote.StickerApiRepository
import data.storage.StickerFileStorage
import domain.model.aijob.AiJobOrigin
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.GenerateStickersPayload
import domain.model.aijob.ImproveStickersPayload
import domain.model.aijob.RemoveBackgroundPayload
import domain.model.aijob.WorkspaceDraftContext
import domain.model.aijob.WorkspaceDraftKind
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import presentation.aijob.AiJobEnqueueHelper
import presentation.aijob.DraftResultApplier
import presentation.aijob.WorkspaceDraftFactory
import presentation.aijob.toSnapshot
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import data.util.EmojiPreferences
import data.util.OnDeviceImageProcessor
import domain.model.AiQuotaOperation
import domain.model.DecorationFont
import domain.model.DecorationFontWeight
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.repository.StickerRepository
import domain.repository.AiQuotaRepository
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
import kotlin.time.Clock
import presentation.common.AiQuotaGate
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_apply_removal
import setiker.composeapp.generated.resources.error_failed_improve_sticker
import setiker.composeapp.generated.resources.error_failed_generate_sticker
import setiker.composeapp.generated.resources.error_failed_save_sticker
import setiker.composeapp.generated.resources.error_pack_id_missing
import setiker.composeapp.generated.resources.error_prompt_required
import setiker.composeapp.generated.resources.error_select_image

@OptIn(ExperimentalUuidApi::class)
class EditorViewModel(
    private val repository: StickerRepository,
    private val emojiPreferences: EmojiPreferences,
    private val fileStorage: StickerFileStorage,
    private val apiRepository: StickerApiRepository,
    private val onDeviceImageProcessor: OnDeviceImageProcessor,
    private val aiJobManager: AiJobManager,
    private val enqueueHelper: AiJobEnqueueHelper,
    private val draftResultApplier: DraftResultApplier,
    private val aiQuotaRepository: AiQuotaRepository
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _effect = Channel<EditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var packId: String = ""
    private var stickerIndex: Int? = null
    private var backgroundRemovalJob: Job? = null
    private val undoStack = ArrayDeque<EditorHistoryEntry>()
    private val redoStack = ArrayDeque<EditorHistoryEntry>()

    private data class EditorHistoryEntry(
        val decorations: List<StickerDecoration>,
        val imagePath: String,
        val selectedDecorationId: String?
    )

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
                        draftResultApplier.observeJobCompletion(draftId),
                        aiJobManager.observeJobs().map { jobs ->
                            jobs
                                .filter { it.workspaceDraftId == draftId }
                                .maxByOrNull { it.updatedAt }
                        }
                    ) { draft, completedJob, latestJob -> Triple(draft, completedJob, latestJob) }
                        .collect { (draft, completedJob, latestJob) ->
                            if (draft == null) return@collect
                            val active = latestJob?.takeIf { it.status in ACTIVE_AI_JOB_STATUSES }
                            val failed = latestJob?.takeIf { it.status in FAILED_AI_JOB_STATUSES }
                            _state.update { current ->
                                val next = draftResultApplier.applyToEditor(current, draft, completedJob)
                                val hasTerminalJob = completedJob != null || failed != null
                                next.copy(
                                    isApiLoading = when {
                                        active?.type == AiJobType.GENERATE_STICKERS ||
                                            active?.type == AiJobType.IMPROVE_STICKERS -> true
                                        hasTerminalJob -> false
                                        else -> next.isApiLoading
                                    },
                                    isBackgroundRemoving = when {
                                        active?.type == AiJobType.REMOVE_BACKGROUND -> true
                                        hasTerminalJob -> false
                                        else -> next.isBackgroundRemoving
                                    },
                                    backgroundJobMessage = active?.progress?.stepLabel
                                        ?: if (active != null) {
                                            AI_JOB_FALLBACK_LABEL
                                        } else if (hasTerminalJob) {
                                            null
                                        } else {
                                            next.backgroundJobMessage
                                        },
                                    error = failed?.failureMessage ?: next.error
                                )
                            }
                        }
                }
        }
    }

    fun onIntent(intent: EditorIntent) {
        when (intent) {
            is EditorIntent.UpdateImagePath -> {
                recordHistory()
                _state.update {
                    it.copy(
                        imagePath = intent.path,
                        decorations = emptyList(),
                        selectedDecorationId = null
                    )
                }
            }
            EditorIntent.Undo -> undo()
            EditorIntent.Redo -> redo()
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
            is EditorIntent.RequestRemoveBackground -> requestRemoveBackground()
            is EditorIntent.ConfirmRemoveBackground -> removeBackground()
            is EditorIntent.DismissRemoveBackgroundConfirm -> {
                _state.update { it.copy(removeBackgroundConfirmVisible = false) }
            }
            is EditorIntent.DismissBackgroundRemoverSheet -> dismissBackgroundRemoverSheet()
            is EditorIntent.ConfirmBackgroundRemoval -> confirmBackgroundRemoval()
            is EditorIntent.SetPackId -> {
                this.packId = intent.packId
                intent.stickerIndex?.let { this.stickerIndex = it }
                _state.update { it.copy(packId = intent.packId, stickerIndex = intent.stickerIndex) }
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
                recordHistory()
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
            is EditorIntent.UpdateTextDecorationBorderColor -> updateTextDecorationBorderColor(intent.id, intent.colorArgb)
            is EditorIntent.UpdateTextDecorationBorderWidth -> updateTextDecorationBorderWidth(intent.id, intent.widthRatio)
            is EditorIntent.UpdateEmojiDecorationValue -> updateEmojiDecorationValue(intent.id, intent.emoji)
            is EditorIntent.UpdateEmojiDecorationBorderColor -> updateEmojiDecorationBorderColor(intent.id, intent.colorArgb)
            is EditorIntent.UpdateEmojiDecorationBorderWidth -> updateEmojiDecorationBorderWidth(intent.id, intent.widthRatio)
            is EditorIntent.UpdateImageDecorationPath -> updateImageDecorationPath(intent.id, intent.imagePath)
            is EditorIntent.OpenAiGenerateSheet -> openAiGenerateSheet()
            is EditorIntent.CloseAiGenerateSheet -> {
                _state.update { it.copy(aiGenerateSheetOpen = false) }
            }
            is EditorIntent.UpdateGeneratePrompt -> {
                _state.update { it.copy(generatePrompt = intent.prompt) }
            }
            is EditorIntent.UpdateGenerateInputImage -> {
                _state.update { it.copy(generateInputImage = intent.path) }
            }
            is EditorIntent.GenerateSticker -> generateSticker()
            is EditorIntent.RequestImproveSticker -> requestImproveSticker()
            is EditorIntent.ConfirmImproveSticker -> improveSticker()
            is EditorIntent.DismissImproveConfirm -> {
                _state.update { it.copy(improveConfirmVisible = false) }
            }
            is EditorIntent.ApplyGeneratedSticker -> applyGeneratedSticker(intent.draft)
            is EditorIntent.RestoreWorkspaceDraft -> restoreWorkspaceDraft(intent.draftId)
            is EditorIntent.DismissGeneratedResultsSheet -> {
                _state.update { it.copy(generatedResultsSheetVisible = false) }
            }
            is EditorIntent.CancelGeneratedResults -> {
                _state.update {
                    it.copy(
                        generatedPreview = emptyList(),
                        generatedResultsSheetVisible = false
                    )
                }
            }
            is EditorIntent.ShowGeneratedResultsSheet -> {
                _state.update {
                    if (it.generatedPreview.isEmpty() || it.isApiLoading) it
                    else it.copy(generatedResultsSheetVisible = true)
                }
            }
        }
    }

    private fun openAiGenerateSheet() {
        _state.update {
            // Default reference image = the sticker we are editing. User can replace from gallery
            // or clear it directly in the sheet. We re-seed on every open so the UX is
            // predictable; if the user explicitly cleared it during the same open, they can
            // clear it again after re-opening — that's an acceptable trade-off for predictability.
            val defaultReference = it.imagePath.takeIf { path -> path.isNotBlank() }
            it.copy(
                aiGenerateSheetOpen = true,
                generateInputImage = defaultReference,
                improveConfirmVisible = false
            )
        }
        refreshAiUsage()
    }

    private fun refreshAiUsage() {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingAiUsage = true, aiUsageLoadFailed = false) }
            val usage = aiQuotaRepository.getUsage(forceRefresh = true)
            _state.update {
                it.copy(
                    aiUsage = usage,
                    isLoadingAiUsage = false,
                    aiUsageLoadFailed = usage == null
                )
            }
        }
    }

    private fun generateSticker() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.generatePrompt.isBlank()) {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_prompt_required)))
                return@launch
            }
            AiQuotaGate.checkCanStart(
                aiQuotaRepository,
                AiQuotaOperation.GENERATE,
                forceRefresh = true
            )?.let { message ->
                _effect.send(EditorEffect.ShowError(message))
                return@launch
            }
            val draft = upsertEditorDraft(currentState)
            enqueueHelper.enqueueGenerateStickers(
                draft = draft,
                origin = AiJobOrigin.EDITOR,
                payload = GenerateStickersPayload(
                    prompt = currentState.generatePrompt,
                    inputImagePath = currentState.generateInputImage
                )
            )
            _state.update {
                it.copy(
                    isApiLoading = true,
                    backgroundJobMessage = AI_JOB_FALLBACK_LABEL,
                    aiGenerateSheetOpen = false,
                    generatedResultsSheetVisible = false,
                    generatedPreview = emptyList()
                )
            }
        }
    }

    private fun applyGeneratedSticker(draft: presentation.createpack.DraftSticker) {
        if (draft.imagePath.isBlank()) return
        // Replacing the sticker invalidates the existing decorations because the underlying
        // image likely has a different composition. API-provided decorations are part of the
        // generated result, so preserve them with the selected draft.
        _state.update {
            it.copy(
                imagePath = draft.imagePath,
                decorations = draft.decorations,
                selectedDecorationId = null,
                generatedPreview = emptyList(),
                generatedResultsSheetVisible = false
            )
        }
    }

    private fun requestImproveSticker() {
        if (_state.value.imagePath.isBlank()) {
            viewModelScope.launch {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
            }
            return
        }
        _state.update {
            it.copy(improveConfirmVisible = true, removeBackgroundConfirmVisible = false)
        }
    }

    private fun improveSticker() {
        viewModelScope.launch {
            val currentState = _state.value
            if (currentState.imagePath.isBlank()) {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
                return@launch
            }
            AiQuotaGate.checkCanStart(
                aiQuotaRepository,
                AiQuotaOperation.IMPROVE,
                forceRefresh = true
            )?.let { message ->
                _effect.send(EditorEffect.ShowError(message))
                return@launch
            }
            val draft = upsertEditorDraft(
                currentState.copy(
                    generatedPreview = emptyList(),
                    generatedResultsSheetVisible = false
                )
            )
            enqueueHelper.enqueueImproveStickers(
                draft = draft,
                origin = AiJobOrigin.EDITOR,
                payload = ImproveStickersPayload(imagePaths = listOf(currentState.imagePath))
            )
            _state.update {
                it.copy(
                    isApiLoading = true,
                    backgroundJobMessage = AI_JOB_FALLBACK_LABEL,
                    improveConfirmVisible = false,
                    generatedResultsSheetVisible = false,
                    generatedPreview = emptyList()
                )
            }
        }
    }

    private suspend fun upsertEditorDraft(state: EditorState): domain.model.aijob.WorkspaceDraft {
        val context = WorkspaceDraftContext(
            prompt = state.generatePrompt,
            inputImagePath = state.generateInputImage,
            editorImagePath = state.imagePath,
            editorDecorations = state.decorations,
            backgroundPreviewPath = state.backgroundRemoverPreviewPath
        )
        val draft = WorkspaceDraftFactory.create(
            kind = WorkspaceDraftKind.EDITOR_SESSION,
            origin = AiJobOrigin.EDITOR,
            displayTitle = "Sticker editor",
            context = context,
            originRoute = "editor?packId=$packId&stickerIndex=${stickerIndex ?: -1}",
            packId = packId,
            stickerIndex = stickerIndex,
            id = state.workspaceDraftId ?: Uuid.random().toString()
        )
        aiJobManager.upsertDraft(draft)
        _state.update { it.copy(workspaceDraftId = draft.id) }
        return draft
    }

    private fun restoreWorkspaceDraft(draftId: String) {
        viewModelScope.launch {
            val draft = aiJobManager.getDraft(draftId) ?: return@launch
            val context = WorkspaceDraftFactory.decodeContext(draft)
            _state.update {
                draftResultApplier.applyToEditor(
                    it.copy(
                        workspaceDraftId = draftId,
                        imagePath = context.editorImagePath.ifBlank { it.imagePath },
                        decorations = context.editorDecorations,
                        generatePrompt = context.prompt,
                        generateInputImage = context.inputImagePath
                    ),
                    draft,
                    null
                )
            }
        }
    }

    fun addImageDecoration(path: String) {
        if (path.isBlank()) return
        recordHistory()
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
        recordHistory()
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
        recordHistory()
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
    private fun updateTextDecorationBorderColor(id: String, colorArgb: Long) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(borderColorArgb = colorArgb)
                    } else decoration
                }
            )
        }
    }

    private fun updateTextDecorationBorderWidth(id: String, widthRatio: Float) {
        val clamped = widthRatio.coerceIn(0f, 0.2f)
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is TextDecoration && decoration.id == id) {
                        decoration.copy(borderWidthRatio = clamped)
                    } else decoration
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
    private fun updateEmojiDecorationBorderColor(id: String, colorArgb: Long) {
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is EmojiDecoration && decoration.id == id) {
                        decoration.copy(borderColorArgb = colorArgb)
                    } else decoration
                }
            )
        }
    }

    private fun updateEmojiDecorationBorderWidth(id: String, widthRatio: Float) {
        val clamped = widthRatio.coerceIn(0f, 0.2f)
        _state.update { current ->
            current.copy(
                decorations = current.decorations.map { decoration ->
                    if (decoration is EmojiDecoration && decoration.id == id) {
                        decoration.copy(borderWidthRatio = clamped)
                    } else decoration
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
        recordHistory()
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

    private fun requestRemoveBackground() {
        if (_state.value.imagePath.isBlank()) {
            viewModelScope.launch {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
            }
            return
        }
        _state.update { it.copy(removeBackgroundConfirmVisible = true, improveConfirmVisible = false) }
    }

    private fun removeBackground() {
        val path = _state.value.imagePath
        if (path.isBlank()) {
            viewModelScope.launch {
                _effect.send(EditorEffect.ShowError(UiText.StringRes(Res.string.error_select_image)))
            }
            return
        }

        viewModelScope.launch {
            val current = _state.value
            AiQuotaGate.checkCanStart(
                aiQuotaRepository,
                AiQuotaOperation.BACKGROUND_REMOVE,
                forceRefresh = true
            )?.let { message ->
                _effect.send(EditorEffect.ShowError(message))
                return@launch
            }
            val draft = upsertEditorDraft(current)
            enqueueHelper.enqueueRemoveBackground(
                draft = draft,
                payload = RemoveBackgroundPayload(imagePath = path)
            )
            _state.update {
                it.copy(
                    isBackgroundRemoverSheetOpen = true,
                    isBackgroundRemoving = true,
                    backgroundRemoverPreviewPath = null,
                    backgroundJobMessage = AI_JOB_FALLBACK_LABEL,
                    removeBackgroundConfirmVisible = false
                )
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
        this.packId = packId
        this.stickerIndex = index

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
            if (currentState.isSaving || currentState.isApiLoading || currentState.isBackgroundRemoving) return@launch

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
                _state.update { it.copy(isSaving = true) }
                android.util.Log.d("EditorViewModel", "Saving sticker with packId: '$effectivePackId'")
                
                // Save editable base image and flattened preview image separately
                val time = Clock.System.now().toEpochMilliseconds()
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
                _state.update { it.copy(isSaving = false) }
                _effect.send(EditorEffect.StickerSaved)
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false) }
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

    private fun nextDecorationId(): String = "dec_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"

    private fun currentHistoryEntry(): EditorHistoryEntry {
        val snapshot = _state.value
        return EditorHistoryEntry(
            decorations = snapshot.decorations,
            imagePath = snapshot.imagePath,
            selectedDecorationId = snapshot.selectedDecorationId
        )
    }

    private fun recordHistory() {
        undoStack.addLast(currentHistoryEntry())
        while (undoStack.size > MAX_UNDO_STEPS) {
            undoStack.removeFirst()
        }
        redoStack.clear()
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = false) }
    }

    private fun restoreHistory(entry: EditorHistoryEntry) {
        _state.update {
            it.copy(
                decorations = entry.decorations,
                imagePath = entry.imagePath,
                selectedDecorationId = entry.selectedDecorationId
            )
        }
    }

    private fun undo() {
        val previous = undoStack.removeLastOrNull() ?: return
        redoStack.addLast(currentHistoryEntry())
        restoreHistory(previous)
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    private fun redo() {
        val next = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(currentHistoryEntry())
        restoreHistory(next)
        _state.update { it.copy(canUndo = undoStack.isNotEmpty(), canRedo = redoStack.isNotEmpty()) }
    }

    private companion object {
        private const val MAX_UNDO_STEPS = 20
        private const val AI_JOB_FALLBACK_LABEL = "Processing..."

        private val ACTIVE_AI_JOB_STATUSES = setOf(
            AiJobStatus.QUEUED,
            AiJobStatus.RUNNING,
            AiJobStatus.WAITING_FOR_NETWORK,
            AiJobStatus.CHECKPOINTED,
            AiJobStatus.CANCEL_REQUESTED
        )

        private val FAILED_AI_JOB_STATUSES = setOf(
            AiJobStatus.FAILED_FINAL,
            AiJobStatus.FAILED_RETRYABLE,
            AiJobStatus.CANCELLED
        )
    }
}
