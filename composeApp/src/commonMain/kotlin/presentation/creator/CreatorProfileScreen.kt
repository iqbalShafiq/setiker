package presentation.creator

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import data.remote.resolveApiUrl
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.ExploreSortBottomSheet
import presentation.components.FollowPackBottomBarFab
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSelectableChip
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarIconButton
import presentation.components.ScreenContentHorizontalScrollRow
import presentation.components.ScreenSectionTitle
import presentation.components.UnfollowConfirmDialog
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
import presentation.theme.screenContentHorizontalPadding
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.creator_follow
import setiker.composeapp.generated.resources.creator_following
import setiker.composeapp.generated.resources.creator_not_found_desc
import setiker.composeapp.generated.resources.creator_not_found_title
import setiker.composeapp.generated.resources.creator_public_packs
import setiker.composeapp.generated.resources.explore_sort
import setiker.composeapp.generated.resources.explore_sort_downloads
import setiker.composeapp.generated.resources.explore_sort_likes
import setiker.composeapp.generated.resources.explore_sort_popular
import setiker.composeapp.generated.resources.explore_sort_recent
import setiker.composeapp.generated.resources.explore_sort_saves
import setiker.composeapp.generated.resources.retry
import setiker.composeapp.generated.resources.social_counts
import setiker.composeapp.generated.resources.sort_by

@Composable
fun CreatorProfileScreen(
    userId: String,
    state: CreatorProfileState,
    onIntent: (CreatorProfileIntent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    var showSortSheet by remember { mutableStateOf(false) }
    var showUnfollowDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.profile?.displayName ?: state.profile?.username ?: "Creator",
                onBackClick = null
            )
        },
        bottomBar = {
            if (state.profile != null) {
                val profile = state.profile
                PackBottomBar(
                    actions = {
                        PackBottomBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = { onIntent(CreatorProfileIntent.NavigateBack) }
                        )
                        PackBottomBarIconButton(
                            icon = Icons.Default.Sort,
                            contentDescription = stringResource(Res.string.explore_sort),
                            onClick = { showSortSheet = true }
                        )
                    },
                    floatingActionButton = {
                        FollowPackBottomBarFab(
                            isFollowing = profile.isFollowing,
                            onClick = {
                                if (profile.isFollowing) {
                                    showUnfollowDialog = true
                                } else {
                                    onIntent(CreatorProfileIntent.ToggleFollow)
                                }
                            },
                            followContentDescription = stringResource(Res.string.creator_follow),
                            followingContentDescription = stringResource(Res.string.creator_following)
                        )
                    }
                )
            } else {
                PackBottomBar(
                    actions = {
                        PackBottomBarIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = { onIntent(CreatorProfileIntent.NavigateBack) }
                        )
                    },
                    floatingActionButton = null
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingIndicator(Modifier.fillMaxSize().padding(innerPadding))
            state.loadFailed || state.profile == null -> EmptyState(
                title = stringResource(Res.string.creator_not_found_title),
                description = stringResource(Res.string.creator_not_found_desc),
                illustration = AppIllustration.ErrorState,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                action = {
                    AppPrimaryButton(
                        text = stringResource(Res.string.retry),
                        onClick = { onIntent(CreatorProfileIntent.Load(userId)) }
                    )
                }
            )
            else -> {
                val profile = state.profile
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .screenContentHorizontalPadding(),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item("header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "@${profile.username}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = neubrutalOnSurface()
                            )
                            Text(
                                text = "${profile.followerCount} followers · ${profile.publicPackCount} packs",
                                style = MaterialTheme.typography.bodyMedium,
                                color = neubrutalMutedOnSurface()
                            )
                        }
                    }
                    item("sort_section_title") {
                        ScreenSectionTitle(
                            text = stringResource(Res.string.sort_by),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                    item("sort_tabs") {
                        ScreenContentHorizontalScrollRow { spacing ->
                            ExploreSort.entries.forEachIndexed { index, sort ->
                                if (index > 0) {
                                    Spacer(modifier = Modifier.width(spacing))
                                }
                                NeubrutalSelectableChip(
                                    label = sort.chipLabel(),
                                    selected = state.sort == sort,
                                    onClick = { onIntent(CreatorProfileIntent.ChangeSort(sort)) }
                                )
                            }
                        }
                    }
                    item("packs_section_title") {
                        ScreenSectionTitle(
                            text = stringResource(Res.string.creator_public_packs),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        )
                    }
                    items(state.packs, key = { it.id }) { pack ->
                        val thumb = resolveApiUrl(pack.stickers.firstOrNull()?.sticker?.url)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .neubrutalShadow(
                                    offsetX = NeubrutalShadowOffset,
                                    offsetY = NeubrutalShadowOffset,
                                    cornerRadius = NeubrutalCardRadius,
                                    color = neubrutalShadowColor()
                                )
                                .clip(RoundedCornerShape(NeubrutalCardRadius))
                                .background(neubrutalCardSurface())
                                .neubrutalBorderWithGloss(
                                    color = neubrutalBorderColor(),
                                    cornerRadius = NeubrutalCardRadius,
                                    highlightColor = neubrutalGlossyHighlightColor()
                                )
                                .clickable { onIntent(CreatorProfileIntent.OpenPack(pack.id)) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = pack.name,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(
                                    text = pack.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = neubrutalOnSurface()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        Res.string.social_counts,
                                        pack.likeCount,
                                        pack.saveCount,
                                        pack.downloadCount
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = neubrutalMutedOnSurface()
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            ExploreSortBottomSheet(
                currentSort = state.sort,
                onSortSelected = { onIntent(CreatorProfileIntent.ChangeSort(it)) },
                onDismiss = { showSortSheet = false }
            )
        }

        if (showUnfollowDialog && state.profile != null) {
            val profile = state.profile
            UnfollowConfirmDialog(
                displayName = profile.displayName ?: "@${profile.username}",
                onConfirm = {
                    showUnfollowDialog = false
                    onIntent(CreatorProfileIntent.ToggleFollow)
                },
                onDismiss = { showUnfollowDialog = false }
            )
        }
    }
}

@Composable
private fun ExploreSort.chipLabel(): String = when (this) {
    ExploreSort.RECENT -> stringResource(Res.string.explore_sort_recent)
    ExploreSort.POPULAR -> stringResource(Res.string.explore_sort_popular)
    ExploreSort.DOWNLOADS -> stringResource(Res.string.explore_sort_downloads)
    ExploreSort.LIKES -> stringResource(Res.string.explore_sort_likes)
    ExploreSort.SAVES -> stringResource(Res.string.explore_sort_saves)
}
