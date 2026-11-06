package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import domain.model.User
import domain.model.UserRole
import org.jetbrains.compose.resources.stringResource
import presentation.icons.SetikerIcons
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.create_new_pack
import setiker.composeapp.generated.resources.explore
import setiker.composeapp.generated.resources.generate_sticker_pack
import setiker.composeapp.generated.resources.login
import setiker.composeapp.generated.resources.profile
import setiker.composeapp.generated.resources.sync
import setiker.composeapp.generated.resources.video_to_sticker_pack

/**
 * The home action bar.
 *
 * Signed-out is the same person glyph in the muted tone rather than a second, outlined glyph: the
 * label already says "Log in", so a different drawing was a second way of saying the same thing.
 */
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
    /**
     * Whether the account actions (profile, sync) can be used.
     *
     * They stay disabled until the session has been resolved, so a tap during the cold-start
     * window cannot be mistaken for "signed out".
     */
    accountActionsEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    PackBottomBar(
        actions = {
            PackBottomBarIconButton(
                icon = SetikerIcons.Explore,
                contentDescription = stringResource(Res.string.explore),
                onClick = onExploreClick
            )
            PackBottomBarIconButton(
                icon = SetikerIcons.Person,
                contentDescription = stringResource(
                    if (currentUser != null) Res.string.profile else Res.string.login
                ),
                onClick = onProfileClick,
                enabled = accountActionsEnabled,
                iconTint = if (currentUser != null) {
                    neubrutalOnSurface()
                } else {
                    neubrutalMutedOnSurface()
                }
            )
            PendingSyncAction(
                pendingSyncCount = pendingSyncCount,
                isSyncing = isSyncing,
                onClick = onSyncClick,
                enabled = accountActionsEnabled
            )
            PackBottomBarIconButton(
                icon = SetikerIcons.Sparkle,
                contentDescription = stringResource(Res.string.generate_sticker_pack),
                onClick = onGeneratePackClick
            )
            PackBottomBarIconButton(
                icon = SetikerIcons.Movie,
                contentDescription = stringResource(Res.string.video_to_sticker_pack),
                onClick = onVideoPackClick
            )
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = SetikerIcons.Add,
                contentDescription = stringResource(Res.string.create_new_pack),
                onClick = onAddPackClick
            )
        },
        modifier = modifier
    )
}

/**
 * Sync, with the pending count as a neubrutal counter badge.
 *
 * The count used to ride a raw Material `Badge`: a soft pill with a 50 % radius, sitting on the
 * only surface in the app that has no rounded-free edges. The counter now uses the same badge as
 * the top bar.
 */
@Composable
private fun PendingSyncAction(
    pendingSyncCount: Int,
    isSyncing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Box(modifier = Modifier.size(44.dp)) {
        PackBottomBarIconButton(
            icon = SetikerIcons.Sync,
            contentDescription = stringResource(Res.string.sync),
            onClick = onClick,
            enabled = enabled
        )
        if (pendingSyncCount > 0 && !isSyncing) {
            NeubrutalCounterBadge(
                count = pendingSyncCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
            )
        }
    }
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
