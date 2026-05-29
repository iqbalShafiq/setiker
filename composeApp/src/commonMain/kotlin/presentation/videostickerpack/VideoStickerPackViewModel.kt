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
import domain.model.Sticker
import domain.model.StickerDraftInput
import domain.model.StickerPack
import domain.repository.StickerRepository
import domain.util.VideoStickerPackPlanner
import kotlin.random.Random
import kotlin.time.Clock
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import presentation.common.PackIdentifierSanitizer
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_generate_video_sticker_pack

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

    private var previewPlaybackJob: Job? = null
    private var previewExtractJob: Job? = null

    fun onIntent(intent: VideoStickerPackIntent) {
        when (intent) {
            is VideoStickerPackIntent.LoadVideo -> loadVideo(intent.path)
            is VideoStickerPackIntent.UpdateStart -> {
                stopPreviewPlayback()
                updateRange(startMs = intent.ms)
            }
            is VideoStickerPackIntent.UpdateEnd -> {
                stopPreviewPlayback()
                updateRange(endMs = intent.ms)
            }
            is VideoStickerPackIntent.ScrubPreviewTo -> scrubPreviewTo(intent.index)
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
            VideoStickerPackIntent.PlayPreview -> startPreviewPlayback()
            VideoStickerPackIntent.PausePreview -> stopPreviewPlayback()
            VideoStickerPackIntent.Generate -> generate(extractFreshCandidates = true)
            VideoStickerPackIntent.Regenerate -> generate(extractFreshCandidates = false)
            VideoStickerPackIntent.SavePack -> savePack()
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewPlaybackJob?.cancel()
        previewExtractJob?.cancel()
    }

    private fun loadVideo(path: String) {
        viewModelScope.launch {
            stopPreviewPlayback()
            _state.update {
                it.copy(
                    isLoadingVideo = true,
                    videoPath = path,
                    previewFramePath = null,
                    previewFrames = emptyList(),
                    currentPreviewIndex = 0,
                    isPreviewPlaying = false,
                    errorMessage = null
                )
            }
            val duration = fileStorage.getVideoDurationMs(path).takeIf { it > 0L } ?: 60_000L
            val previewPath = fileStorage.extractVideoFrameToFile(
                videoPath = path,
                atMs = 0L,
                fileName = "video_pack_preview_${Clock.System.now().toEpochMilliseconds()}.png"
            )
            _state.update {
                it.copy(
                    isLoadingVideo = false,
                    previewFramePath = previewPath,
                    sourceDurationMs = duration,
                    selectedStartMs = 0L,
                    selectedEndMs = duration.coerceAtMost(VideoStickerPackPlanner.MAX_SEGMENT_MS),
                    previewFrames = emptyList(),
                    currentPreviewIndex = 0,
                    isPreviewPlaying = false,
                    candidates = emptyList(),
                    candidateGrids = emptyList(),
                    candidateManifest = emptyList(),
                    generatedPlan = null,
                    selectedStaticStickerKeys = emptySet(),
                    selectedAnimatedStickerKeys = emptySet()
                )
            }
            reschedulePreviewExtraction(immediate = true)
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
                previewFrames = emptyList(),
                currentPreviewIndex = 0,
                candidates = emptyList(),
                candidateGrids = emptyList(),
                candidateManifest = emptyList(),
                generatedPlan = null,
                selectedStaticStickerKeys = emptySet(),
                selectedAnimatedStickerKeys = emptySet()
            )
        }
        reschedulePreviewExtraction(immediate = false)
    }

    private fun scrubPreviewTo(index: Int) {
        stopPreviewPlayback()
        _state.update { current ->
            val maxIndex = (current.activePreviewFrames.size - 1).coerceAtLeast(0)
            current.copy(currentPreviewIndex = index.coerceIn(0, maxIndex))
        }
    }

    private fun reschedulePreviewExtraction(immediate: Boolean) {
        previewExtractJob?.cancel()
        val snapshot = _state.value
        if (snapshot.videoPath.isBlank()) return
        if (snapshot.selectedEndMs <= snapshot.selectedStartMs) return

        previewExtractJob = viewModelScope.launch {
            if (!immediate) delay(PREVIEW_EXTRACT_DEBOUNCE_MS)
            val current = _state.value
            if (current.videoPath.isBlank()) return@launch
            if (current.selectedEndMs <= current.selectedStartMs) return@launch
            extractPreviewFramesForRange(
                videoPath = current.videoPath,
                startMs = current.selectedStartMs,
                endMs = current.selectedEndMs
            )
        }
    }

    private suspend fun extractPreviewFramesForRange(
        videoPath: String,
        startMs: Long,
        endMs: Long
    ) {
        val span = (endMs - startMs).coerceAtLeast(1L)
        val count = VideoStickerPackState.PREVIEW_FRAME_COUNT
        val timestamps = (0 until count).map { index ->
            if (count == 1) startMs else startMs + (index.toLong() * span / (count - 1))
        }
        val frames = mutableListOf<VideoStickerPackPreviewFrame>()
        timestamps.forEachIndexed { index, timestampMs ->
            val path = fileStorage.extractVideoFrameToFile(
                videoPath = videoPath,
                atMs = timestampMs,
                fileName = "video_pack_preview_${Clock.System.now().toEpochMilliseconds()}_$index.png"
            )
            if (path != null) {
                frames += VideoStickerPackPreviewFrame(timestampMs = timestampMs, filePath = path)
                _state.update {
                    it.copy(
                        previewFrames = frames.toList(),
                        currentPreviewIndex = it.currentPreviewIndex.coerceIn(0, (frames.size - 1).coerceAtLeast(0))
                    )
                }
            }
        }
    }

    private fun startPreviewPlayback() {
        if (_state.value.isPreviewPlaying) return
        if (!_state.value.canPlayPreview) return
        _state.update { it.copy(isPreviewPlaying = true) }
        previewPlaybackJob?.cancel()
        previewPlaybackJob = viewModelScope.launch {
            while (isActive && _state.value.isPreviewPlaying) {
                val frames = _state.value.activePreviewFrames
                if (frames.isEmpty()) break
                _state.update {
                    it.copy(currentPreviewIndex = (it.currentPreviewIndex + 1) % frames.size)
                }
                delay(previewFrameDelayMs())
            }
            _state.update { it.copy(isPreviewPlaying = false) }
        }
    }

    private fun stopPreviewPlayback() {
        previewPlaybackJob?.cancel()
        previewPlaybackJob = null
        if (_state.value.isPreviewPlaying) {
            _state.update { it.copy(isPreviewPlaying = false) }
        }
    }

    private fun previewFrameDelayMs(): Long {
        val state = _state.value
        val frameCount = state.activePreviewFrames.size.coerceAtLeast(1)
        val trimMs = state.selectedDurationMs.coerceAtLeast(1L)
        return (trimMs / frameCount).coerceAtLeast(80L)
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
                    _effect.send(
                        VideoStickerPackEffect.ShowError(
                            UiText.DynamicString("Choose a segment up to 60 seconds.")
                        )
                    )
                    return@launch
                }
            stopPreviewPlayback()

            _state.update {
                it.copy(
                    isProcessing = true,
                    processingStep = VideoStickerPackProcessingStep.FindingFrames,
                    processingProgress = 0f,
                    errorMessage = null
                )
            }

            try {
                val (candidates, grids, manifest) = if (
                    extractFreshCandidates ||
                    current.candidateGrids.isEmpty() ||
                    current.candidateManifest.isEmpty()
                ) {
                    val extracted = extractor.extractCandidates(
                        videoPath = current.videoPath,
                        startMs = current.selectedStartMs,
                        endMs = current.selectedEndMs,
                        onProgress = { done, total ->
                            _state.update {
                                it.copy(processingProgress = done.toFloat() / total.coerceAtLeast(1))
                            }
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
                stopPreviewPlayback()
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
                val identifier = PackIdentifierSanitizer.sanitize(
                    current.packName,
                    Random.nextInt(1000, 9999)
                )

                val staticInputs = selectedStatic.map { sticker ->
                    StickerDraftInput.StickerInput(
                        imagePath = sticker.localPath,
                        decorations = sticker.plan.decorations
                    )
                }
                val animatedStickers = selectedAnimated.mapIndexed { index, sticker ->
                    val loadedFrames = sticker.timeline.map { resolved ->
                        fileStorage.loadImage(resolved.localPath)
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { bytes ->
                                DecodedFrame(bytes = bytes, durationMs = resolved.frame.durationMs)
                            }
                    }
                    val frames = loadedFrames.takeIf { loaded -> loaded.all { it != null } }
                        ?.filterNotNull()
                        .orEmpty()
                    val sourceFrames = if (frames.size >= 2) {
                        frames
                    } else {
                        val start = sticker.plan.timeline.minOfOrNull { it.timestampMs } ?: current.selectedStartMs
                        val end = (sticker.plan.timeline.maxOfOrNull { it.timestampMs } ?: current.selectedEndMs)
                            .coerceAtLeast(start + 1_000L)
                        fileStorage.decodeVideoFrames(
                            videoPath = current.videoPath,
                            spec = AnimatedStickerSpec(
                                trimStartMs = start,
                                trimEndMs = end.coerceAtMost(current.sourceDurationMs),
                                fps = sticker.plan.fps,
                                loopCount = sticker.plan.loopCount
                            )
                        )
                    }
                    val timestamp = Clock.System.now().toEpochMilliseconds()
                    val baseAnimatedPath = fileStorage.saveAnimatedStickerImage(
                        frames = sourceFrames,
                        fileName = "video_pack_${identifier}_anim_${index}_${timestamp}_base.webp"
                    )
                    val previewAnimatedPath = if (
                        sticker.plan.baseDecorations.isEmpty() &&
                        sticker.plan.frameDecorations.isEmpty()
                    ) {
                        baseAnimatedPath
                    } else {
                        fileStorage.saveAnimatedStickerImage(
                            frames = sourceFrames,
                            fileName = "video_pack_${identifier}_anim_${index}_${timestamp}_preview.webp",
                            baseDecorations = sticker.plan.baseDecorations,
                            frameDecorations = sticker.plan.frameDecorations
                        )
                    }
                    Sticker(
                        imageFile = previewAnimatedPath,
                        sourceImageFile = baseAnimatedPath,
                        emojis = listOf("⭐"),
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
                if (animatedStickers.isNotEmpty()) {
                    val animatedIdentifier = if (shouldSplitNames) "${identifier}_animated" else identifier
                    val animatedPack = StickerPack(
                        identifier = animatedIdentifier,
                        name = if (shouldSplitNames) "${current.packName} Animated" else current.packName,
                        publisher = current.publisher,
                        trayImageFile = fileStorage.saveTrayImage(
                            sourcePath = animatedStickers.first().imageFile,
                            fileName = "tray_${animatedIdentifier}_${Clock.System.now().toEpochMilliseconds()}.png"
                        ),
                        stickers = animatedStickers,
                        isAnimated = true,
                        visibility = "PRIVATE"
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
                val destination = savedPacks.firstOrNull()?.identifier
                    ?: error("No selected stickers returned")
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

    private companion object {
        const val PREVIEW_EXTRACT_DEBOUNCE_MS = 250L
    }
}

fun ResolvedVideoStaticSticker.selectionKey(): String =
    "static:${plan.candidateId}:${plan.timestampMs}:$localPath"

fun ResolvedVideoAnimatedSticker.selectionKey(index: Int): String =
    "animated:$index:${plan.timeline.firstOrNull()?.timestampMs ?: 0}:${plan.timeline.lastOrNull()?.timestampMs ?: 0}"

private fun Set<String>.toggle(key: String): Set<String> =
    if (key in this) this - key else this + key
