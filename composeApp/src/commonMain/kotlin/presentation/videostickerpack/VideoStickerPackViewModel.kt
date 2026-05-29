package presentation.videostickerpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.AnimatedStickerSpec
import domain.model.DecodedFrame
import domain.model.ResolvedVideoAnimatedSticker
import domain.model.ResolvedVideoStaticSticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.repository.StickerRepository
import domain.util.VideoStickerPackPlanner
import kotlin.time.Clock
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.PackIdentifierSanitizer
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_generate_video_sticker_pack
import kotlin.random.Random

class VideoStickerPackViewModel(
    private val fileStorage: StickerFileStorage,
    private val extractor: VideoFrameCandidateExtractor,
    private val gridComposer: CandidateGridComposer,
    private val apiRepository: StickerApiRepository,
    private val stickerRepository: StickerRepository,
    private val draftSaver: StickerPackDraftSaver
) : ViewModel() {
    private val _state = MutableStateFlow(VideoStickerPackState())
    val state: StateFlow<VideoStickerPackState> = _state.asStateFlow()

    private val _effect = Channel<VideoStickerPackEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: VideoStickerPackIntent) {
        when (intent) {
            is VideoStickerPackIntent.LoadVideo -> loadVideo(intent.path)
            is VideoStickerPackIntent.UpdateStart -> updateRange(startMs = intent.ms)
            is VideoStickerPackIntent.UpdateEnd -> updateRange(endMs = intent.ms)
            is VideoStickerPackIntent.UpdatePrompt -> _state.update {
                it.copy(
                    prompt = intent.value,
                    generatedPlan = null,
                    selectedStaticStickerKeys = emptySet(),
                    selectedAnimatedStickerKeys = emptySet()
                )
            }
            is VideoStickerPackIntent.UpdatePackName -> _state.update { it.copy(packName = intent.value) }
            is VideoStickerPackIntent.UpdatePublisher -> _state.update { it.copy(publisher = intent.value) }
            is VideoStickerPackIntent.ToggleStaticStickerSelection -> toggleStaticSelection(intent.key)
            is VideoStickerPackIntent.ToggleAnimatedStickerSelection -> toggleAnimatedSelection(intent.key)
            VideoStickerPackIntent.Generate -> generate(extractFreshCandidates = true)
            VideoStickerPackIntent.Regenerate -> generate(extractFreshCandidates = false)
            VideoStickerPackIntent.SavePack -> savePack()
            VideoStickerPackIntent.Cancel -> viewModelScope.launch { _effect.send(VideoStickerPackEffect.NavigateBack) }
        }
    }

    private fun loadVideo(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVideo = true, videoPath = path, errorMessage = null) }
            val duration = fileStorage.getVideoDurationMs(path).takeIf { it > 0L } ?: 60_000L
            _state.update {
                it.copy(
                    isLoadingVideo = false,
                    sourceDurationMs = duration,
                    selectedStartMs = 0L,
                    selectedEndMs = duration.coerceAtMost(VideoStickerPackPlanner.MAX_SEGMENT_MS),
                        candidates = emptyList(),
                        candidateGrids = emptyList(),
                        candidateManifest = emptyList(),
                        generatedPlan = null,
                        selectedStaticStickerKeys = emptySet(),
                        selectedAnimatedStickerKeys = emptySet()
                    )
                }
            }
    }

    private fun updateRange(startMs: Long? = null, endMs: Long? = null) {
        _state.update { current ->
            val source = current.sourceDurationMs.coerceAtLeast(1L)
            val nextStart = (startMs ?: current.selectedStartMs).coerceIn(0L, source - 1L)
            val requestedEnd = (endMs ?: current.selectedEndMs).coerceIn(nextStart + 1L, source)
            val nextEnd = requestedEnd
                .coerceAtMost(nextStart + VideoStickerPackPlanner.MAX_SEGMENT_MS)
                .coerceAtMost(source)
            current.copy(
                selectedStartMs = nextStart,
                selectedEndMs = nextEnd,
                candidates = emptyList(),
                candidateGrids = emptyList(),
                candidateManifest = emptyList(),
                generatedPlan = null,
                selectedStaticStickerKeys = emptySet(),
                selectedAnimatedStickerKeys = emptySet()
            )
        }
    }

    private fun toggleStaticSelection(key: String) {
        _state.update { current ->
            current.copy(selectedStaticStickerKeys = current.selectedStaticStickerKeys.toggle(key))
        }
    }

    private fun toggleAnimatedSelection(key: String) {
        _state.update { current ->
            current.copy(selectedAnimatedStickerKeys = current.selectedAnimatedStickerKeys.toggle(key))
        }
    }

    private fun generate(extractFreshCandidates: Boolean) {
        viewModelScope.launch {
            val current = _state.value
            if (current.isProcessing) return@launch
            runCatching { VideoStickerPackPlanner.validateSelectedRange(current.selectedStartMs, current.selectedEndMs) }
                .onFailure {
                    _effect.send(VideoStickerPackEffect.ShowError(UiText.DynamicString("Choose a segment up to 60 seconds.")))
                    return@launch
                }

            _state.update {
                it.copy(
                    isProcessing = true,
                    processingStep = VideoStickerPackProcessingStep.FindingFrames,
                    processingProgress = 0f,
                    errorMessage = null
                )
            }

            try {
                val (candidates, grids, manifest) = if (extractFreshCandidates || current.candidateGrids.isEmpty() || current.candidateManifest.isEmpty()) {
                    val extracted = extractor.extractCandidates(
                        videoPath = current.videoPath,
                        startMs = current.selectedStartMs,
                        endMs = current.selectedEndMs,
                        onProgress = { done, total ->
                            _state.update { it.copy(processingProgress = done.toFloat() / total.coerceAtLeast(1)) }
                        }
                    ).take(VideoStickerPackPlanner.MAX_CANDIDATES)
                    if (extracted.isEmpty()) error("No usable frames extracted")
                    _state.update {
                        it.copy(
                            candidates = extracted,
                            processingStep = VideoStickerPackProcessingStep.BuildingGrids
                        )
                    }
                    val composed = gridComposer.composeGrids(extracted.map { it.filePath })
                    val builtManifest = VideoStickerPackPlanner.buildCandidateManifest(extracted, composed)
                    _state.update {
                        it.copy(
                            candidateGrids = composed,
                            candidateManifest = builtManifest
                        )
                    }
                    Triple(extracted, composed, builtManifest)
                } else {
                    Triple(current.candidates, current.candidateGrids, current.candidateManifest)
                }

                _state.update {
                    it.copy(
                        processingStep = VideoStickerPackProcessingStep.AskingAi,
                        processingProgress = 0f
                    )
                }
                val generated = apiRepository.generateVideoStickerPack(
                    candidateGridPaths = grids.map { it.filePath },
                    candidateManifest = manifest,
                    candidates = candidates,
                    selectedStartMs = current.selectedStartMs,
                    selectedEndMs = current.selectedEndMs,
                    sourceDurationMs = current.sourceDurationMs,
                    prompt = current.prompt.takeIf { it.isNotBlank() }
                )
                if (generated.staticStickers.isEmpty() && generated.animatedStickers.isEmpty()) {
                    error("No generated stickers returned")
                }
                _state.update { it.copy(processingStep = VideoStickerPackProcessingStep.PreparingPreview) }
                val selectedStaticKeys = generated.staticStickers.map { it.selectionKey() }.toSet()
                val selectedAnimatedKeys = generated.animatedStickers.mapIndexed { index, sticker ->
                    sticker.selectionKey(index)
                }.toSet()

                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        processingProgress = 1f,
                        generatedPlan = generated,
                        selectedStaticStickerKeys = selectedStaticKeys,
                        selectedAnimatedStickerKeys = selectedAnimatedKeys
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        errorMessage = e.message
                    )
                }
                _effect.send(
                    VideoStickerPackEffect.ShowError(
                        e.message?.let(UiText::DynamicString)
                            ?: UiText.StringRes(Res.string.error_failed_generate_video_sticker_pack)
                    )
                )
            }
        }
    }

    private fun savePack() {
        viewModelScope.launch {
            val current = _state.value
            if (!current.canSave) return@launch
            try {
                _state.update {
                    it.copy(
                        isProcessing = true,
                        processingStep = VideoStickerPackProcessingStep.Saving,
                        processingProgress = 0f
                    )
                }
                val generated = current.generatedPlan ?: return@launch
                val selectedStatic = generated.staticStickers.filter {
                    it.selectionKey() in current.selectedStaticStickerKeys
                }
                val selectedAnimated = generated.animatedStickers.filterIndexed { index, sticker ->
                    sticker.selectionKey(index) in current.selectedAnimatedStickerKeys
                }
                if (selectedStatic.isEmpty() && selectedAnimated.isEmpty()) return@launch

                val shouldSplitNames = selectedStatic.isNotEmpty() && selectedAnimated.isNotEmpty()
                val identifier = PackIdentifierSanitizer.sanitize(current.packName, Random.nextInt(1000, 9999))
                val staticInputs = selectedStatic.map { sticker ->
                    StickerDraftInput.StickerInput(
                        imagePath = sticker.localPath,
                        decorations = sticker.plan.decorations
                    )
                }
                val animatedInputs = selectedAnimated.mapIndexed { index, sticker ->
                    val loadedFrames = sticker.timeline.map { resolved ->
                        fileStorage.loadImage(resolved.localPath)?.takeIf { it.isNotEmpty() }?.let { bytes ->
                            DecodedFrame(bytes = bytes, durationMs = resolved.frame.durationMs)
                        }
                    }
                    val frames = loadedFrames.takeIf { loaded -> loaded.all { it != null } }
                        ?.filterNotNull()
                        .orEmpty()
                    val animatedPath = if (frames.size >= 2) {
                        fileStorage.saveAnimatedStickerImage(
                            frames = frames,
                            fileName = "video_pack_${identifier}_anim_${index}_${Clock.System.now().toEpochMilliseconds()}.webp",
                            baseDecorations = sticker.plan.baseDecorations,
                            frameDecorations = sticker.plan.frameDecorations
                        )
                    } else {
                        val start = sticker.plan.timeline.minOfOrNull { it.timestampMs } ?: current.selectedStartMs
                        val end = (sticker.plan.timeline.maxOfOrNull { it.timestampMs } ?: current.selectedEndMs)
                            .coerceAtLeast(start + 1_000L)
                        val decoded = fileStorage.decodeVideoFrames(
                            videoPath = current.videoPath,
                            spec = AnimatedStickerSpec(
                                trimStartMs = start,
                                trimEndMs = end.coerceAtMost(current.sourceDurationMs),
                                fps = sticker.plan.fps,
                                loopCount = sticker.plan.loopCount
                            )
                        )
                        fileStorage.saveAnimatedStickerImage(
                            frames = decoded,
                            fileName = "video_pack_${identifier}_anim_${index}_${Clock.System.now().toEpochMilliseconds()}.webp",
                            baseDecorations = sticker.plan.baseDecorations,
                            frameDecorations = sticker.plan.frameDecorations
                        )
                    }
                    StickerDraftInput.StickerInput(
                        imagePath = animatedPath,
                        decorations = sticker.plan.baseDecorations,
                        isAnimated = true,
                        sourceVideoFile = current.videoPath,
                        frameDecorations = sticker.plan.frameDecorations
                    )
                }

                val savedPacks = mutableListOf<StickerPack>()
                if (staticInputs.isNotEmpty()) {
                    val staticIdentifier = if (shouldSplitNames) "${identifier}_static" else identifier
                    val staticPack = draftSaver.buildDraftPack(
                        StickerDraftInput(
                            identifier = staticIdentifier,
                            name = if (shouldSplitNames) "${current.packName} Static" else current.packName,
                            publisher = current.publisher,
                            visibility = "PRIVATE",
                            trayImagePath = selectedStatic.first().localPath,
                            stickers = staticInputs
                        )
                    )
                    stickerRepository.savePack(staticPack)
                    savedPacks += staticPack
                }
                if (animatedInputs.isNotEmpty()) {
                    val animatedIdentifier = if (shouldSplitNames) "${identifier}_animated" else identifier
                    val animatedPack = draftSaver.buildDraftPack(
                        StickerDraftInput(
                            identifier = animatedIdentifier,
                            name = if (shouldSplitNames) "${current.packName} Animated" else current.packName,
                            publisher = current.publisher,
                            visibility = "PRIVATE",
                            trayImagePath = animatedInputs.first().imagePath,
                            stickers = animatedInputs
                        )
                    )
                    stickerRepository.savePack(animatedPack)
                    savedPacks += animatedPack
                }
                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        processingProgress = 1f
                    )
                }
                val destination = savedPacks.firstOrNull()?.identifier ?: error("No selected stickers returned")
                _effect.send(VideoStickerPackEffect.NavigateToPackDetail(destination))
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, processingStep = null) }
                _effect.send(
                    VideoStickerPackEffect.ShowError(
                        UiText.DynamicString(e.message ?: "Failed to save pack.")
                    )
                )
            }
        }
    }
}

private fun ResolvedVideoStaticSticker.selectionKey(): String =
    "static:${plan.candidateId}:${plan.timestampMs}:$localPath"

private fun ResolvedVideoAnimatedSticker.selectionKey(index: Int): String =
    "animated:$index:${plan.timeline.firstOrNull()?.timestampMs ?: 0}:${plan.timeline.lastOrNull()?.timestampMs ?: 0}"

private fun Set<String>.toggle(key: String): Set<String> =
    if (key in this) this - key else this + key
