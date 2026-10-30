package presentation.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.QuotaExceededBottomSheet
import presentation.common.resolveOrDefault
import presentation.components.AppSnackbarHostState

@Composable
fun HomeScreenRoot(
    onPackClick: (String) -> Unit,
    onCreatePackClick: () -> Unit,
    onExploreClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onLoginClick: () -> Unit,
    onVideoStickerPackClick: (String) -> Unit,
    onAiJobsClick: () -> Unit = {},
    onTopBarAiJobsClick: () -> Unit = {},
    onPaywallClick: () -> Unit = {},
    showOfflineBanner: Boolean = false,
    viewModel: HomeViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { AppSnackbarHostState() }
    var showQuotaExceeded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPackDetail -> onPackClick(effect.packId)
                is HomeEffect.NavigateToCreatePack -> onCreatePackClick()
                is HomeEffect.ShowError -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
                is HomeEffect.ShowSuccess -> {
                    snackbarHostState.showMessage(effect.message.resolveOrDefault())
                }
                is HomeEffect.NavigateToProfile -> onProfileClick()
                is HomeEffect.NavigateToSync -> onSyncClick()
                is HomeEffect.NavigateToExplore -> onExploreClick()
                is HomeEffect.NavigateToLogin -> onLoginClick()
                is HomeEffect.NavigateToVideoStickerPack -> onVideoStickerPackClick(effect.videoPath)
                HomeEffect.NavigateToAiJobs -> onAiJobsClick()
                HomeEffect.NavigateToAiJobsFromTopBar -> onTopBarAiJobsClick()
                HomeEffect.ShowQuotaExceeded -> showQuotaExceeded = true
                HomeEffect.NavigateToPaywall -> onPaywallClick()
            }
        }
    }

    QuotaExceededBottomSheet(
        visible = showQuotaExceeded,
        onDismiss = { showQuotaExceeded = false },
        onGetMore = onPaywallClick
    )

    HomeScreen(
        state = state,
        onIntent = viewModel::onIntent,
        onPackClick = onPackClick,
        snackbarHostState = snackbarHostState,
        showOfflineBanner = showOfflineBanner
    )
}
