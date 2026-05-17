package presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.AppTopBar
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
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
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.deleting
import setiker.composeapp.generated.resources.history_clear_all
import setiker.composeapp.generated.resources.history_filter_all
import setiker.composeapp.generated.resources.history_filter_background
import setiker.composeapp.generated.resources.history_filter_generate
import setiker.composeapp.generated.resources.history_filter_grid
import setiker.composeapp.generated.resources.history_none_desc
import setiker.composeapp.generated.resources.history_none_title
import setiker.composeapp.generated.resources.history_outputs
import setiker.composeapp.generated.resources.history_refresh
import setiker.composeapp.generated.resources.history_subtitle
import setiker.composeapp.generated.resources.history_title

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
        when {
            state.isLoading -> LoadingIndicator(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            state.items.isEmpty() -> EmptyState(
                title = stringResource(Res.string.history_none_title),
                description = state.error ?: stringResource(Res.string.history_none_desc),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
            else -> {
                Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null, "generate", "grid-split", "background-remove").forEach { filter ->
                            FilterChip(
                                selected = state.typeFilter == filter,
                                onClick = { onIntent(ProcessingHistoryIntent.ChangeFilter(filter)) },
                                label = {
                                    Text(
                                        when (filter) {
                                            null -> stringResource(Res.string.history_filter_all)
                                            "generate" -> stringResource(Res.string.history_filter_generate)
                                            "grid-split" -> stringResource(Res.string.history_filter_grid)
                                            else -> stringResource(Res.string.history_filter_background)
                                        }
                                    )
                                }
                            )
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp),
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
                                    .border(
                                        width = NeubrutalBorderWidth,
                                        color = neubrutalBorderColor(),
                                        shape = RoundedCornerShape(NeubrutalCardRadius)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
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
