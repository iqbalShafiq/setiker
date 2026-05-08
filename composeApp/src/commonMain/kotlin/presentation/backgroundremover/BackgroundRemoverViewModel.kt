package presentation.backgroundremover

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.StickerApiRepository
import data.util.applyMaskToImage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_failed_apply_removal
import setiker.composeapp.generated.resources.error_failed_remove_background

class BackgroundRemoverViewModel(
    private val apiRepository: StickerApiRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BackgroundRemoverState())
    val state: StateFlow<BackgroundRemoverState> = _state.asStateFlow()

    private val _effect = Channel<BackgroundRemoverEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: BackgroundRemoverIntent) {
        when (intent) {
            is BackgroundRemoverIntent.LoadImage -> {
                _state.update { it.copy(imagePath = intent.path) }
            }
            is BackgroundRemoverIntent.UpdateBrushSize -> {
                _state.update { it.copy(brushSize = intent.size) }
            }
            is BackgroundRemoverIntent.ToggleMode -> {
                _state.update { it.copy(isErasing = !it.isErasing) }
            }
            is BackgroundRemoverIntent.AddPath -> {
                _state.update { it.copy(paths = it.paths + intent.path) }
            }
            is BackgroundRemoverIntent.Undo -> {
                _state.update {
                    if (it.paths.isNotEmpty()) {
                        it.copy(paths = it.paths.dropLast(1))
                    } else it
                }
            }
            is BackgroundRemoverIntent.ClearAll -> {
                _state.update { it.copy(paths = emptyList()) }
            }
            is BackgroundRemoverIntent.AutoRemove -> autoRemove()
            is BackgroundRemoverIntent.ApplyRemoval -> applyRemoval()
            is BackgroundRemoverIntent.ConfirmResult -> confirmResult()
            is BackgroundRemoverIntent.DismissResultSheet -> {
                _state.update { it.copy(isResultSheetOpen = false, removedBackgroundPath = null) }
            }
            is BackgroundRemoverIntent.Reset -> {
                _state.update {
                    it.copy(
                        paths = emptyList(),
                        removedBackgroundPath = null,
                        isResultSheetOpen = false,
                        brushSize = 20f,
                        isErasing = true
                    )
                }
            }
            is BackgroundRemoverIntent.UpdateCanvasSize -> {
                _state.update {
                    it.copy(
                        canvasWidth = intent.width,
                        canvasHeight = intent.height
                    )
                }
            }
        }
    }

    private fun autoRemove() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }

            try {
                // Auto-remove requires platform-specific ML library (e.g., ML Kit on Android, Vision on iOS)
                // For now, apply the current brush paths as a basic removal
                val currentState = _state.value
                val resultPath = if (currentState.paths.isNotEmpty()) {
                    applyMaskToImage(
                        imagePath = currentState.imagePath,
                        paths = currentState.paths,
                        canvasSize = IntSize(512, 512)
                    )
                } else {
                    currentState.imagePath
                }

                _state.update { it.copy(isProcessing = false, removedBackgroundPath = resultPath) }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
                _effect.send(
                    BackgroundRemoverEffect.ShowError(
                        e.message?.let(UiText::DynamicString)
                            ?: UiText.StringRes(Res.string.error_failed_remove_background)
                    )
                )
            }
        }
    }

    private fun applyRemoval() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }

            try {
                val currentState = _state.value
                val resultPath = apiRepository.removeBackground(currentState.imagePath)

                _state.update {
                    it.copy(
                        isProcessing = false,
                        removedBackgroundPath = resultPath,
                        isResultSheetOpen = true
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
                _effect.send(
                    BackgroundRemoverEffect.ShowError(
                        e.message?.let(UiText::DynamicString)
                            ?: UiText.StringRes(Res.string.error_failed_apply_removal)
                    )
                )
            }
        }
    }

    private fun confirmResult() {
        viewModelScope.launch {
            val resultPath = _state.value.removedBackgroundPath ?: return@launch
            _state.update { it.copy(isResultSheetOpen = false) }
            _effect.send(BackgroundRemoverEffect.BackgroundRemoved(resultPath))
        }
    }
}
