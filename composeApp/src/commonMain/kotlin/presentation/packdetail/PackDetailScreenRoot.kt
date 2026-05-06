package presentation.packdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.koinInject

@Composable
fun PackDetailScreenRoot(
    packId: String,
    onBackClick: () -> Unit,
    onEditPack: (String) -> Unit,
    onAddSticker: (String) -> Unit,
    onEditSticker: (Int, String) -> Unit,
    viewModel: PackDetailViewModel = koinInject()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onIntent(PackDetailIntent.LoadPack(packId))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PackDetailEffect.NavigateBack -> onBackClick()
                is PackDetailEffect.NavigateToEditPack -> onEditPack(packId)
                is PackDetailEffect.NavigateToAddSticker -> onAddSticker(packId)
                is PackDetailEffect.NavigateToEditSticker -> onEditSticker(effect.index, packId)
                is PackDetailEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is PackDetailEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is PackDetailEffect.ShowShareSheet -> {
                    snackbarHostState.showSnackbar("Sharing pack: $packId")
                }
            }
        }
    }

    PackDetailScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        onEditPack = { onEditPack(packId) },
        onAddSticker = { onAddSticker(packId) },
        onEditSticker = { index -> onEditSticker(index, packId) },
        snackbarHostState = snackbarHostState
    )
}
