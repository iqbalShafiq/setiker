package presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonCheck
import androidx.compose.runtime.Composable
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface

@Composable
fun FollowPackBottomBarIconButton(
    isFollowing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    followContentDescription: String = "Follow",
    unfollowContentDescription: String = "Unfollow"
) {
    PackBottomBarIconButton(
        icon = if (isFollowing) Icons.Filled.PersonCheck else Icons.Default.PersonAdd,
        contentDescription = if (isFollowing) unfollowContentDescription else followContentDescription,
        onClick = onClick,
        enabled = enabled,
        iconTint = if (isFollowing) AccentCoral else neubrutalOnSurface()
    )
}

@Composable
fun FollowPackBottomBarFab(
    isFollowing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    followContentDescription: String = "Follow",
    followingContentDescription: String = "Following"
) {
    PackBottomBarFab(
        icon = if (isFollowing) Icons.Filled.PersonCheck else Icons.Default.PersonAdd,
        contentDescription = if (isFollowing) followingContentDescription else followContentDescription,
        onClick = onClick,
        enabled = enabled,
        containerColor = if (isFollowing) neubrutalCardSurface() else AccentCoral,
        iconTint = if (isFollowing) AccentCoral else NeubrutalWhite
    )
}
