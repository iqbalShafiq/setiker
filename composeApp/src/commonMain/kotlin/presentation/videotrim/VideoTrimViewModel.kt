package presentation.videotrim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.storage.StickerFileStorage
import domain.model.StickerPack
import domain.util.AnimatedStickerValidator
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
import presentation.common.UiText

class VideoTrimViewModel(
    private val fileStorage: StickerFileStorage
) : ViewModel() {

    private val _state = MutableStateFlow(VideoTrimState())
    val state: StateFlow<VideoTrimState> = _state.asStateFlow()

    private val _effect = Channel<VideoTrimEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var playJob: Job? = null

    fun onIntent(intent: VideoTrimIntent) {
        when (intent) {
            is VideoTrimIntent.LoadVideo -> loadVideo(intent.videoPath)
            is VideoTrimIntent.UpdateTrimStart -> {
                stopPlayback()
                updateTrim(start = intent.ms)
            }
            is VideoTrimIntent.UpdateTrimEnd -> {
                stopPlayback()
                updateTrim(end = intent.ms)
            }
            is VideoTrimIntent.UpdateFps -> _state.update { it.copy(fps = intent.fps) }
            is VideoTrimIntent.UpdateSpeed -> _state.update { it.copy(speed = intent.speed) }
            is VideoTrimIntent.PlayPreview -> startPlayback()
            is VideoTrimIntent.PausePreview -> stopPlayback()
            is VideoTrimIntent.Confirm -> confirm()
            is VideoTrimIntent.Cancel -> {
                stopPlayback()
                viewModelScope.launch { _effect.send(VideoTrimEffect.NavigateBack) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playJob?.cancel()
    }

    private fun loadVideo(videoPath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, videoPath = videoPath, errorMessage = null) }
            val totalMs = fileStorage.getVideoDurationMs(videoPath)
                .takeIf { it > 0 } ?: StickerPack.MAX_ANIMATION_DURATION_MS
            val cappedEnd = totalMs.coerceAtMost(StickerPack.MAX_ANIMATION_DURATION_MS)
            _state.update {
                it.copy(
                    isLoading = false,
                    videoDurationMs = totalMs,
                    trimStartMs = 0L,
                    trimEndMs = cappedEnd
                )
            }
            // Extract a small thumbnail strip so the preview no longer looks blank
            // (Coil cannot render videos directly). Cheap; we sample a fixed number
            // across the full video and reuse them as both static + playback frames.
            extractThumbnailStrip(videoPath, totalMs)
        }
    }

    private suspend fun extractThumbnailStrip(videoPath: String, totalMs: Long) {
        val timestamps = buildList {
            val count = VideoTrimState.PREVIEW_THUMBNAIL_COUNT
            val span = totalMs.coerceAtLeast(1L)
            for (i in 0 until count) {
                val ms = if (count == 1) 0L else (i.toLong() * span / (count - 1))
                add(ms.coerceIn(0, totalMs - 1))
            }
        }
        val thumbs = mutableListOf<VideoTrimThumbnail>()
        timestamps.forEachIndexed { i, ms ->
            val name = "trim_thumb_${System.currentTimeMillis()}_$i.png"
            val path = fileStorage.extractVideoFrameToFile(videoPath, ms, name)
            if (path != null) {
                thumbs.add(VideoTrimThumbnail(timestampMs = ms, filePath = path))
                _state.update { it.copy(thumbnails = thumbs.toList()) }
            }
        }
    }

    private fun updateTrim(start: Long? = null, end: Long? = null) {
        _state.update { current ->
            val total = current.videoDurationMs.coerceAtLeast(1L)
            val newStart = (start ?: current.trimStartMs).coerceIn(0L, total - 1)
            val newEnd = (end ?: current.trimEndMs).coerceIn(newStart + 1, total)
            val maxAllowed = StickerPack.MAX_ANIMATION_DURATION_MS
            val clampedEnd = if (newEnd - newStart > maxAllowed) newStart + maxAllowed else newEnd
            current.copy(trimStartMs = newStart, trimEndMs = clampedEnd, currentThumbIndex = 0)
        }
    }

    private fun startPlayback() {
        if (_state.value.isPlaying) return
        if (!_state.value.canPlay) return
        _state.update { it.copy(isPlaying = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            // Frame interval driven by user-selected fps + speed so play preview
            // matches the speed they will see in the final sticker.
            val intervalProvider = {
                val fps = _state.value.fps.coerceAtLeast(1)
                val speed = _state.value.speed.coerceAtLeast(0.1f)
                val baseMs = 1000L / fps
                (baseMs / speed).toLong().coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
            }
            while (isActive && _state.value.isPlaying) {
                val pool = _state.value.thumbnailsInRange
                if (pool.isEmpty()) break
                _state.update { it.copy(currentThumbIndex = (it.currentThumbIndex + 1) % pool.size) }
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

    private fun confirm() {
        val current = _state.value
        if (current.videoPath.isBlank()) return
        if (current.effectiveTrimMs <= 0) return

        val spec = current.toSpec()
        val validation = AnimatedStickerValidator.validateSpec(spec)
        if (validation is AnimatedStickerValidator.Result.Failure) {
            viewModelScope.launch {
                _effect.send(
                    VideoTrimEffect.ShowError(UiText.DynamicString("Invalid animation spec: ${validation.reason}"))
                )
            }
            return
        }

        // Hand off to VideoCrop — the user picks the square region; frame decoding happens
        // there once they tap Apply, so we never silently scale or center-crop.
        stopPlayback()
        viewModelScope.launch {
            _effect.send(VideoTrimEffect.NavigateToVideoCrop(current.videoPath, spec))
        }
    }
}
