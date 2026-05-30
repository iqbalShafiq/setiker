package presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppTopBar
import presentation.components.AppTopBarBadgedActionIcon
import presentation.components.AiGenerateStickerPackBottomSheet
import presentation.components.EmptyState
import presentation.components.HomeBottomBar
import presentation.components.LoadingIndicator
import presentation.components.NeubrutalIconButton
import presentation.components.SortBottomSheet
import presentation.components.ProcessingPackListCard
import presentation.components.StickerPackListCard
import presentation.components.rememberImagePicker
import presentation.components.rememberVideoPicker
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel_search
import setiker.composeapp.generated.resources.home_hint
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title
import setiker.composeapp.generated.resources.search
import setiker.composeapp.generated.resources.search_packs_placeholder
import setiker.composeapp.generated.resources.home_ai_jobs_cd
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
    val generateInputImagePicker = rememberImagePicker { path ->
        path?.let { onIntent(HomeIntent.UpdateGeneratePackInputImage(it)) }
    }
    val videoPicker = rememberVideoPicker { path ->
        path?.let { onIntent(HomeIntent.StartVideoStickerPack(it)) }
    }

    LaunchedEffect(Unit) {
        onIntent(HomeIntent.LoadPacks)
    }

    var showSortSheet by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) searchFocusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_stickers_title),
                titleContent = {
                    AnimatedContent(
                        targetState = isSearchExpanded,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(180)) togetherWith
                                fadeOut(animationSpec = tween(120))).using(
                                SizeTransform(clip = false)
                            )
                        },
                        label = "home_topbar_title_transition"
                    ) { expanded ->
                        if (expanded) {
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { onIntent(HomeIntent.SearchQueryChanged(it)) },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester),
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(Res.string.search_packs_placeholder),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = neubrutalMutedOnSurface()
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        } else {
                            Text(
                                text = stringResource(Res.string.my_stickers_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            )
                        }
                    }
                },
                actions = {
                    NeubrutalIconButton(
                        icon = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(
                            if (isSearchExpanded) Res.string.cancel_search else Res.string.search
                        ),
                        onClick = {
                            if (isSearchExpanded) {
                                isSearchExpanded = false
                                onIntent(HomeIntent.SearchQueryChanged(""))
                            } else {
                                isSearchExpanded = true
                            }
                        }
                    )
                    NeubrutalIconButton(
                        icon = Icons.Default.Sort,
                        contentDescription = stringResource(Res.string.sort_content_description),
                        onClick = { showSortSheet = true }
                    )
                    AppTopBarBadgedActionIcon(
                        icon = Icons.Default.Workspaces,
                        contentDescription = stringResource(Res.string.home_ai_jobs_cd),
                        badgeCount = state.aiJobsBadgeCount,
                        onClick = { onIntent(HomeIntent.NavigateToAiJobs) }
                    )
                }
            )
        },
        bottomBar = {
            HomeBottomBar(
                currentUser = state.currentUser,
                pendingSyncCount = state.pendingSyncCount,
                isSyncing = state.isSyncing,
                onExploreClick = { onIntent(HomeIntent.NavigateToExplore) },
                onProfileClick = {
                    if (state.currentUser != null) {
                        onIntent(HomeIntent.NavigateToProfile)
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onSyncClick = {
                    if (state.currentUser != null) {
                        if (state.activeAiJobCount > 0) {
                            onIntent(HomeIntent.NavigateToAiJobs)
                        } else {
                            onIntent(HomeIntent.NavigateToSync)
                        }
                    } else {
                        onIntent(HomeIntent.NavigateToLogin)
                    }
                },
                onGeneratePackClick = { onIntent(HomeIntent.OpenGeneratePackSheet) },
                onVideoPackClick = { videoPicker.launch() },
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
            !state.hasListContent -> {
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
                    Text(
                        text = stringResource(Res.string.home_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )

                    val packsToShow = state.filteredPacks
                    val processingPacksToShow = state.filteredProcessingPacks

                    if (packsToShow.isEmpty() && processingPacksToShow.isEmpty() && state.searchQuery.isNotEmpty()) {
                        EmptyState(
                            title = stringResource(Res.string.no_search_results_title),
                            description = stringResource(Res.string.no_search_results_desc),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                top = 0.dp,
                                bottom = 20.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = processingPacksToShow,
                                key = { "processing_${it.draftId}" }
                            ) { pack ->
                                ProcessingPackListCard(
                                    pack = pack,
                                    modifier = Modifier.animateItem()
                                )
                            }
                            items(
                                items = packsToShow,
                                key = { it.identifier }
                            ) { pack ->
                                StickerPackListCard(
                                    pack = pack,
                                    onClick = { onPackClick(pack.identifier) },
                                    modifier = Modifier.animateItem()
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

        if (state.isGeneratePackSheetOpen) {
            AiGenerateStickerPackBottomSheet(
                packName = state.generatePackName,
                onPackNameChange = { onIntent(HomeIntent.UpdateGeneratePackName(it)) },
                publisher = state.generatePackPublisher,
                onPublisherChange = { onIntent(HomeIntent.UpdateGeneratePackPublisher(it)) },
                prompt = state.generatePackPrompt,
                onPromptChange = { onIntent(HomeIntent.UpdateGeneratePackPrompt(it)) },
                layout = state.generatePackLayout,
                onLayoutChange = { onIntent(HomeIntent.UpdateGeneratePackLayout(it)) },
                inputImagePath = state.generatePackInputImagePath,
                onPickInputImage = { generateInputImagePicker.launch() },
                onClearInputImage = { onIntent(HomeIntent.UpdateGeneratePackInputImage(null)) },
                isGenerating = state.isGeneratePackLoading,
                onGenerate = { onIntent(HomeIntent.GenerateStickerPack) },
                onDismiss = { onIntent(HomeIntent.CloseGeneratePackSheet) }
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
