package presentation.home

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.QuotaExceededBottomSheet
import presentation.common.resolveOrDefault

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
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showQuotaExceeded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel.effect) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is HomeEffect.NavigateToPackDetail -> onPackClick(effect.packId)
                is HomeEffect.NavigateToCreatePack -> onCreatePackClick()
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
                }
                is HomeEffect.ShowSuccess -> {
                    snackbarHostState.showSnackbar(effect.message.resolveOrDefault())
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
