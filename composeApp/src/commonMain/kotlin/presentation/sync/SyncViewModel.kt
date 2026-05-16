package presentation.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.sync.SyncManager
import domain.model.SyncOperationStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SyncViewModel(private val syncManager: SyncManager) : ViewModel() {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()
    
    init {
        viewModelScope.launch { syncManager.operationsFlow.collect { ops -> _state.update { it.copy(operations = ops, isLoading = false) } } }
        viewModelScope.launch { syncManager.isSyncing.collect { syncing -> _state.update { it.copy(isSyncing = syncing) } } }
        viewModelScope.launch { syncManager.syncStage.collect { stage -> _state.update { it.copy(syncStage = stage) } } }
        viewModelScope.launch { syncManager.lastSyncReport.collect { report -> _state.update { it.copy(lastReport = report) } } }
    }
    
    fun onIntent(intent: SyncIntent) {
        when (intent) {
            is SyncIntent.SyncNow -> viewModelScope.launch { syncManager.sync() }
            is SyncIntent.RetryOperation -> viewModelScope.launch { syncManager.retry(intent.operationId) }
            is SyncIntent.CancelOperation -> viewModelScope.launch { syncManager.cancel(intent.operationId) }
            is SyncIntent.ClearCompleted -> viewModelScope.launch { syncManager.clearCompleted() }
            else -> {}
        }
    }
}
