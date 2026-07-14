package presentation.sync

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
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
import presentation.common.ContentStateAnimations
import presentation.common.ListLoadPhase
import presentation.components.AppTopBar
import presentation.components.AppIllustration
import presentation.components.EmptyState
import presentation.components.InteractionBlockedBox
import presentation.components.LoadingIndicator
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.retry
import setiker.composeapp.generated.resources.sync_already_running
import setiker.composeapp.generated.resources.sync_another_in_progress
import setiker.composeapp.generated.resources.sync_clear_done
import setiker.composeapp.generated.resources.sync_detail_download_issues
import setiker.composeapp.generated.resources.sync_detail_downloaded_packs
import setiker.composeapp.generated.resources.sync_detail_downloaded_stickers
import setiker.composeapp.generated.resources.sync_detail_removed
import setiker.composeapp.generated.resources.sync_detail_uploaded
import setiker.composeapp.generated.resources.sync_empty_desc
import setiker.composeapp.generated.resources.sync_empty_title
import setiker.composeapp.generated.resources.sync_last_sync
import setiker.composeapp.generated.resources.sync_login_to_sync
import setiker.composeapp.generated.resources.sync_now
import setiker.composeapp.generated.resources.sync_operation_status
import setiker.composeapp.generated.resources.sync_operation_target
import setiker.composeapp.generated.resources.sync_operations_count
import setiker.composeapp.generated.resources.sync_pending_count
import setiker.composeapp.generated.resources.sync_ready
import setiker.composeapp.generated.resources.sync_remove
import setiker.composeapp.generated.resources.sync_resume_when_online
import setiker.composeapp.generated.resources.sync_sign_in_required
import setiker.composeapp.generated.resources.sync_stage_pulling_remote
import setiker.composeapp.generated.resources.sync_stage_pushing_local
import setiker.composeapp.generated.resources.sync_synced_with_warnings
import setiker.composeapp.generated.resources.sync_syncing
import setiker.composeapp.generated.resources.sync_timestamp_days_ago
import setiker.composeapp.generated.resources.sync_timestamp_hours_ago
import setiker.composeapp.generated.resources.sync_timestamp_just_now
import setiker.composeapp.generated.resources.sync_timestamp_minutes_ago
import setiker.composeapp.generated.resources.sync_title
import setiker.composeapp.generated.resources.sync_up_to_date
import setiker.composeapp.generated.resources.sync_waiting_internet
import setiker.composeapp.generated.resources.syncing

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
                title = stringResource(Res.string.sync_title)
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (state.isSyncing) stringResource(Res.string.syncing) else null,
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                        onClick = onBackClick,
                        enabled = !state.isSyncing
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.DoneAll,
                        contentDescription = stringResource(Res.string.sync_clear_done),
                        onClick = { onIntent(SyncIntent.ClearCompleted) },
                        enabled = !state.isSyncing && state.operations.any { it.status == SyncOperationStatus.SUCCESS }
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.sync_now),
                        onClick = { onIntent(SyncIntent.SyncNow) },
                        enabled = !state.isSyncing,
                        isLoading = state.isSyncing
                    )
                }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        InteractionBlockedBox(
            blocked = state.isSyncing,
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
            SyncStatusCard(
                isSyncing = state.isSyncing,
                syncStage = state.syncStage,
                lastReport = state.lastReport,
                pendingCount = state.operations.count { it.status == SyncOperationStatus.PENDING }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(Res.string.sync_operations_count, state.operations.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = neubrutalOnSurface()
            )

            Spacer(modifier = Modifier.height(8.dp))

            val opsPhase = when {
                state.isLoading -> ListLoadPhase.Loading
                state.operations.isEmpty() -> ListLoadPhase.Empty
                else -> ListLoadPhase.Content
            }
            AnimatedContent(
                targetState = opsPhase,
                transitionSpec = { ContentStateAnimations.fadeOnly() },
                label = "sync_ops_phase",
                modifier = Modifier.fillMaxSize()
            ) { current ->
                when (current) {
                    ListLoadPhase.Loading -> LoadingIndicator(
                        modifier = Modifier.fillMaxSize(),
                        illustration = AppIllustration.LoadingState
                    )
                    ListLoadPhase.Empty -> EmptyState(
                        title = stringResource(Res.string.sync_empty_title),
                        description = stringResource(Res.string.sync_empty_desc),
                        illustration = AppIllustration.SuccessSync,
                        modifier = Modifier.fillMaxSize()
                    )
                    ListLoadPhase.Error -> Unit
                    ListLoadPhase.Content -> LazyColumn(
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
                            text = stringResource(
                                Res.string.sync_last_sync,
                                formatTimestamp(it.timestamp)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun syncTitle(
    isSyncing: Boolean,
    syncStage: SyncStage,
    pendingCount: Int,
    lastReport: SyncReport?,
): String {
    if (isSyncing) {
        return when (syncStage) {
            SyncStage.PUSHING_LOCAL -> stringResource(Res.string.sync_stage_pushing_local)
            SyncStage.PULLING_REMOTE -> stringResource(Res.string.sync_stage_pulling_remote)
            SyncStage.WAITING_FOR_INTERNET -> stringResource(Res.string.sync_waiting_internet)
            SyncStage.SIGN_IN_REQUIRED -> stringResource(Res.string.sync_sign_in_required)
            SyncStage.IDLE -> stringResource(Res.string.sync_syncing)
        }
    }
    return when {
        lastReport?.result is SyncResult.SkippedOffline -> stringResource(Res.string.sync_waiting_internet)
        lastReport?.result is SyncResult.SkippedNotAuthenticated -> stringResource(Res.string.sync_sign_in_required)
        lastReport?.result is SyncResult.SkippedInProgress -> stringResource(Res.string.sync_already_running)
        lastReport?.remoteDownloadFailures?.let { it > 0 } == true -> stringResource(Res.string.sync_synced_with_warnings)
        pendingCount > 0 -> stringResource(Res.string.sync_pending_count, pendingCount)
        else -> stringResource(Res.string.sync_up_to_date)
    }
}

@Composable
private fun syncDetail(lastReport: SyncReport?): String {
    val report = lastReport ?: return stringResource(Res.string.sync_ready)
    return when (val result = report.result) {
        is SyncResult.Failed -> result.error
        SyncResult.SkippedOffline -> stringResource(Res.string.sync_resume_when_online)
        SyncResult.SkippedNotAuthenticated -> stringResource(Res.string.sync_login_to_sync)
        SyncResult.SkippedInProgress -> stringResource(Res.string.sync_another_in_progress)
        SyncResult.Success -> buildString {
            append(stringResource(Res.string.sync_detail_uploaded, report.operationsSucceeded, report.operationsProcessed))
            append(stringResource(Res.string.sync_detail_downloaded_packs, report.remotePacksDownloaded))
            append(stringResource(Res.string.sync_detail_downloaded_stickers, report.remoteStickersDownloaded))
            if (report.remoteItemsDeleted > 0) {
                append(stringResource(Res.string.sync_detail_removed, report.remoteItemsDeleted))
            }
            if (report.remoteDownloadFailures > 0) {
                append(stringResource(Res.string.sync_detail_download_issues, report.remoteDownloadFailures))
            }
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
                        text = stringResource(
                            Res.string.sync_operation_target,
                            operation.type.name,
                            operation.targetId
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = neubrutalOnSurface()
                    )
                    Text(
                        text = stringResource(Res.string.sync_operation_status, operation.status.name),
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
                                Text(stringResource(Res.string.retry), color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton(onClick = onCancel) {
                                Text(stringResource(Res.string.sync_remove), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    SyncOperationStatus.PENDING -> {
                        TextButton(onClick = onCancel) {
                            Text(stringResource(Res.string.cancel), color = MaterialTheme.colorScheme.error)
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

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val diff = Clock.System.now().toEpochMilliseconds() - timestamp
    return when {
        diff < 60_000 -> stringResource(Res.string.sync_timestamp_just_now)
        diff < 3600_000 -> stringResource(Res.string.sync_timestamp_minutes_ago, diff / 60_000)
        diff < 86400_000 -> stringResource(Res.string.sync_timestamp_hours_ago, diff / 3600_000)
        else -> stringResource(Res.string.sync_timestamp_days_ago, diff / 86400_000)
    }
}
