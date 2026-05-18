package presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import domain.model.User
import domain.model.UserRole
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.create_new_pack
import setiker.composeapp.generated.resources.explore
import setiker.composeapp.generated.resources.generate_sticker_pack
import setiker.composeapp.generated.resources.login
import setiker.composeapp.generated.resources.profile
import setiker.composeapp.generated.resources.sync
import setiker.composeapp.generated.resources.video_to_sticker_pack

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeBottomBar(
    currentUser: User?,
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onExploreClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSyncClick: () -> Unit,
    onGeneratePackClick: () -> Unit,
    onVideoPackClick: () -> Unit,
    onAddPackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PackBottomBar(
        actions = {
            PackBottomBarIconButton(
                icon = Icons.Default.Explore,
                contentDescription = stringResource(Res.string.explore),
                onClick = onExploreClick
            )
            PackBottomBarIconButton(
                icon = if (currentUser != null) Icons.Default.Person else Icons.Outlined.AccountCircle,
                contentDescription = stringResource(
                    if (currentUser != null) Res.string.profile else Res.string.login
                ),
                onClick = onProfileClick
            )
            BadgedBox(
                badge = {
                    if (pendingSyncCount > 0 && !isSyncing) {
                        Badge { Text(pendingSyncCount.toString()) }
                    }
                }
            ) {
                PackBottomBarIconButton(
                    icon = Icons.Default.Refresh,
                    contentDescription = stringResource(Res.string.sync),
                    onClick = onSyncClick
                )
            }
            PackBottomBarIconButton(
                icon = Icons.Default.AutoAwesome,
                contentDescription = stringResource(Res.string.generate_sticker_pack),
                onClick = onGeneratePackClick
            )
            PackBottomBarIconButton(
                icon = Icons.Default.Movie,
                contentDescription = stringResource(Res.string.video_to_sticker_pack),
                onClick = onVideoPackClick
            )
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = Icons.Default.Add,
                contentDescription = stringResource(Res.string.create_new_pack),
                onClick = onAddPackClick
            )
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
            onExploreClick = {},
            onProfileClick = {},
            onSyncClick = {},
            onGeneratePackClick = {},
            onVideoPackClick = {},
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
            onExploreClick = {},
            onProfileClick = {},
            onSyncClick = {},
            onGeneratePackClick = {},
            onVideoPackClick = {},
            onAddPackClick = {}
        )
    }
}
