package presentation.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import data.remote.ExploreFeed
import data.remote.model.CloudStickerPack
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.ExploreSortBottomSheet
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSearchBar
import presentation.components.NeubrutalSelectableChip
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_load_explore_failed
import setiker.composeapp.generated.resources.explore_back
import setiker.composeapp.generated.resources.explore_creator_unknown
import setiker.composeapp.generated.resources.explore_empty_following_desc
import setiker.composeapp.generated.resources.explore_empty_following_title
import setiker.composeapp.generated.resources.explore_empty_saved_desc
import setiker.composeapp.generated.resources.explore_empty_saved_title
import setiker.composeapp.generated.resources.explore_empty_shared_desc
import setiker.composeapp.generated.resources.explore_empty_shared_title
import setiker.composeapp.generated.resources.explore_featured_badge
import setiker.composeapp.generated.resources.explore_feed_discover
import setiker.composeapp.generated.resources.explore_feed_following
import setiker.composeapp.generated.resources.explore_feed_saved
import setiker.composeapp.generated.resources.explore_feed_shared
import setiker.composeapp.generated.resources.explore_history
import setiker.composeapp.generated.resources.explore_sign_in_action
import setiker.composeapp.generated.resources.explore_sign_in_desc
import setiker.composeapp.generated.resources.explore_sign_in_title
import setiker.composeapp.generated.resources.explore_sort
import setiker.composeapp.generated.resources.explore_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.retry
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
    val contentHorizontalPadding = 20.dp

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
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(ExploreIntent.Refresh) },
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item("search") {
                    Box(modifier = Modifier.padding(horizontal = contentHorizontalPadding)) {
                        NeubrutalSearchBar(
                            query = state.searchQuery,
                            onQueryChange = { onIntent(ExploreIntent.SearchChanged(it)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                item("feed_tabs") {
                    val feedTabs = listOf(
                        ExploreFeed.DISCOVER to Res.string.explore_feed_discover,
                        ExploreFeed.SAVED to Res.string.explore_feed_saved,
                        ExploreFeed.FOLLOWING to Res.string.explore_feed_following,
                        ExploreFeed.SHARED_WITH_ME to Res.string.explore_feed_shared
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Spacer(modifier = Modifier.width(contentHorizontalPadding))
                        feedTabs.forEachIndexed { index, (feed, labelRes) ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            NeubrutalSelectableChip(
                                label = stringResource(labelRes),
                                selected = state.feed == feed,
                                onClick = { onIntent(ExploreIntent.ChangeFeed(feed)) }
                            )
                        }
                        Spacer(modifier = Modifier.width(contentHorizontalPadding))
                    }
                }
                if (state.feed == ExploreFeed.DISCOVER && state.featuredPack != null) {
                    item("featured") {
                        PublicPackCard(
                            pack = state.featuredPack,
                            badge = stringResource(Res.string.explore_featured_badge),
                            showSocialActions = state.isAuthenticated,
                            onClick = { onIntent(ExploreIntent.OpenPack(state.featuredPack.id)) },
                            onCreatorClick = state.featuredPack.owner?.id?.let { ownerId ->
                                { onIntent(ExploreIntent.OpenCreator(ownerId)) }
                            },
                            onToggleLike = { onIntent(ExploreIntent.ToggleLike(state.featuredPack.id)) },
                            onToggleSave = { onIntent(ExploreIntent.ToggleSave(state.featuredPack.id)) },
                            modifier = Modifier.padding(horizontal = contentHorizontalPadding)
                        )
                    }
                }
                when {
                    state.requiresLogin -> {
                        item("sign_in") {
                            EmptyState(
                                title = stringResource(Res.string.explore_sign_in_title),
                                description = stringResource(Res.string.explore_sign_in_desc),
                                illustration = AppIllustration.SearchEmpty,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentHorizontalPadding),
                                action = {
                                    AppPrimaryButton(
                                        text = stringResource(Res.string.explore_sign_in_action),
                                        onClick = { onIntent(ExploreIntent.NavigateLogin) }
                                    )
                                }
                            )
                        }
                    }
                    state.isLoading && state.packs.isEmpty() -> {
                        item("loading") {
                            LoadingIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentHorizontalPadding)
                                    .height(240.dp),
                                illustration = AppIllustration.LoadingState
                            )
                        }
                    }
                    state.loadFailed -> {
                        item("error") {
                            EmptyState(
                                title = stringResource(Res.string.error_load_explore_failed),
                                description = stringResource(Res.string.no_search_results_desc),
                                illustration = AppIllustration.ErrorState,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentHorizontalPadding),
                                action = {
                                    AppPrimaryButton(
                                        text = stringResource(Res.string.retry),
                                        onClick = { onIntent(ExploreIntent.Refresh) }
                                    )
                                }
                            )
                        }
                    }
                    state.packs.isEmpty() -> {
                        item("empty") {
                            val (title, desc) = emptyStateForFeed(state.feed)
                            EmptyState(
                                title = title,
                                description = desc,
                                illustration = AppIllustration.SearchEmpty,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = contentHorizontalPadding)
                            )
                        }
                    }
                    else -> {
                        items(state.packs, key = { it.id }) { pack ->
                            PublicPackCard(
                                pack = pack,
                                showSocialActions = state.isAuthenticated,
                                onClick = { onIntent(ExploreIntent.OpenPack(pack.id)) },
                                onCreatorClick = pack.owner?.id?.let { ownerId ->
                                    { onIntent(ExploreIntent.OpenCreator(ownerId)) }
                                },
                                onToggleLike = { onIntent(ExploreIntent.ToggleLike(pack.id)) },
                                onToggleSave = { onIntent(ExploreIntent.ToggleSave(pack.id)) },
                                modifier = Modifier.padding(horizontal = contentHorizontalPadding)
                            )
                        }
                        if (state.isLoadingMore) {
                            item("pagination_loader") {
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
                        } else if (state.canLoadMore) {
                            item("pagination_trigger") {
                                LaunchedEffect(state.page, state.sort, state.feed) {
                                    onIntent(ExploreIntent.LoadMore)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ExploreSortBottomSheet(
                currentSort = state.sort,
                onSortSelected = { onIntent(ExploreIntent.ChangeSort(it)) },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}

@Composable
private fun emptyStateForFeed(feed: ExploreFeed): Pair<String, String> = when (feed) {
    ExploreFeed.SAVED -> stringResource(Res.string.explore_empty_saved_title) to
        stringResource(Res.string.explore_empty_saved_desc)
    ExploreFeed.FOLLOWING -> stringResource(Res.string.explore_empty_following_title) to
        stringResource(Res.string.explore_empty_following_desc)
    ExploreFeed.SHARED_WITH_ME -> stringResource(Res.string.explore_empty_shared_title) to
        stringResource(Res.string.explore_empty_shared_desc)
    ExploreFeed.DISCOVER -> stringResource(Res.string.no_search_results_title) to
        stringResource(Res.string.no_search_results_desc)
}

@Composable
private fun PublicPackCard(
    pack: CloudStickerPack,
    onClick: () -> Unit,
    badge: String? = null,
    showSocialActions: Boolean = false,
    onCreatorClick: (() -> Unit)? = null,
    onToggleLike: () -> Unit = {},
    onToggleSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val firstSticker = pack.stickers.firstOrNull()?.sticker?.url
    val creator = pack.owner?.displayName ?: pack.owner?.username ?: stringResource(Res.string.explore_creator_unknown)
    val liked = pack.isLiked ?: pack.liked ?: false
    val saved = pack.isSaved ?: pack.saved ?: false
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
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
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
                .neubrutalBorderWithGloss(
                    color = border,
                    cornerRadius = 12.dp,
                    highlightColor = neubrutalGlossyHighlightColor()
                ),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            if (badge != null) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = AccentCoral
                )
            }
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface()
            )
            Text(
                text = creator,
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface(),
                modifier = if (onCreatorClick != null) Modifier.clickable(onClick = onCreatorClick) else Modifier
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(Res.string.social_counts, pack.likeCount, pack.saveCount, pack.downloadCount),
                style = MaterialTheme.typography.labelSmall,
                color = neubrutalMutedOnSurface()
            )
        }
        if (showSocialActions) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onToggleLike, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (liked) AccentCoral else neubrutalMutedOnSurface()
                    )
                }
                IconButton(onClick = onToggleSave, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (saved) AccentCoral else neubrutalMutedOnSurface()
                    )
                }
            }
        }
    }
}
