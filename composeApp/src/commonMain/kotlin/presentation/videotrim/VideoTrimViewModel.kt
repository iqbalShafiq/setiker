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
import kotlin.time.Clock
import presentation.common.UiText
import presentation.common.toUiText

class VideoTrimViewModel(
    private val fileStorage: StickerFileStorage
) : ViewModel() {

    private val _state = MutableStateFlow(VideoTrimState())
    val state: StateFlow<VideoTrimState> = _state.asStateFlow()

    private val _effect = Channel<VideoTrimEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var playJob: Job? = null

    /**
     * Background thumbnail extraction tied to the latest trim window. Every time
     * the user nudges the trim slider we cancel this and reschedule, so the
     * preview frames we cycle during playback always cover exactly the current
     * trim range. Without that, a small trim of a long source would fall back to
     * 2-3 in-range frames and the playback would look like a slideshow no matter
     * what speed was selected.
     */
    private var extractJob: Job? = null

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
        extractJob?.cancel()
    }

    private fun loadVideo(videoPath: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, videoPath = videoPath, error = null) }
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
            // Run the first extraction immediately (no debounce) so the user
            // doesn't watch an empty preview for 300 ms after the screen opens.
            rescheduleThumbnailExtraction(immediate = true)
        }
    }

    private fun rescheduleThumbnailExtraction(immediate: Boolean = false) {
        extractJob?.cancel()
        val snapshot = _state.value
        if (snapshot.videoPath.isBlank()) return
        if (snapshot.trimEndMs <= snapshot.trimStartMs) return

        val videoPath = snapshot.videoPath

        extractJob = viewModelScope.launch {
            // Debounce the slider drag — only blow away the existing thumbnails
            // (and start re-extracting) when the user has paused for a moment.
            // While they're actively dragging we keep the previous trim's frames
            // visible so the preview doesn't flash to a loader on every tick.
            if (!immediate) delay(TRIM_EXTRACT_DEBOUNCE_MS)
            val current = _state.value
            val startMs = current.trimStartMs
            val endMs = current.trimEndMs
            if (endMs <= startMs) return@launch
            _state.update {
                it.copy(
                    thumbnails = emptyList(),
                    currentThumbIndex = 0,
                    isExtracting = true
                )
            }
            extractThumbnailsForRange(videoPath, startMs, endMs)
        }
    }

    private suspend fun extractThumbnailsForRange(
        videoPath: String,
        startMs: Long,
        endMs: Long
    ) {
        val count = VideoTrimState.PREVIEW_THUMBNAIL_COUNT
        val span = (endMs - startMs).coerceAtLeast(1L)
        val timestamps = (0 until count).map { i ->
            if (count == 1) startMs
            else startMs + (i.toLong() * span / (count - 1))
        }
        val thumbs = mutableListOf<VideoTrimThumbnail>()
        try {
            timestamps.forEachIndexed { i, ms ->
                val name = "trim_thumb_${Clock.System.now().toEpochMilliseconds()}_$i.png"
                val path = fileStorage.extractVideoFrameToFile(videoPath, ms, name)
                if (path != null) {
                    thumbs.add(VideoTrimThumbnail(timestampMs = ms, filePath = path))
                    _state.update { it.copy(thumbnails = thumbs.toList()) }
                }
            }
        } finally {
            _state.update { it.copy(isExtracting = false) }
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
        // Re-extract thumbnails for the new range so playback density stays high.
        // Debounced inside rescheduleThumbnailExtraction so dragging the slider
        // doesn't fire off dozens of extract jobs.
        rescheduleThumbnailExtraction(immediate = false)
    }

    private fun startPlayback() {
        if (_state.value.isPlaying) return
        if (!_state.value.canPlay) return
        _state.update { it.copy(isPlaying = true) }
        playJob?.cancel()
        playJob = viewModelScope.launch {
            // Playback interval reflects the REAL trim duration at the chosen
            // speed. Thumbnails are extracted to span exactly the current trim
            // window (re-extracted on every trim change), so dividing the trim
            // span by frame count gives us a loop that runs for trimDuration /
            // speed seconds — 1x preview matches actual playback length, 2x is
            // twice as fast. Frame count is high enough (PREVIEW_THUMBNAIL_COUNT)
            // that the preview reads as video instead of a slideshow.
            val intervalProvider = {
                val state = _state.value
                val pool = state.thumbnailsInRange
                val frames = pool.size.coerceAtLeast(1)
                val speed = state.speed.coerceAtLeast(0.1f)
                val trimMs = state.effectiveTrimMs.coerceAtLeast(1L)
                val perFrameMs = (trimMs.toDouble() / frames / speed).toLong()
                perFrameMs.coerceAtLeast(StickerPack.MIN_FRAME_DURATION_MS)
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
            val errorText = validation.reason.toUiText()
            _state.update { it.copy(error = errorText) }
            viewModelScope.launch {
                _effect.send(VideoTrimEffect.ShowError(errorText))
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

    private companion object {
        /**
         * Wait this long after the last trim slider tick before kicking off a new
         * extraction. Long enough to swallow a rapid drag (we don't want to spin
         * up 40 frame extractions per pixel of movement), short enough that the
         * preview refreshes almost immediately once the user lets go.
         */
        private const val TRIM_EXTRACT_DEBOUNCE_MS = 250L
    }
}
