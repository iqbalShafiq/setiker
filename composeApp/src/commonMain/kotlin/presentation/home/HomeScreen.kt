package presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import domain.model.Sticker
import domain.model.StickerPack
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.components.StickerPackCard

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
                title = "My Stickers",
                actions = {
                    // Add settings or other actions here
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onIntent(HomeIntent.CreateNewPack) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create new pack"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    title = "No Stickers Yet",
                    description = "Create your first sticker pack to get started",
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
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