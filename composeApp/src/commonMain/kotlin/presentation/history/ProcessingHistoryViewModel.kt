package presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.local.dao.ProcessingHistoryCacheDao
import data.local.entity.ProcessingHistoryCacheEntity
import data.remote.ExploreApiRepository
import data.remote.model.ProcessingHistoryItem
import data.remote.model.ProcessingHistoryOutputFile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import presentation.common.UiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.report_submitted

class ProcessingHistoryViewModel(
    private val exploreApiRepository: ExploreApiRepository,
    private val historyCacheDao: ProcessingHistoryCacheDao? = null
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
            is ProcessingHistoryIntent.ShowReport -> _state.update {
                it.copy(showReportSheet = true, reportTargetId = intent.id, reportReason = null, reportDetails = "")
            }
            ProcessingHistoryIntent.DismissReport -> _state.update { it.copy(showReportSheet = false) }
            is ProcessingHistoryIntent.SelectReportReason -> _state.update { it.copy(reportReason = intent.reason) }
            is ProcessingHistoryIntent.UpdateReportDetails -> _state.update { it.copy(reportDetails = intent.value) }
            ProcessingHistoryIntent.SubmitReport -> submitReport()
        }
    }

    private fun submitReport() {
        val id = _state.value.reportTargetId ?: return
        val reason = _state.value.reportReason ?: return
        viewModelScope.launch {
            _state.update { it.copy(isSubmittingReport = true) }
            runCatching {
                exploreApiRepository.reportProcessingHistory(id, reason, _state.value.reportDetails.takeIf { it.isNotBlank() })
            }.onSuccess {
                _state.update { it.copy(isSubmittingReport = false, showReportSheet = false) }
                _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.StringRes(Res.string.report_submitted)))
            }.onFailure { error ->
                _state.update { it.copy(isSubmittingReport = false) }
                _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.DynamicString(error.message ?: "Report failed")))
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null, isShowingCachedData = false) }
            runCatching {
                exploreApiRepository.getProcessingHistory(_state.value.typeFilter)
            }.onSuccess { items ->
                cacheItems(items)
                _state.update { it.copy(isLoading = false, items = items, isShowingCachedData = false) }
            }.onFailure { error ->
                val cached = loadCachedItems()
                if (cached.isNotEmpty()) {
                    _state.update {
                        it.copy(isLoading = false, items = cached, isShowingCachedData = true, error = null)
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = error.message ?: "Failed to load history") }
                    _effect.send(ProcessingHistoryEffect.ShowMessage(UiText.DynamicString(error.message ?: "Failed to load history")))
                }
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

    private suspend fun cacheItems(items: List<ProcessingHistoryItem>) {
        val dao = historyCacheDao ?: return
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        dao.replaceAll(
            items.map { item ->
                ProcessingHistoryCacheEntity(
                    id = item.id,
                    type = item.type,
                    outputCount = item.outputFiles.size,
                    previewUrl = item.outputFiles.firstOrNull()?.url,
                    createdAt = now,
                    syncedAt = now
                )
            }
        )
    }

    private suspend fun loadCachedItems(): List<ProcessingHistoryItem> {
        val dao = historyCacheDao ?: return emptyList()
        val type = _state.value.typeFilter
        val entities = if (type == null) {
            dao.observeAll()
        } else {
            dao.observeByType(type)
        }
        return entities.first().map { entity ->
            val outputs = entity.previewUrl?.let { url ->
                listOf(ProcessingHistoryOutputFile(url = url))
            } ?: emptyList()
            ProcessingHistoryItem(
                id = entity.id,
                userId = "",
                type = entity.type,
                outputFiles = outputs,
                expiresAt = "",
                createdAt = entity.createdAt.toString()
            )
        }
    }
}
