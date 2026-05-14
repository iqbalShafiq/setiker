package presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import domain.model.User
import domain.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomBar(
    currentUser: User?,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onAddPackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PackBottomBar(
        actions = {
            IconButton(onClick = onProfileClick) {
                Icon(
                    imageVector = if (currentUser != null) Icons.Default.Person else Icons.Outlined.AccountCircle,
                    contentDescription = if (currentUser != null) "Profile" else "Login"
                )
            }
            BadgedBox(badge = { if (pendingSyncCount > 0 && !isSyncing) { Badge { Text(pendingSyncCount.toString()) } } }) {
                IconButton(onClick = onSyncClick) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Sync")
                }
            }
        },
        floatingActionButton = {
            PackBottomBarFab(icon = Icons.Default.Add, contentDescription = "Create Pack", onClick = onAddPackClick)
        },
        modifier = modifier
    )
}

// MARK: - Previews

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeBottomBarLoggedInPreview() {
    val mockUser = User(
        id = "1",
        email = "test@example.com",
        username = "testuser",
        name = "Test User",
        role = UserRole(id = "1", name = "user"),
        isActive = true,
        createdAt = 0L
    )
    MaterialTheme {
        HomeBottomBar(
            currentUser = mockUser,
            pendingSyncCount = 3,
            isSyncing = false,
            onProfileClick = {},
            onSyncClick = {},
            onAddPackClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun HomeBottomBarGuestPreview() {
    MaterialTheme {
        HomeBottomBar(
            currentUser = null,
            pendingSyncCount = 0,
            isSyncing = false,
            onProfileClick = {},
            onSyncClick = {},
            onAddPackClick = {}
        )
    }
}