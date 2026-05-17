package presentation.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import data.remote.ExploreSort
import data.remote.model.CloudStickerPack
import org.jetbrains.compose.resources.stringResource
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.AppTopBar
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.explore_back
import setiker.composeapp.generated.resources.explore_creator_unknown
import setiker.composeapp.generated.resources.explore_history
import setiker.composeapp.generated.resources.explore_sort
import setiker.composeapp.generated.resources.explore_sort_title
import setiker.composeapp.generated.resources.explore_subtitle
import setiker.composeapp.generated.resources.explore_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.social_counts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    state: ExploreState,
    onIntent: (ExploreIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(ExploreIntent.LoadInitial)
    }

    var showSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.explore_title),
                onBackClick = null
            )
        },
        bottomBar = {
            PackBottomBar(
                actions = {
                    PackBottomBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.explore_back),
                        onClick = { onIntent(ExploreIntent.NavigateBack) }
                    )
                    PackBottomBarIconButton(
                        icon = Icons.Default.History,
                        contentDescription = stringResource(Res.string.explore_history),
                        onClick = { onIntent(ExploreIntent.NavigateHistory) }
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = Icons.Default.Sort,
                        contentDescription = stringResource(Res.string.explore_sort),
                        onClick = { showSortSheet = true }
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
            state.filteredPacks.isEmpty() -> {
                EmptyState(
                    title = stringResource(Res.string.no_search_results_title),
                    description = state.error ?: stringResource(Res.string.no_search_results_desc),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.filteredPacks, key = { it.id }) { pack ->
                        PublicPackCard(
                            pack = pack,
                            onClick = { onIntent(ExploreIntent.OpenPack(pack.id)) }
                        )
                    }
                    item("pagination_loader") {
                        if (state.canLoadMore) {
                            LaunchedEffect(state.page, state.sort) {
                                onIntent(ExploreIntent.LoadMore)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentCoral
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = neubrutalScreenBackground()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.explore_sort_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    ExploreSort.entries.forEach { option ->
                        val selected = option == state.sort
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (selected) neubrutalCardSurface() else neubrutalScreenBackground())
                                .border(NeubrutalBorderWidth, neubrutalBorderColor(), RoundedCornerShape(12.dp))
                                .clickable {
                                    onIntent(ExploreIntent.ChangeSort(option))
                                    showSortSheet = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = option.value.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                color = neubrutalOnSurface()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicPackCard(
    pack: CloudStickerPack,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstSticker = pack.stickers.firstOrNull()?.sticker?.url
    val creator = pack.owner?.displayName ?: pack.owner?.username ?: stringResource(Res.string.explore_creator_unknown)
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .clip(RoundedCornerShape(NeubrutalCardRadius))
            .background(neubrutalCardSurface())
            .border(NeubrutalBorderWidth, border, RoundedCornerShape(NeubrutalCardRadius))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = firstSticker,
            contentDescription = pack.name,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(neubrutalScreenBackground())
                .border(NeubrutalBorderWidth, border, RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Text(
                text = creator,
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.social_counts, pack.likeCount, pack.saveCount, pack.downloadCount),
                style = MaterialTheme.typography.labelSmall,
                color = neubrutalMutedOnSurface()
            )
        }
    }
}
