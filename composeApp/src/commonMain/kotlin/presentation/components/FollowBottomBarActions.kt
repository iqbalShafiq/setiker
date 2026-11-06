package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import presentation.icons.SetikerMorphs
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalCardSurface

@Composable
fun FollowPackBottomBarFab(
    isFollowing: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    followContentDescription: String = "Follow",
    followingContentDescription: String = "Following"
) {
    val haptics = rememberAppHaptics()
    val beat = rememberIconBeat(trigger = isFollowing)
    val containerColor by animateColorAsState(
        targetValue = if (isFollowing) neubrutalCardSurface() else AccentCoral,
        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_BASE_MS),
        label = "follow_button_container"
    )
    val iconTint by animateColorAsState(
        targetValue = if (isFollowing) AccentCoral else NeubrutalWhite,
        animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_BASE_MS),
        label = "follow_button_icon"
    )

    PackBottomBarMorphFab(
        morph = SetikerMorphs.Follow,
        checked = isFollowing,
        contentDescription = if (isFollowing) followingContentDescription else followContentDescription,
        onCheckedChange = {
            haptics.selection()
            onClick()
        },
        enabled = enabled,
        containerColor = containerColor,
        iconTint = iconTint,
        iconModifier = Modifier.iconBeat(beat)
    )
}

