package presentation.backgroundremover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BackgroundRemoverViewModel : ViewModel() {

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
            is BackgroundRemoverIntent.Reset -> {
                _state.update {
                    it.copy(
                        paths = emptyList(),
                        removedBackgroundPath = null,
                        brushSize = 20f,
                        isErasing = true
                    )
                }
            }
        }
    }

    private fun autoRemove() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            
            try {
                // TODO: Implement auto-remove logic using platform-specific ML/image processing
                val currentState = _state.value
                val resultPath = currentState.imagePath // Placeholder
                
                _state.update { it.copy(isProcessing = false, removedBackgroundPath = resultPath) }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
                _effect.send(BackgroundRemoverEffect.ShowError(e.message ?: "Failed to remove background"))
            }
        }
    }

    private fun applyRemoval() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            
            try {
                // TODO: Implement actual background removal logic
                val currentState = _state.value
                val resultPath = currentState.imagePath // Placeholder
                
                _state.update { it.copy(isProcessing = false, removedBackgroundPath = resultPath) }
                _effect.send(BackgroundRemoverEffect.BackgroundRemoved(resultPath))
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message) }
                _effect.send(BackgroundRemoverEffect.ShowError(e.message ?: "Failed to apply removal"))
            }
        }
    }
}
