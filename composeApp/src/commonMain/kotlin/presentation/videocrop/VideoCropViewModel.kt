package presentation.videocrop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.storage.AnimatedStickerDraftStore
import data.storage.StickerFileStorage
import domain.model.AnimatedStickerSpec
import domain.model.CropTransform
import domain.model.StickerPack
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.withContext
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_decode_frames
import setiker.composeapp.generated.resources.error_failed_extract_preview
import util.decodeImageBitmapInfo

class VideoCropViewModel(
    private val fileStorage: StickerFileStorage,
    private val draftStore: AnimatedStickerDraftStore
) : ViewModel() {

    private val _state = MutableStateFlow(VideoCropState())
    val state: StateFlow<VideoCropState> = _state.asStateFlow()

    private val _effect = Channel<VideoCropEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var playJob: Job? = null

    fun onIntent(intent: VideoCropIntent) {
        when (intent) {
            is VideoCropIntent.Load -> load(intent.videoPath, intent.spec, intent.packId)
            is VideoCropIntent.UpdateScale -> _state.update {
                it.copy(
                    transform = it.transform.copy(
                        scale = intent.scale.coerceIn(CropTransform.MIN_SCALE, CropTransform.MAX_SCALE)
                    )
                )
            }
            is VideoCropIntent.UpdateOffset -> _state.update {
                it.copy(
                    transform = it.transform.copy(
                        offsetXNorm = intent.xNorm.coerceIn(-1f, 1f),
                        offsetYNorm = intent.yNorm.coerceIn(-1f, 1f)
                    )
                )
            }
            is VideoCropIntent.RotateLeft -> _state.update {
                it.copy(transform = it.transform.copy(rotation = it.transform.rotation - 90f))
            }
            is VideoCropIntent.RotateRight -> _state.update {
                it.copy(transform = it.transform.copy(rotation = it.transform.rotation + 90f))
            }
            is VideoCropIntent.FlipHorizontal -> _state.update {
                it.copy(transform = it.transform.copy(flipHorizontal = !it.transform.flipHorizontal))
            }
            is VideoCropIntent.Reset -> _state.update { it.copy(transform = CropTransform()) }
            is VideoCropIntent.PlayPreview -> startPlayback()
            is VideoCropIntent.PausePreview -> stopPlayback()
            is VideoCropIntent.Apply -> apply()
            is VideoCropIntent.Cancel -> {
                stopPlayback()
                viewModelScope.launch { _effect.send(VideoCropEffect.NavigateBack) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
    }

    private fun load(videoPath: String, spec: AnimatedStickerSpec, packId: String) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    videoPath = videoPath,
                    spec = spec,
                    packId = packId,
                    isLoadingPreview = true,
                    errorMessage = null,
                    transform = CropTransform(),
                    previewFrames = emptyList(),
                    currentPreviewIndex = 0
                )
            }

            // Extract a small uniform set of preview frames inside the trim range so the
            // user can play/pause to verify the crop area against actual motion.
            val frames = mutableListOf<VideoCropPreviewFrame>()
            val span = (spec.trimEndMs - spec.trimStartMs).coerceAtLeast(1L)
            val count = VideoCropState.PREVIEW_FRAME_COUNT
            for (i in 0 until count) {
                val ms = if (count == 1) spec.trimStartMs
                else spec.trimStartMs + i.toLong() * span / (count - 1)
                val name = "crop_preview_${System.currentTimeMillis()}_$i.png"
                val path = fileStorage.extractVideoFrameToFile(videoPath, ms, name)
                if (path != null) {
                    frames.add(VideoCropPreviewFrame(timestampMs = ms, filePath = path))
                    _state.update {
                        it.copy(
                            previewFrames = frames.toList(),
                            isLoadingPreview = false
                        )
                    }
                }
            }
            if (frames.isEmpty()) {
                _state.update { it.copy(isLoadingPreview = false, errorMessage = "Could not load preview") }
                _effect.send(VideoCropEffect.ShowError(UiText.StringRes(Res.string.error_failed_extract_preview)))
                return@launch
            }
            // Use first frame to populate dimensions (good enough — we keep them fixed).
            val info = decodeImageBitmapInfo(frames.first().filePath)
            _state.update {
                it.copy(
                    previewWidth = info?.width ?: 0,
                    previewHeight = info?.height ?: 0
                )
            }
        }
    }

    private fun startPlayback() {
        if (_state.value.isPlaying) return
        if (!_state.value.canPlay) return
        _state.update { it.copy(isPlaying = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            // Mirror the trim screen: a small uniform set of preview frames spans
            // the trim range, so the natural playback interval is
            // trimDuration / frameCount / speed. Using 1000/fps here made playback
            // race through all 8 previews in well under a second regardless of the
            // real trim length, which felt glitchy and made the speed chips look
            // like they did nothing.
            val intervalProvider = {
                val state = _state.value
                val spec = state.spec
                val frames = state.previewFrames.size.coerceAtLeast(1)
                val speed = (spec?.speed ?: 1f).coerceAtLeast(0.1f)
                val trimMs = ((spec?.trimEndMs ?: 0L) - (spec?.trimStartMs ?: 0L)).coerceAtLeast(1L)
                val perFrameMs = (trimMs.toDouble() / frames / speed).toLong()
                perFrameMs.coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
            }
            while (isActive && _state.value.isPlaying) {
                val pool = _state.value.previewFrames
                if (pool.isEmpty()) break
                _state.update { it.copy(currentPreviewIndex = (it.currentPreviewIndex + 1) % pool.size) }
                delay(intervalProvider())
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

    private fun apply() {
        val current = _state.value
        val spec = current.spec ?: return
        if (current.videoPath.isBlank()) return

        stopPlayback()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isApplying = true,
                    applyProgress = 0f,
                    applyProgressLabel = null,
                    errorMessage = null
                )
            }
            try {
                val finalSpec = spec.copy(cropTransform = current.transform)
                val frames = fileStorage.decodeVideoFrames(
                    videoPath = current.videoPath,
                    spec = finalSpec,
                    onProgress = { current2, total ->
                        // Forward real progress so the dialog shows actual frame counts,
                        // not a fake animation. Hop back to the main dispatcher because
                        // the storage callback fires on the IO/main thread depending on
                        // the source media; updating StateFlow from any thread is safe
                        // but Compose recomposes more reliably on the main thread.
                        val safeTotal = total.coerceAtLeast(1)
                        val pct = (current2.toFloat() / safeTotal).coerceIn(0f, 1f)
                        _state.update {
                            it.copy(
                                applyProgress = pct,
                                applyProgressLabel = "$current2 / $safeTotal frames"
                            )
                        }
                    }
                )
                if (frames.isEmpty()) {
                    _state.update { it.copy(isApplying = false, errorMessage = "No frames extracted") }
                    _effect.send(VideoCropEffect.ShowError(UiText.StringRes(Res.string.error_failed_decode_frames)))
                    return@launch
                }
                val draftId = withContext(Dispatchers.Default) {
                    draftStore.put(
                        AnimatedStickerDraftStore.Draft(
                            videoPath = current.videoPath,
                            frames = frames,
                            spec = finalSpec
                        )
                    )
                }
                _state.update { it.copy(isApplying = false, applyProgress = 1f) }
                _effect.send(VideoCropEffect.NavigateToAnimatedEditor(draftId))
            } catch (e: Exception) {
                _state.update { it.copy(isApplying = false, errorMessage = e.message) }
                _effect.send(
                    VideoCropEffect.ShowError(e.toUiText(Res.string.error_failed_decode_frames))
                )
            }
        }
    }
}
