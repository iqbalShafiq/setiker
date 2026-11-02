package presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.resources.stringResource
import presentation.common.ContentStateAnimations
import presentation.common.ListLoadPhase
import presentation.components.AiGenerateStickerPackBottomSheet
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.AppSnackbarHostState
import presentation.components.AppTopBar
import presentation.components.AppTopBarBadgedActionIcon
import presentation.components.EmptyState
import presentation.components.HomeBottomBar
import presentation.components.LoadingDots
import presentation.components.NeubrutalIconButton
import presentation.components.NeubrutalSnackbarHost
import presentation.components.OfflineBanner
import presentation.components.ProcessingPackListCard
import presentation.components.PromptPresetPickerSheet
import presentation.components.SkeletonPackList
import presentation.components.SortBottomSheet
import presentation.components.StickerPackListCard
import presentation.components.rememberImagePicker
import presentation.components.rememberVideoPicker
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.screenContentHorizontalPadding
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel_search
import setiker.composeapp.generated.resources.error_load_packs_failed
import setiker.composeapp.generated.resources.home_ai_jobs_cd
import setiker.composeapp.generated.resources.home_hint
import setiker.composeapp.generated.resources.home_try_sample_pack
import setiker.composeapp.generated.resources.loading_pack
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.no_search_results_desc
import setiker.composeapp.generated.resources.no_search_results_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title
import setiker.composeapp.generated.resources.retry
import setiker.composeapp.generated.resources.search
import setiker.composeapp.generated.resources.search_packs_placeholder
import setiker.composeapp.generated.resources.sort_content_description

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    onIntent: (HomeIntent) -> Unit,
    onPackClick: (String) -> Unit,
    snackbarHostState: AppSnackbarHostState = remember { AppSnackbarHostState() },
    showOfflineBanner: Boolean = false,
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
    val focusManager = LocalFocusManager.current

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
                            ContentStateAnimations.contentTransform(
                                fadeIn(
                                    animationSpec = tween(
                                        durationMillis = SetikerMotion.DURATION_BASE_MS,
                                        easing = SetikerMotion.EaseOut
                                    )
                                ),
                                fadeOut(
                                    animationSpec = tween(
                                        durationMillis = SetikerMotion.DURATION_FAST_MS,
                                        easing = SetikerMotion.EaseOut
                                    )
                                )
                            )
                        },
                        label = "home_topbar_title_transition"
                    ) { expanded ->
                        if (expanded) {
                            val searchInteractionSource = remember { MutableInteractionSource() }
                            val isSearchFocused by searchInteractionSource.collectIsFocusedAsState()
                            // A focus ring that fades in: the inline search field used to give no
                            // sign of being focused beyond the blinking cursor.
                            val focusRing by animateColorAsState(
                                targetValue = if (isSearchFocused) AccentCoral else Color.Transparent,
                                animationSpec = SetikerMotion.standard(),
                                label = "home_search_focus_ring"
                            )
                            BasicTextField(
                                value = state.searchQuery,
                                onValueChange = { onIntent(HomeIntent.SearchQueryChanged(it)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = { focusManager.clearFocus() }
                                ),
                                interactionSource = searchInteractionSource,
                                textStyle = MaterialTheme.typography.titleMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(searchFocusRequester)
                                    .border(
                                        width = 2.dp,
                                        color = focusRing,
                                        shape = RoundedCornerShape(NeubrutalSmallRadius)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                decorationBox = { innerTextField ->
                                    if (state.searchQuery.isEmpty()) {
                                        Text(
                                            text = stringResource(Res.string.search_packs_placeholder),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = neubrutalMutedOnSurface(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            softWrap = false
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
                        icon = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = stringResource(Res.string.sort_content_description),
                        onClick = { showSortSheet = true }
                    )
                    AppTopBarBadgedActionIcon(
                        icon = Icons.Default.Workspaces,
                        contentDescription = stringResource(Res.string.home_ai_jobs_cd),
                        badgeCount = state.aiJobsBadgeCount,
                        onClick = { onIntent(HomeIntent.NavigateToAiJobsFromTopBar) }
                    )
                }
            )
        },
        bottomBar = {
            val authAction = homeAuthAction(state.authState)
            HomeBottomBar(
                currentUser = state.currentUser,
                pendingSyncCount = state.pendingSyncCount,
                isSyncing = state.isSyncing,
                accountActionsEnabled = authAction != HomeAuthAction.WAITING,
                onExploreClick = { onIntent(HomeIntent.NavigateToExplore) },
                onProfileClick = {
                    when (authAction) {
                        HomeAuthAction.SIGNED_IN -> onIntent(HomeIntent.NavigateToProfile)
                        HomeAuthAction.SIGN_IN -> onIntent(HomeIntent.NavigateToLogin)
                        // The session is still being resolved: acting now would send a signed-in
                        // user to the login screen.
                        HomeAuthAction.WAITING -> Unit
                    }
                },
                onSyncClick = {
                    when (authAction) {
                        HomeAuthAction.SIGNED_IN -> {
                            if (state.activeAiJobCount > 0) {
                                onIntent(HomeIntent.NavigateToAiJobs)
                            } else {
                                onIntent(HomeIntent.NavigateToSync)
                            }
                        }
                        HomeAuthAction.SIGN_IN -> onIntent(HomeIntent.NavigateToLogin)
                        HomeAuthAction.WAITING -> Unit
                    }
                },
                onGeneratePackClick = { onIntent(HomeIntent.OpenGeneratePackSheet) },
                onVideoPackClick = { videoPicker.launch() },
                onAddPackClick = { onIntent(HomeIntent.CreateNewPack) }
            )
        },
        snackbarHost = { NeubrutalSnackbarHost(snackbarHostState) },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        val phase = when {
            state.isLoading -> ListLoadPhase.Loading
            state.loadFailed -> ListLoadPhase.Error
            !state.hasListContent || state.hasSearchNoResults -> ListLoadPhase.Empty
            else -> ListLoadPhase.Content
        }

        AnimatedContent(
            targetState = phase,
            transitionSpec = { with(ContentStateAnimations) { listLoad() } },
            label = "home_content_phase",
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { currentPhase ->
            when (currentPhase) {
                ListLoadPhase.Loading -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .screenContentHorizontalPadding()
                        .padding(top = 12.dp)
                ) {
                    LoadingDots(
                        dotSize = 8.dp,
                        contentDescription = stringResource(Res.string.loading_pack),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 16.dp)
                    )
                    SkeletonPackList()
                }

                ListLoadPhase.Error -> EmptyState(
                    title = stringResource(Res.string.error_load_packs_failed),
                    description = stringResource(Res.string.no_stickers_yet_desc),
                    illustration = AppIllustration.ErrorState,
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        AppPrimaryButton(
                            text = stringResource(Res.string.retry),
                            onClick = { onIntent(HomeIntent.RetryLoadPacks) }
                        )
                    }
                )

                ListLoadPhase.Empty -> if (state.hasSearchNoResults) {
                    EmptyState(
                        title = stringResource(Res.string.no_search_results_title),
                        description = stringResource(Res.string.no_search_results_desc),
                        illustration = AppIllustration.SearchEmpty,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyState(
                        title = stringResource(Res.string.no_stickers_yet_title),
                        description = stringResource(Res.string.no_stickers_yet_desc),
                        illustration = AppIllustration.EmptyPack,
                        modifier = Modifier.fillMaxSize(),
                        action = {
                            AppPrimaryButton(
                                text = stringResource(Res.string.home_try_sample_pack),
                                onClick = { onIntent(HomeIntent.CreateNewPack) }
                            )
                        }
                    )
                }

                ListLoadPhase.Content -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .screenContentHorizontalPadding()
                        .padding(top = 12.dp)
                ) {
                    AnimatedVisibility(
                        visible = showOfflineBanner,
                        enter = fadeIn(
                            animationSpec = tween(
                                durationMillis = SetikerMotion.DURATION_BASE_MS,
                                easing = SetikerMotion.EaseOut
                            )
                        ) + slideInVertically(
                            animationSpec = tween(
                                durationMillis = SetikerMotion.DURATION_BASE_MS,
                                easing = SetikerMotion.EaseOut
                            )
                        ) { -it / 4 },
                        exit = fadeOut(
                            animationSpec = tween(
                                durationMillis = SetikerMotion.DURATION_FAST_MS,
                                easing = SetikerMotion.EaseOut
                            )
                        ) + slideOutVertically(
                            animationSpec = tween(
                                durationMillis = SetikerMotion.DURATION_FAST_MS,
                                easing = SetikerMotion.EaseOut
                            )
                        ) { -it / 4 }
                    ) {
                        Column {
                            OfflineBanner(
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }

                    Text(
                        text = stringResource(Res.string.home_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = neubrutalMutedOnSurface(),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(
                            top = 0.dp,
                            bottom = 12.dp
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = 0.dp,
                            bottom = 20.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = state.filteredProcessingPacks,
                            key = { "processing_${it.draftId}" }
                        ) { pack ->
                            ProcessingPackListCard(
                                pack = pack,
                                onClick = { onIntent(HomeIntent.NavigateToAiJobs) },
                                modifier = Modifier.animateItem()
                            )
                        }
                        items(
                            items = state.filteredPacks,
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

        if (showSortSheet) {
            SortBottomSheet(
                currentSort = state.sortOrder,
                onSortSelected = { onIntent(HomeIntent.SortOrderChanged(it)) },
                onDismiss = { showSortSheet = false }
            )
        }

        var presetPickerVisible by remember { mutableStateOf(false) }
        PromptPresetPickerSheet(
            visible = presetPickerVisible,
            onDismiss = { presetPickerVisible = false },
            onPresetSelected = { preset ->
                onIntent(HomeIntent.UpdateGeneratePackPrompt(preset.prompt))
            }
        )

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
                onDismiss = { onIntent(HomeIntent.CloseGeneratePackSheet) },
                aiUsage = state.aiUsage,
                isLoadingQuota = state.isLoadingAiUsage,
                quotaLoadFailed = state.aiUsageLoadFailed,
                onOpenPresets = { presetPickerVisible = true }
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
