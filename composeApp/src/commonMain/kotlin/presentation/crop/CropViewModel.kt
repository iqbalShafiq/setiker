package presentation.crop

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.util.applyCropTransformation
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_crop_image

class CropViewModel : ViewModel() {

    private val _state = MutableStateFlow(CropState())
    val state: StateFlow<CropState> = _state.asStateFlow()

    private val _effect = Channel<CropEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: CropIntent) {
        when (intent) {
            is CropIntent.LoadImage -> {
                _state.update { it.copy(imagePath = intent.path) }
            }
            is CropIntent.RotateLeft -> {
                _state.update { it.copy(rotation = it.rotation - 90f) }
            }
            is CropIntent.RotateRight -> {
                _state.update { it.copy(rotation = it.rotation + 90f) }
            }
            is CropIntent.FlipHorizontal -> {
                _state.update { it.copy(isFlippedHorizontal = !it.isFlippedHorizontal) }
            }
            is CropIntent.FlipVertical -> {
                _state.update { it.copy(isFlippedVertical = !it.isFlippedVertical) }
            }
            is CropIntent.UpdateScale -> {
                _state.update { it.copy(scale = intent.scale.coerceIn(0.5f, 4f)) }
            }
            is CropIntent.UpdateOffset -> {
                _state.update {
                    it.copy(
                        offsetX = intent.x.coerceIn(-1f, 1f),
                        offsetY = intent.y.coerceIn(-1f, 1f)
                    )
                }
            }
            is CropIntent.ApplyCrop -> applyCrop()
            is CropIntent.Reset -> {
                _state.update {
                    it.copy(
                        rotation = 0f,
                        scale = 1f,
                        offsetX = 0f,
                        offsetY = 0f,
                        isFlippedHorizontal = false,
                        isFlippedVertical = false
                    )
                }
            }
        }
    }

    private fun applyCrop() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }

            try {
                val currentState = _state.value
                val croppedPath = applyCropTransformation(
                    sourcePath = currentState.imagePath,
                    scale = currentState.scale,
                    rotation = currentState.rotation,
                    offsetX = currentState.offsetX,
                    offsetY = currentState.offsetY,
                    flipHorizontal = currentState.isFlippedHorizontal,
                    flipVertical = currentState.isFlippedVertical
                )

                _state.update { it.copy(isProcessing = false, croppedImagePath = croppedPath) }
                _effect.send(CropEffect.ImageCropped(croppedPath))
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
                _effect.send(
                    CropEffect.ShowError(
                        e.toUiText(Res.string.error_failed_crop_image)
                    )
                )
            }
        }
    }
}
