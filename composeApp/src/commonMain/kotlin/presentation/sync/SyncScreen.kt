package presentation.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlin.time.Clock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.SyncOperation
import domain.model.SyncOperationStatus
import domain.model.SyncOperationType
import domain.model.SyncReport
import domain.model.SyncResult
import domain.model.SyncStage
import presentation.components.AppTopBar
import presentation.components.AppPrimaryButton
import presentation.components.AppSecondaryButton
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreenRoot(
    viewModel: SyncViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    SyncScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncScreen(
    state: SyncState,
    onIntent: (SyncIntent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Sync Status",
                onBackClick = onBackClick
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
                SyncStatusCard(
                isSyncing = state.isSyncing,
                syncStage = state.syncStage,
                lastReport = state.lastReport,
                pendingCount = state.operations.count { it.status == SyncOperationStatus.PENDING }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppPrimaryButton(
                    text = "Sync Now",
                    onClick = { onIntent(SyncIntent.SyncNow) },
                    enabled = !state.isSyncing,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                AppSecondaryButton(
                    text = "Clear Done",
                    onClick = { onIntent(SyncIntent.ClearCompleted) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Operations (${state.operations.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.operations.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No local changes. Cloud sync will run automatically when you are online.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.operations) { operation ->
                        SyncOperationItem(
                            operation = operation,
                            onRetry = { onIntent(SyncIntent.RetryOperation(operation.id)) },
                            onCancel = { onIntent(SyncIntent.CancelOperation(operation.id)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    syncStage: SyncStage,
    lastReport: SyncReport?,
    pendingCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = neubrutalCardSurface()
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, neubrutalBorderColor())
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (pendingCount > 0) Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (pendingCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = syncTitle(isSyncing, syncStage, pendingCount, lastReport),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = syncDetail(lastReport),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    lastReport?.let {
                        Text(
                            text = "Last sync: ${formatTimestamp(it.timestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun syncTitle(
    isSyncing: Boolean,
    syncStage: SyncStage,
    pendingCount: Int,
    lastReport: SyncReport?,
): String {
    if (isSyncing) {
        return when (syncStage) {
            SyncStage.PUSHING_LOCAL -> "Syncing local changes"
            SyncStage.PULLING_REMOTE -> "Downloading cloud changes"
            SyncStage.WAITING_FOR_INTERNET -> "Waiting for internet"
            SyncStage.SIGN_IN_REQUIRED -> "Sign in to sync"
            SyncStage.IDLE -> "Syncing"
        }
    }
    return when {
        lastReport?.result is SyncResult.SkippedOffline -> "Waiting for internet"
        lastReport?.result is SyncResult.SkippedNotAuthenticated -> "Sign in to sync"
        lastReport?.result is SyncResult.SkippedInProgress -> "Sync already running"
        lastReport?.remoteDownloadFailures?.let { it > 0 } == true -> "Synced with warnings"
        pendingCount > 0 -> "$pendingCount pending"
        else -> "Up to date"
    }
}

private fun syncDetail(lastReport: SyncReport?): String {
    val report = lastReport ?: return "Manual and background sync are ready."
    return when (val result = report.result) {
        is SyncResult.Failed -> result.error
        SyncResult.SkippedOffline -> "Sync will resume automatically when your phone is online."
        SyncResult.SkippedNotAuthenticated -> "Log in to upload local changes and download cloud packs."
        SyncResult.SkippedInProgress -> "Another sync is already in progress."
        SyncResult.Success -> buildString {
            append("Uploaded ${report.operationsSucceeded}/${report.operationsProcessed}")
            append(" - Downloaded ${report.remotePacksDownloaded} packs")
            append(" and ${report.remoteStickersDownloaded} stickers")
            if (report.remoteItemsDeleted > 0) append(" - Removed ${report.remoteItemsDeleted}")
            if (report.remoteDownloadFailures > 0) append(" - ${report.remoteDownloadFailures} download issues")
        }
    }
}

@Composable
private fun SyncOperationItem(
    operation: SyncOperation,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = neubrutalCardSurface()
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, neubrutalBorderColor())
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${operation.type.name}: ${operation.targetId}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = "Status: ${operation.status.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (operation.status) {
                            SyncOperationStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
                            SyncOperationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
                            SyncOperationStatus.SUCCESS -> MaterialTheme.colorScheme.primary
                            SyncOperationStatus.FAILED -> MaterialTheme.colorScheme.error
                            SyncOperationStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                    operation.errorMessage?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                when (operation.status) {
                    SyncOperationStatus.FAILED -> {
                        Row {
                            TextButton(onClick = onRetry) {
                                Text("Retry", color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = onCancel) {
                                Text("Remove", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    SyncOperationStatus.PENDING -> {
                        TextButton(onClick = onCancel) {
                            Text("Cancel", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun SyncScreenPreview() {
    MaterialTheme {
        SyncScreen(
            state = SyncState(
                operations = listOf(
                    SyncOperation(
                        id = "1",
                        type = SyncOperationType.CREATE_PACK,
                        targetId = "pack_1",
                        payload = "{}",
                        status = SyncOperationStatus.SUCCESS,
                        createdAt = 0L
                    ),
                    SyncOperation(
                        id = "2",
                        type = SyncOperationType.ADD_STICKER,
                        targetId = "pack_2",
                        payload = "{}",
                        status = SyncOperationStatus.PENDING,
                        createdAt = 0L
                    ),
                    SyncOperation(
                        id = "3",
                        type = SyncOperationType.DELETE_PACK,
                        targetId = "pack_3",
                        payload = "{}",
                        status = SyncOperationStatus.FAILED,
                        errorMessage = "Network error",
                        createdAt = 0L
                    )
                ),
                isSyncing = false,
                isLoading = false
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun SyncScreenLoadingPreview() {
    MaterialTheme {
        SyncScreen(
            state = SyncState(isLoading = true),
            onIntent = {},
            onBackClick = {}
        )
    }
}

@Preview
@Composable
private fun SyncScreenSyncingPreview() {
    MaterialTheme {
        SyncScreen(
            state = SyncState(
                operations = listOf(
                    SyncOperation(
                        id = "1",
                        type = SyncOperationType.UPDATE_PACK,
                        targetId = "pack_1",
                        payload = "{}",
                        status = SyncOperationStatus.IN_PROGRESS,
                        createdAt = 0L
                    )
                ),
                isSyncing = true,
                isLoading = false
            ),
            onIntent = {},
            onBackClick = {}
        )
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val diff = Clock.System.now().toEpochMilliseconds() - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
