package presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalCardSurface

@Composable
fun FollowPackBottomBarFab(
    isFollowing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    followContentDescription: String = "Follow",
    followingContentDescription: String = "Following"
) {
    PackBottomBarFab(
        icon = if (isFollowing) Icons.Filled.HowToReg else Icons.Default.PersonAdd,
        contentDescription = if (isFollowing) followingContentDescription else followContentDescription,
        onClick = onClick,
        enabled = enabled,
        containerColor = if (isFollowing) neubrutalCardSurface() else AccentCoral,
        iconTint = if (isFollowing) AccentCoral else NeubrutalWhite
    )
}
