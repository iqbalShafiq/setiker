package presentation.sync

import domain.model.SyncOperation
import domain.model.SyncReport
import domain.model.SyncStage

data class SyncState(
    val operations: List<SyncOperation> = emptyList(),
    val isSyncing: Boolean = false,
    val syncStage: SyncStage = SyncStage.IDLE,
    val lastReport: SyncReport? = null,
    val isLoading: Boolean = true
)
