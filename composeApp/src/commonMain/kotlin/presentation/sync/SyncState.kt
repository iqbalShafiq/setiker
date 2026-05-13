package presentation.sync

import domain.model.SyncOperation
import domain.model.SyncReport

data class SyncState(
    val operations: List<SyncOperation> = emptyList(),
    val isSyncing: Boolean = false,
    val lastReport: SyncReport? = null,
    val isLoading: Boolean = true
)