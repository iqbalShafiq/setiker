package presentation.packdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject

@Composable
fun PackDetailScreenRoot(
    packId: String,
    onBackClick: () -> Unit,
    onEditPack: (String) -> Unit,
    onAddSticker: (String) -> Unit,
    onEditSticker: (Int, String) -> Unit,
    onAddToWhatsApp: ((String, String) -> Unit)? = null,
    viewModel: PackDetailViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
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
                is PackDetailEffect.LaunchAddToWhatsApp -> {
                    onAddToWhatsApp?.invoke(effect.packId, effect.packName)
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
