package presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.remote.ExploreApiRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText

class ProcessingHistoryViewModel(
    private val exploreApiRepository: ExploreApiRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ProcessingHistoryState())
    val state: StateFlow<ProcessingHistoryState> = _state.asStateFlow()

    private val _effect = Channel<ProcessingHistoryEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: ProcessingHistoryIntent) {
        when (intent) {
            ProcessingHistoryIntent.Load -> load()
            is ProcessingHistoryIntent.ChangeFilter -> {
                _state.update { it.copy(typeFilter = intent.type) }
                load()
            }
            is ProcessingHistoryIntent.DeleteItem -> deleteItem(intent.id)
            ProcessingHistoryIntent.ClearAll -> clearAll()
            ProcessingHistoryIntent.NavigateBack -> viewModelScope.launch { _effect.send(ProcessingHistoryEffect.NavigateBack) }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                exploreApiRepository.getProcessingHistory(_state.value.typeFilter)
            }.onSuccess { items ->
                _state.update { it.copy(isLoading = false, items = items) }
            }.onFailure { error ->
                _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load history") }
                _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.DynamicString(error.message ?: "Failed to load history")))
            }
        }
    }

    private fun deleteItem(id: String) {
        viewModelScope.launch {
            runCatching { exploreApiRepository.deleteHistoryItem(id) }
                .onSuccess {
                    _state.update { it.copy(items = it.items.filterNot { item -> item.id == id }) }
                }
                .onFailure { error ->
                    _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.DynamicString(error.message ?: "Failed to delete history item")))
                }
        }
    }

    private fun clearAll() {
        viewModelScope.launch {
            _state.update { it.copy(isClearing = true) }
            runCatching {
                exploreApiRepository.clearHistory(_state.value.typeFilter)
            }.onSuccess {
                _state.update { it.copy(isClearing = false, items = emptyList()) }
            }.onFailure { error ->
                _state.update { it.copy(isClearing = false) }
                _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.DynamicString(error.message ?: "Failed to clear history")))
            }
        }
    }
}
