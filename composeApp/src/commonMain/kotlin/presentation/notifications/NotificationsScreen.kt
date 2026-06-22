package presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppIllustration
import presentation.components.AppPrimaryButton
import presentation.components.AppTopBar
import presentation.components.EmptyState
import presentation.components.LoadingIndicator
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.notifications_empty_desc
import setiker.composeapp.generated.resources.notifications_empty_title
import setiker.composeapp.generated.resources.notifications_load_failed
import setiker.composeapp.generated.resources.notifications_mark_all_read
import setiker.composeapp.generated.resources.notifications_title
import setiker.composeapp.generated.resources.retry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onMarkAllRead: () -> Unit,
    onItemClick: (data.remote.model.UserNotificationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val title = if (state.unreadCount > 0) {
        "${stringResource(Res.string.notifications_title)} (${state.unreadCount})"
    } else {
        stringResource(Res.string.notifications_title)
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = onBackClick,
                actions = {
                    if (state.unreadCount > 0 && !state.isLoading) {
                        TextButton(onClick = onMarkAllRead) {
                            Text(stringResource(Res.string.notifications_mark_all_read))
                        }
                    }
                }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading && state.items.isNotEmpty(),
            onRefresh = onRefresh,
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.isLoading && state.items.isEmpty() -> {
                    LoadingIndicator(Modifier.fillMaxSize())
                }
                state.loadFailed -> {
                    EmptyState(
                        title = stringResource(Res.string.notifications_load_failed),
                        description = "",
                        illustration = AppIllustration.ErrorState,
                        modifier = Modifier.fillMaxSize(),
                        action = {
                            AppPrimaryButton(
                                text = stringResource(Res.string.retry),
                                onClick = onRefresh
                            )
                        }
                    )
                }
                state.items.isEmpty() -> {
                    EmptyState(
                        title = stringResource(Res.string.notifications_empty_title),
                        description = stringResource(Res.string.notifications_empty_desc),
                        illustration = AppIllustration.SearchEmpty,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp)
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            val unread = item.readAt == null
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (unread) FontWeight.Bold else FontWeight.Normal,
                                color = neubrutalOnSurface(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onItemClick(item) }
                                    .padding(vertical = 10.dp)
                            )
                            item.body?.let { body ->
                                Text(
                                    text = body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = neubrutalMutedOnSurface(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onItemClick(item) }
                                        .padding(bottom = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
