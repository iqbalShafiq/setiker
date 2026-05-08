package presentation.crop

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import org.koin.compose.koinInject
import presentation.common.resolveOrDefault

@Composable
fun CropScreenRoot(
    imagePath: String,
    onBackClick: () -> Unit,
    onImageCropped: (String) -> Unit,
    viewModel: CropViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(imagePath) {
        viewModel.onIntent(CropIntent.LoadImage(imagePath))
    }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is CropEffect.ImageCropped -> onImageCropped(effect.path)
                is CropEffect.NavigateBack -> onBackClick()
                is CropEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
            }
        }
    }

    CropScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onBackClick = onBackClick,
        snackbarHostState = snackbarHostState
    )
}
