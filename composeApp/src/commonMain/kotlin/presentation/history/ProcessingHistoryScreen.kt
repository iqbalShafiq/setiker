package presentation.history

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import data.remote.resolveApiUrl
import presentation.common.ContentStateAnimations
import presentation.common.ListLoadPhase
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSelectableChip
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.ReportContentBottomSheet
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.NeubrutalCardRadius
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.deleting
import setiker.composeapp.generated.resources.history_clear_all
import setiker.composeapp.generated.resources.history_filter_all
import setiker.composeapp.generated.resources.history_filter_background
import setiker.composeapp.generated.resources.history_filter_generate
import setiker.composeapp.generated.resources.history_filter_grid
import setiker.composeapp.generated.resources.history_filter_improve
import setiker.composeapp.generated.resources.history_filter_pack
import setiker.composeapp.generated.resources.history_filter_video
import setiker.composeapp.generated.resources.history_none_desc
import setiker.composeapp.generated.resources.history_none_title
import setiker.composeapp.generated.resources.history_outputs
import setiker.composeapp.generated.resources.history_report_output
import setiker.composeapp.generated.resources.history_refresh
import setiker.composeapp.generated.resources.history_offline_banner
import setiker.composeapp.generated.resources.history_title
import setiker.composeapp.generated.resources.retry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessingHistoryScreen(
    state: ProcessingHistoryState,
    onIntent: (ProcessingHistoryIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.history_title),
                onBackClick = null
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (state.isClearing) stringResource(Res.string.deleting) else null,
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { onIntent(ProcessingHistoryIntent.NavigateBack) },
                        enabled = !state.isClearing
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.Refresh,
                        contentDescription = stringResource(Res.string.history_refresh),
                        onClick = { onIntent(ProcessingHistoryIntent.Load) },
                        enabled = !state.isClearing
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.history_clear_all),
                        onClick = { onIntent(ProcessingHistoryIntent.ClearAll) },
                        enabled = !state.isClearing && state.items.isNotEmpty(),
                        isLoading = state.isClearing
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        InteractionBlockedBox(
            blocked = state.isClearing,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp)
            ) {
                if (state.isShowingCachedData) {
                    Text(
                        text = stringResource(Res.string.history_offline_banner),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        null,
                        "generate",
                        "grid-split",
                        "background-remove",
                        "improve",
                        "video-sticker-pack",
                        "sticker-pack"
                    ).forEach { filter ->
                        NeubrutalSelectableChip(
                            label = when (filter) {
                                null -> stringResource(Res.string.history_filter_all)
                                "generate" -> stringResource(Res.string.history_filter_generate)
                                "grid-split" -> stringResource(Res.string.history_filter_grid)
                                "background-remove" -> stringResource(Res.string.history_filter_background)
                                "improve" -> stringResource(Res.string.history_filter_improve)
                                "video-sticker-pack" -> stringResource(Res.string.history_filter_video)
                                "sticker-pack" -> stringResource(Res.string.history_filter_pack)
                                else -> filter
                            },
                            selected = state.typeFilter == filter,
                            onClick = { onIntent(ProcessingHistoryIntent.ChangeFilter(filter)) }
                        )
                    }
                }

                val resultsPhase = when {
                    state.isLoading -> ListLoadPhase.Loading
                    state.items.isEmpty() && state.error != null -> ListLoadPhase.Error
                    state.items.isEmpty() -> ListLoadPhase.Empty
                    else -> ListLoadPhase.Content
                }
                AnimatedContent(
                    targetState = resultsPhase,
                    transitionSpec = { with(ContentStateAnimations) { listLoad() } },
                    label = "history_results_phase",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 12.dp)
                ) { current ->
                    when (current) {
                        ListLoadPhase.Loading -> LoadingIndicator(
                            modifier = Modifier.fillMaxSize(),
                            illustration = AppIllustration.LoadingState
                        )
                        ListLoadPhase.Error -> EmptyState(
                            title = stringResource(Res.string.history_none_title),
                            description = state.error ?: stringResource(Res.string.history_none_desc),
                            illustration = AppIllustration.ErrorState,
                            modifier = Modifier.fillMaxSize(),
                            action = {
                                AppPrimaryButton(
                                    text = stringResource(Res.string.retry),
                                    onClick = { onIntent(ProcessingHistoryIntent.Load) }
                                )
                            }
                        )
                        ListLoadPhase.Empty -> EmptyState(
                            title = stringResource(Res.string.history_none_title),
                            description = stringResource(Res.string.history_none_desc),
                            illustration = AppIllustration.SuccessSync,
                            modifier = Modifier.fillMaxSize()
                        )
                        ListLoadPhase.Content -> LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .neubrutalShadow(
                                            offsetX = NeubrutalSmallShadowOffset,
                                            offsetY = NeubrutalSmallShadowOffset,
                                            cornerRadius = NeubrutalCardRadius,
                                            color = neubrutalShadowColor()
                                        )
                                        .background(neubrutalCardSurface(), RoundedCornerShape(NeubrutalCardRadius))
                                        .neubrutalBorderWithGloss(
                                            color = neubrutalBorderColor(),
                                            cornerRadius = NeubrutalCardRadius,
                                            highlightColor = neubrutalGlossyHighlightColor()
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val previewUrl = resolveApiUrl(item.outputFiles.firstOrNull()?.url)
                                    if (!previewUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = previewUrl,
                                            contentDescription = item.type,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(start = if (previewUrl != null) 10.dp else 0.dp)) {
                                        Text(
                                            text = item.type,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = neubrutalOnSurface()
                                        )
                                        Text(
                                            text = stringResource(Res.string.history_outputs, item.outputFiles.size),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = neubrutalMutedOnSurface()
                                        )
                                    }
                                    PackBottomBarIconButton(
                                        icon = Icons.Outlined.Flag,
                                        contentDescription = stringResource(Res.string.history_report_output),
                                        onClick = { onIntent(ProcessingHistoryIntent.ShowReport(item.id)) }
                                    )
                                    PackBottomBarIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        onClick = { onIntent(ProcessingHistoryIntent.DeleteItem(item.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    ReportContentBottomSheet(
        visible = state.showReportSheet,
        selectedReason = state.reportReason,
        details = state.reportDetails,
        isSubmitting = state.isSubmittingReport,
        onReasonSelected = { onIntent(ProcessingHistoryIntent.SelectReportReason(it)) },
        onDetailsChange = { onIntent(ProcessingHistoryIntent.UpdateReportDetails(it)) },
        onSubmit = { onIntent(ProcessingHistoryIntent.SubmitReport) },
        onDismiss = { onIntent(ProcessingHistoryIntent.DismissReport) }
    )
}
