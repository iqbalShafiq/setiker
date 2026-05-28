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
import domain.model.StickerDraftInput
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
            is VideoStickerPackIntent.UpdatePrompt -> _state.update { it.copy(prompt = intent.value, generatedPlan = null) }
            is VideoStickerPackIntent.UpdatePackName -> _state.update { it.copy(packName = intent.value) }
            is VideoStickerPackIntent.UpdatePublisher -> _state.update { it.copy(publisher = intent.value) }
            VideoStickerPackIntent.Generate -> generate(extractFreshCandidates = true)
            VideoStickerPackIntent.Regenerate -> generate(extractFreshCandidates = false)
            VideoStickerPackIntent.SavePack -> savePack()
        }
    }

    private fun loadVideo(path: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingVideo = true, videoPath = path, previewFramePath = null, errorMessage = null) }
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
                        candidates = emptyList(),
                        candidateGrids = emptyList(),
                        candidateManifest = emptyList(),
                        generatedPlan = null
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
                generatedPlan = null
            )
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

                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        processingProgress = 1f,
                        generatedPlan = generated
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
                val identifier = PackIdentifierSanitizer.sanitize(current.packName, Random.nextInt(1000, 9999))
                val staticInputs = generated.staticStickers.map { sticker ->
                    StickerDraftInput.StickerInput(
                        imagePath = sticker.localPath,
                        decorations = sticker.plan.decorations
                    )
                }
                val animatedInputs = generated.animatedStickers.mapIndexed { index, sticker ->
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
                val stickerInputs = staticInputs + animatedInputs
                val tray = generated.staticStickers.firstOrNull()?.localPath
                    ?: animatedInputs.firstOrNull()?.imagePath
                    ?: error("No generated stickers returned")
                val pack = draftSaver.buildDraftPack(
                    StickerDraftInput(
                        identifier = identifier,
                        name = current.packName,
                        publisher = current.publisher,
                        visibility = "PRIVATE",
                        trayImagePath = tray,
                        stickers = stickerInputs
                    )
                )
                stickerRepository.savePack(pack)
                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        processingProgress = 1f
                    )
                }
                _effect.send(VideoStickerPackEffect.NavigateToPackDetail(pack.identifier))
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
