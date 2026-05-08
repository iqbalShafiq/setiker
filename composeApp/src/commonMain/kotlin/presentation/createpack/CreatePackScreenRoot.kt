package presentation.createpack

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import presentation.common.resolveOrDefault

@Composable
fun CreatePackScreenRoot(
    packId: String?,
    onBackClick: () -> Unit,
    onPackSaved: (String) -> Unit,
    viewModel: CreatePackViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(packId) {
        packId?.let { viewModel.onIntent(CreatePackIntent.LoadPack(it)) }
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CreatePackEffect.PackSaved -> onPackSaved(effect.packId)
                is CreatePackEffect.NavigateBack -> onBackClick()
                is CreatePackEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    CreatePackScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
