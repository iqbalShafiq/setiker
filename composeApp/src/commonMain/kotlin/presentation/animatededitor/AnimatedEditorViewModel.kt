package presentation.animatededitor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.aijob.AiJobJson
import data.aijob.AiJobManager
import data.aijob.AnimatedWorkspaceDraftHelper
import data.util.EmojiPreferences
import domain.model.DecorationFont
import domain.model.EmojiDecoration
import domain.model.ImageDecoration
import domain.model.Sticker
import domain.model.StickerDecoration
import domain.model.TextDecoration
import domain.model.aijob.AiJobStatus
import domain.model.aijob.AiJobType
import domain.model.aijob.AnimatedEncodePayload
import domain.model.aijob.AnimatedEncodeResult
import domain.model.aijob.WorkspaceDraftStatus
import domain.repository.AiJobRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Clock
import presentation.aijob.AiJobEnqueueHelper
import presentation.aijob.WorkspaceDraftFactory
import presentation.common.UiText
import presentation.common.animatedFailureMessageToUiText
import presentation.common.toAnimatedUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.animated_error_draft_not_found
import setiker.composeapp.generated.resources.animated_error_frames_missing
import setiker.composeapp.generated.resources.animated_error_save_failed

class AnimatedEditorViewModel(
    private val animatedDraftHelper: AnimatedWorkspaceDraftHelper,
    private val emojiPreferences: EmojiPreferences,
    private val aiJobManager: AiJobManager,
    private val enqueueHelper: AiJobEnqueueHelper,
    private val jobRepository: AiJobRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AnimatedEditorState())
    val state: StateFlow<AnimatedEditorState> = _state.asStateFlow()

    private val _effect = Channel<AnimatedEditorEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var packId: String = ""
    private var playJob: Job? = null
    private var handledCompletedJobId: String? = null

    init {
        observeEncodeJobs()
    }

    private fun observeEncodeJobs() {
        viewModelScope.launch {
            _state
                .map { it.workspaceDraftId }
                .distinctUntilChanged()
                .filterNotNull()
                .collect { draftId ->
                    jobRepository.observeByDraft(draftId).collect { jobs ->
                        val active = jobs.firstOrNull {
                            it.type == AiJobType.ANIMATED_ENCODE &&
                                it.status in setOf(
                                    AiJobStatus.QUEUED,
                                    AiJobStatus.RUNNING,
                                    AiJobStatus.WAITING_FOR_NETWORK,
                                    AiJobStatus.CHECKPOINTED
                                )
                        }
                        val completed = jobs.firstOrNull {
                            it.type == AiJobType.ANIMATED_ENCODE && it.status == AiJobStatus.COMPLETED
                        }
                        val failed = jobs.firstOrNull {
                            it.type == AiJobType.ANIMATED_ENCODE &&
                                it.status in setOf(AiJobStatus.FAILED_FINAL, AiJobStatus.FAILED_RETRYABLE)
                        }

                        _state.update { current ->
                            val hasTerminalJob = completed != null || failed != null
                            current.copy(
                                isSaving = when {
                                    active != null -> true
                                    hasTerminalJob -> false
                                    else -> current.isSaving
                                },
                                backgroundJobMessage = active?.let {
                                    it.progress?.stepLabel ?: AI_JOB_FALLBACK_LABEL
                                } ?: if (hasTerminalJob) null else current.backgroundJobMessage,
                                saveProgress = active?.progress?.fraction ?: current.saveProgress,
                                saveProgressLabel = active?.progress?.stepLabel
                            )
                        }

                        if (completed != null && completed.id != handledCompletedJobId) {
                            handledCompletedJobId = completed.id
                            handleEncodeCompleted(completed)
                        } else if (failed != null) {
                            _state.update { it.copy(isSaving = false) }
                            _effect.send(
                                AnimatedEditorEffect.ShowError(
                                    animatedFailureMessageToUiText(failed.failureMessage)
                                        ?: UiText.StringRes(Res.string.animated_error_save_failed)
                                )
                            )
                        }
                    }
                }
        }
    }

    private suspend fun handleEncodeCompleted(job: domain.model.aijob.AiJob) {
        val draft = aiJobManager.getDraft(job.workspaceDraftId) ?: return
        val context = WorkspaceDraftFactory.decodeContext(draft)
        val outputPath = job.resultJson?.let {
            AiJobJson.codec.decodeFromString<AnimatedEncodeResult>(it).outputPath
        } ?: context.animatedOutputPath
        if (outputPath.isNullOrBlank()) return

        _state.value.emojis.forEach { emoji -> emojiPreferences.addRecentEmoji(emoji) }

        _state.update { it.copy(isSaving = false, saveProgress = 1f, backgroundJobMessage = null) }
        aiJobManager.upsertDraft(
            draft.copy(
                status = WorkspaceDraftStatus.APPLIED,
                updatedAt = Clock.System.now().toEpochMilliseconds()
            )
        )
        _effect.send(
            AnimatedEditorEffect.AnimatedDraftReady(
                imagePath = outputPath,
                sourceVideoFile = context.videoPath.orEmpty(),
                baseDecorations = context.animatedBaseDecorations,
                frameDecorations = context.animatedFrameDecorations
            )
        )
    }

    private val current get() = _state.value

    fun onIntent(intent: AnimatedEditorIntent) {
        when (intent) {
            is AnimatedEditorIntent.LoadDraft -> loadDraft(intent.draftId)
            is AnimatedEditorIntent.ScrubToFrame -> scrubToFrame(intent.index)
            is AnimatedEditorIntent.PlayPreview -> startPlayback()
            is AnimatedEditorIntent.PausePreview -> stopPlayback()
            is AnimatedEditorIntent.AdvanceFrame -> advanceFrame()
            is AnimatedEditorIntent.SetApplyScope -> _state.update { it.copy(applyScope = intent.scope) }
            is AnimatedEditorIntent.AddTextDecoration -> addTextDecoration(intent.text, intent.font)
            is AnimatedEditorIntent.AddEmojiDecoration -> addEmojiDecoration(intent.emoji)
            is AnimatedEditorIntent.AddImageDecoration -> addImageDecoration(intent.imagePath)
            is AnimatedEditorIntent.SelectDecoration -> _state.update { it.copy(selectedDecorationId = intent.id) }
            is AnimatedEditorIntent.RemoveDecoration -> removeDecoration(intent.id)
            is AnimatedEditorIntent.UpdateDecorationTransform -> updateDecorationTransform(
                id = intent.id,
                centerX = intent.centerX,
                centerY = intent.centerY,
                scale = intent.scale
            )
            is AnimatedEditorIntent.UpdateTextDecorationText -> mutateText(intent.id) { it.copy(text = intent.text.trim()) }
            is AnimatedEditorIntent.UpdateTextDecorationFont -> mutateText(intent.id) { it.copy(font = intent.font) }
            is AnimatedEditorIntent.UpdateTextDecorationFontWeight -> mutateText(intent.id) { it.copy(fontWeight = intent.weight) }
            is AnimatedEditorIntent.UpdateTextDecorationColor -> mutateText(intent.id) { it.copy(textColorArgb = intent.colorArgb) }
            is AnimatedEditorIntent.UpdateTextDecorationBorderColor -> mutateText(intent.id) { it.copy(borderColorArgb = intent.colorArgb) }
            is AnimatedEditorIntent.UpdateTextDecorationBorderWidth -> mutateText(intent.id) { it.copy(borderWidthRatio = intent.widthRatio.coerceIn(0f, 0.2f)) }
            is AnimatedEditorIntent.UpdateEmojiDecoration -> mutateEmoji(intent.id) { it.copy(emoji = intent.emoji) }
            is AnimatedEditorIntent.UpdateEmojiDecorationBorderColor -> mutateEmoji(intent.id) { it.copy(borderColorArgb = intent.colorArgb) }
            is AnimatedEditorIntent.UpdateEmojiDecorationBorderWidth -> mutateEmoji(intent.id) { it.copy(borderWidthRatio = intent.widthRatio.coerceIn(0f, 0.2f)) }
            is AnimatedEditorIntent.UpdateImageDecorationPath -> mutateImage(intent.id) { it.copy(imagePath = intent.imagePath) }
            is AnimatedEditorIntent.AddEmojiTag -> {
                if (_state.value.emojis.size < AnimatedEditorState.MAX_EMOJIS_PER_STICKER) {
                    _state.update { it.copy(emojis = it.emojis + intent.emoji) }
                }
            }
            is AnimatedEditorIntent.RemoveEmojiTag -> {
                _state.update {
                    it.copy(emojis = it.emojis.filterIndexed { index, _ -> index != intent.index })
                }
            }
            is AnimatedEditorIntent.UpdateAccessibilityText -> _state.update { it.copy(accessibilityText = intent.text) }
            is AnimatedEditorIntent.ShowEmojiPicker -> {
                viewModelScope.launch {
                    val recent = emojiPreferences.getRecentEmojis()
                    _state.update { it.copy(showEmojiPicker = true, recentEmojis = recent) }
                }
            }
            is AnimatedEditorIntent.HideEmojiPicker -> _state.update { it.copy(showEmojiPicker = false) }
            is AnimatedEditorIntent.ShowDecorationEmojiPicker -> {
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
            is AnimatedEditorIntent.HideDecorationEmojiPicker -> _state.update {
                it.copy(showDecorationEmojiPicker = false, decorationEmojiPickerTargetId = null)
            }
            is AnimatedEditorIntent.ShowTextDecorationSheet -> _state.update { it.copy(showTextDecorationSheet = true) }
            is AnimatedEditorIntent.HideTextDecorationSheet -> _state.update { it.copy(showTextDecorationSheet = false) }
            is AnimatedEditorIntent.SetPackId -> {
                this.packId = intent.packId
            }
            is AnimatedEditorIntent.Save -> saveAnimatedSticker()
        }
    }

    private fun loadDraft(draftId: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, draftId = draftId, workspaceDraftId = draftId) }
            val draft = aiJobManager.getDraft(draftId)
            if (draft == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringRes(Res.string.animated_error_draft_not_found)
                    )
                }
                _effect.send(AnimatedEditorEffect.NavigateBack)
                return@launch
            }
            val frames = animatedDraftHelper.loadFrames(draft)
            if (frames == null) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = UiText.StringRes(Res.string.animated_error_frames_missing)
                    )
                }
                _effect.send(AnimatedEditorEffect.NavigateBack)
                return@launch
            }
            _state.update {
                animatedDraftHelper.editorStateFromDraft(draft, frames).copy(isLoading = false)
            }
        }
    }

    private fun scrubToFrame(index: Int) {
        stopPlayback()
        _state.update {
            val clamped = index.coerceIn(0, it.totalFrames - 1)
            it.copy(currentFrameIndex = clamped)
        }
    }

    private fun startPlayback() {
        if (_state.value.isPlaying) return
        if (_state.value.frames.isEmpty()) return
        _state.update { it.copy(isPlaying = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            while (isActive && _state.value.isPlaying) {
                val frames = _state.value.frames
                if (frames.isEmpty()) break
                val current = _state.value.currentFrameIndex
                val next = (current + 1) % frames.size
                val frameDuration = frames[current].durationMs.coerceAtLeast(8L)
                _state.update { it.copy(currentFrameIndex = next) }
                delay(frameDuration)
            }
            _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun stopPlayback() {
        playJob?.cancel()
        playJob = null
        if (_state.value.isPlaying) {
            _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun advanceFrame() {
        scrubToFrame(_state.value.currentFrameIndex + 1)
    }

    private fun addDecoration(decoration: StickerDecoration) {
        _state.update { current ->
            when (current.applyScope) {
                DecorationApplyScope.AllFrames -> current.copy(
                    baseDecorations = current.baseDecorations + decoration,
                    selectedDecorationId = decoration.id
                )
                DecorationApplyScope.CurrentFrameOnly -> {
                    val existingForFrame = current.frameDecorations[current.currentFrameIndex] ?: emptyList()
                    val updatedMap = current.frameDecorations.toMutableMap().apply {
                        put(current.currentFrameIndex, existingForFrame + decoration)
                    }
                    current.copy(
                        frameDecorations = updatedMap,
                        selectedDecorationId = decoration.id
                    )
                }
            }
        }
    }

    private fun addTextDecoration(text: String, font: DecorationFont) {
        if (text.isBlank()) return
        addDecoration(
            TextDecoration(
                id = nextDecorationId(),
                text = text.trim(),
                font = font,
                fontWeight = domain.model.DecorationFontWeight.Regular,
                textColorArgb = 0xFFFFFFFFL
            )
        )
        _state.update { it.copy(showTextDecorationSheet = false) }
    }

    private fun addEmojiDecoration(emoji: String) {
        if (emoji.isBlank()) return
        addDecoration(EmojiDecoration(id = nextDecorationId(), emoji = emoji))
        _state.update { it.copy(showDecorationEmojiPicker = false, decorationEmojiPickerTargetId = null) }
    }

    private fun addImageDecoration(imagePath: String) {
        if (imagePath.isBlank()) return
        addDecoration(
            ImageDecoration(
                id = nextDecorationId(),
                imagePath = imagePath,
                centerX = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f,
                centerY = 0.5f + (Random.nextFloat() - 0.5f) * 0.2f
            )
        )
    }

    private fun removeDecoration(id: String) {
        _state.update { current ->
            val newBase = current.baseDecorations.filterNot { it.id == id }
            val newFrameDecorations = current.frameDecorations.mapValues { (_, list) ->
                list.filterNot { it.id == id }
            }.filterValues { it.isNotEmpty() }
            current.copy(
                baseDecorations = newBase,
                frameDecorations = newFrameDecorations,
                selectedDecorationId = if (current.selectedDecorationId == id) null else current.selectedDecorationId
            )
        }
    }

    private fun updateDecorationTransform(id: String, centerX: Float, centerY: Float, scale: Float) {
        val cx = centerX.coerceIn(0f, 1f)
        val cy = centerY.coerceIn(0f, 1f)
        val sc = scale.coerceIn(0.3f, 4f)
        _state.update { current ->
            current.copy(
                baseDecorations = current.baseDecorations.map {
                    if (it.id == id) it.withTransform(cx, cy, sc) else it
                },
                frameDecorations = current.frameDecorations.mapValues { (_, list) ->
                    list.map { if (it.id == id) it.withTransform(cx, cy, sc) else it }
                }
            )
        }
    }

    private inline fun mutateText(id: String, crossinline transform: (TextDecoration) -> TextDecoration) {
        _state.update { current ->
            current.copy(
                baseDecorations = current.baseDecorations.map { dec ->
                    if (dec is TextDecoration && dec.id == id) transform(dec) else dec
                },
                frameDecorations = current.frameDecorations.mapValues { (_, list) ->
                    list.map { dec -> if (dec is TextDecoration && dec.id == id) transform(dec) else dec }
                }
            )
        }
    }

    private inline fun mutateEmoji(id: String, crossinline transform: (EmojiDecoration) -> EmojiDecoration) {
        _state.update { current ->
            current.copy(
                baseDecorations = current.baseDecorations.map { dec ->
                    if (dec is EmojiDecoration && dec.id == id) transform(dec) else dec
                },
                frameDecorations = current.frameDecorations.mapValues { (_, list) ->
                    list.map { dec -> if (dec is EmojiDecoration && dec.id == id) transform(dec) else dec }
                }
            )
        }
    }

    private inline fun mutateImage(id: String, crossinline transform: (ImageDecoration) -> ImageDecoration) {
        _state.update { current ->
            current.copy(
                baseDecorations = current.baseDecorations.map { dec ->
                    if (dec is ImageDecoration && dec.id == id) transform(dec) else dec
                },
                frameDecorations = current.frameDecorations.mapValues { (_, list) ->
                    list.map { dec -> if (dec is ImageDecoration && dec.id == id) transform(dec) else dec }
                }
            )
        }
    }

    private fun saveAnimatedSticker() {
        val snapshot = _state.value
        if (snapshot.frames.isEmpty() || snapshot.workspaceDraftId.isNullOrBlank()) return
        viewModelScope.launch {
            stopPlayback()
            _state.update {
                it.copy(
                    isSaving = true,
                    saveProgress = 0f,
                    saveProgressLabel = null,
                    error = null,
                    backgroundJobMessage = AI_JOB_FALLBACK_LABEL
                )
            }
            try {
                animatedDraftHelper.syncEditorState(snapshot.workspaceDraftId, snapshot)
                val draft = aiJobManager.getDraft(snapshot.workspaceDraftId)
                    ?: throw IllegalStateException("Draft not found")
                val time = Clock.System.now().toEpochMilliseconds()
                val packIdSafe = packId.ifBlank { "draft" }
                val fileName = "anim_sticker_${packIdSafe}_${time}.webp"
                enqueueHelper.enqueueAnimatedEncode(
                    draft = draft,
                    payload = AnimatedEncodePayload(outputFileName = fileName)
                )
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, backgroundJobMessage = null) }
                _effect.send(
                    AnimatedEditorEffect.ShowError(e.toAnimatedUiText(Res.string.animated_error_save_failed))
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

    private fun nextDecorationId(): String =
        "anim_dec_${Clock.System.now().toEpochMilliseconds()}_${Random.nextInt(1000, 9999)}"

    private companion object {
        private const val AI_JOB_FALLBACK_LABEL = "Processing..."
    }
}
