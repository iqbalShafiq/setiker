package presentation.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NotificationsScreenRoot(
    onBackClick: () -> Unit,
    onNavigateToPublicPack: (String) -> Unit,
    onNavigateToCreator: (String) -> Unit,
    viewModel: NotificationsViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()

    NotificationsScreen(
        state = state,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onMarkAllRead = viewModel::markAllRead,
        onItemClick = { item ->
            viewModel.markRead(item.id)
            when (val nav = viewModel.resolveNavigation(item)) {
                is NotificationNavigation.PublicPack -> onNavigateToPublicPack(nav.packId)
                is NotificationNavigation.Creator -> onNavigateToCreator(nav.userId)
                null -> Unit
            }
        }
    )
}
