package presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppTopBar
import presentation.components.ClayFab
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.StickerPackCard
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalBg
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.home_hint
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.no_stickers_yet_desc
import setiker.composeapp.generated.resources.no_stickers_yet_title

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

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(Res.string.my_stickers_title),
                actions = {}
            )
        },
        floatingActionButton = {
            ClayFab(onClick = { onIntent(HomeIntent.CreateNewPack) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeubrutalBg
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
                ) {
                    Text(
                        text = stringResource(Res.string.home_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = NeubrutalBlack.copy(alpha = 0.72f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            items = state.packs,
                            key = { it.identifier }
                        ) { pack ->
                            StickerPackCard(
                                pack = pack,
                                onClick = { onPackClick(pack.identifier) }
                            )
                        }
                    }
                }
            }
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
