package presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.components.AppTopBar
import presentation.components.LoadingIndicator
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@Composable
fun NotificationsScreen(
    state: NotificationsState,
    onBackClick: () -> Unit,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = if (state.unreadCount > 0) "Notifications (${state.unreadCount})" else "Notifications",
                onBackClick = onBackClick
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        if (state.isLoading) {
            LoadingIndicator(Modifier.fillMaxSize().padding(innerPadding))
        } else {
            LazyColumn(
                modifier = modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp)
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
                            .clickable { onItemClick(item.id) }
                            .padding(vertical = 10.dp)
                    )
                    item.body?.let { body ->
                        Text(
                            text = body,
                            style = MaterialTheme.typography.bodySmall,
                            color = neubrutalMutedOnSurface(),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
