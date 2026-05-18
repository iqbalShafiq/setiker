package presentation.videostickerpack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
import data.repository.StickerPackDraftSaver
import data.storage.StickerFileStorage
import data.video.CandidateGridComposer
import data.video.VideoFrameCandidateExtractor
import domain.model.StickerDraftInput
import domain.repository.StickerRepository
import domain.util.VideoStickerPackPlanner
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
            is VideoStickerPackIntent.UpdatePackName -> _state.update { it.copy(packName = intent.value) }
            is VideoStickerPackIntent.UpdatePublisher -> _state.update { it.copy(publisher = intent.value) }
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
                    generatedStickers = emptyList()
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
            current.copy(selectedStartMs = nextStart, selectedEndMs = nextEnd, generatedStickers = emptyList())
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
                val grids = if (extractFreshCandidates || current.candidateGrids.isEmpty()) {
                    val candidates = extractor.extractCandidates(
                        videoPath = current.videoPath,
                        startMs = current.selectedStartMs,
                        endMs = current.selectedEndMs,
                        onProgress = { done, total ->
                            _state.update { it.copy(processingProgress = done.toFloat() / total.coerceAtLeast(1)) }
                        }
                    )
                    if (candidates.isEmpty()) error("No usable frames extracted")
                    _state.update {
                        it.copy(
                            candidates = candidates,
                            processingStep = VideoStickerPackProcessingStep.BuildingGrids
                        )
                    }
                    gridComposer.composeGrids(candidates.map { it.filePath }).also { composed ->
                        _state.update { it.copy(candidateGrids = composed) }
                    }
                } else {
                    current.candidateGrids
                }

                _state.update {
                    it.copy(
                        processingStep = VideoStickerPackProcessingStep.AskingAi,
                        processingProgress = 0f
                    )
                }
                val generated = apiRepository.generateVideoStickerPack(
                    candidateGridPaths = grids.map { it.filePath },
                    candidateCount = grids.sumOf { it.frameCount },
                    selectedStartMs = current.selectedStartMs,
                    selectedEndMs = current.selectedEndMs,
                    sourceDurationMs = current.sourceDurationMs
                )
                if (generated.isEmpty()) error("No generated stickers returned")

                _state.update {
                    it.copy(
                        isProcessing = false,
                        processingStep = null,
                        processingProgress = 1f,
                        generatedStickers = generated
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
                val identifier = PackIdentifierSanitizer.sanitize(current.packName, Random.nextInt(1000, 9999))
                val tray = current.generatedStickers.first().localPath
                val pack = draftSaver.buildDraftPack(
                    StickerDraftInput(
                        identifier = identifier,
                        name = current.packName,
                        publisher = current.publisher,
                        visibility = "PRIVATE",
                        trayImagePath = tray,
                        stickers = current.generatedStickers.map { file ->
                            StickerDraftInput.StickerInput(
                                imagePath = file.localPath,
                                decorations = file.decorations
                            )
                        }
                    )
                )
                stickerRepository.savePack(pack)
                _effect.send(VideoStickerPackEffect.NavigateToPackDetail(pack.identifier))
            } catch (e: Exception) {
                _effect.send(
                    VideoStickerPackEffect.ShowError(
                        UiText.DynamicString(e.message ?: "Failed to save pack.")
                    )
                )
            }
        }
    }
}
