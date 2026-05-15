package presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.HomeBottomBar
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalSearchBar
import presentation.components.SortBottomSheet
import presentation.components.StickerPackListCard
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.home_hint
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title
import setiker.composeapp.generated.resources.sort_content_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onPackClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        onIntent(HomeIntent.LoadPacks)
    }

    var showSortSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_stickers_title),
                actions = {
                    IconButton(onClick = { showSortSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = stringResource(Res.string.sort_content_description),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        },
        bottomBar = {
            HomeBottomBar(
                currentUser = state.currentUser,
                pendingSyncCount = state.pendingSyncCount,
                isSyncing = state.isSyncing,
                onProfileClick = {
                    if (state.currentUser != null) {
                        onIntent(HomeIntent.NavigateToProfile)
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onSyncClick = {
                    if (state.currentUser != null) {
                        onIntent(HomeIntent.NavigateToSync)
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onAddPackClick = { onIntent(HomeIntent.CreateNewPack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        when {
            state.isLoading -> {
                LoadingIndicator(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            state.packs.isEmpty() -> {
                EmptyState(
                    title = stringResource(Res.string.no_stickers_yet_title),
                    description = stringResource(Res.string.no_stickers_yet_desc),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                androidx.compose.foundation.layout.Column(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                ) {
                    NeubrutalSearchBar(
                        query = state.searchQuery,
                        onQueryChange = { onIntent(HomeIntent.SearchQueryChanged(it)) },
                        modifier = Modifier.padding(vertical = 10.dp)
                    )

                    Text(
                        text = stringResource(Res.string.home_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    val packsToShow = state.filteredPacks

                    if (packsToShow.isEmpty() && state.searchQuery.isNotEmpty()) {
                        EmptyState(
                            title = stringResource(Res.string.no_search_results_title),
                            description = stringResource(Res.string.no_search_results_desc),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 12.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = packsToShow,
                                key = { it.identifier }
                            ) { pack ->
                                StickerPackListCard(
                                    pack = pack,
                                    onClick = { onPackClick(pack.identifier) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showSortSheet) {
            SortBottomSheet(
                currentSort = state.sortOrder,
                onSortSelected = { onIntent(HomeIntent.SortOrderChanged(it)) },
                onDismiss = { showSortSheet = false }
            )
        }
    }
}

// MARK: - Previews

private val mockPacks = listOf(
    StickerPack(
        identifier = "pack_1",
        name = "Funny Cats",
        publisher = "CatLover",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""), Sticker(imageFile = ""), Sticker(imageFile = ""))
    ),
    StickerPack(
        identifier = "pack_2",
        name = "Dogs",
        publisher = "DogLover",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""))
    ),
    StickerPack(
        identifier = "pack_3",
        name = "Reactions",
        publisher = "MemeMaster",
        trayImageFile = "",
        stickers = listOf(Sticker(imageFile = ""), Sticker(imageFile = ""))
    )
)

@Preview
@Composable
private fun HomeScreenLoadingPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(isLoading = true),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenEmptyPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenWithPacksPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(packs = mockPacks),
            onIntent = {},
            onPackClick = {}
        )
    }
}

@Preview
@Composable
private fun HomeScreenSearchNoResultsPreview() {
    MaterialTheme {
        HomeScreen(
            state = HomeState(
                packs = mockPacks,
                searchQuery = "xyz"
            ),
            onIntent = {},
            onPackClick = {}
        )
    }
}
