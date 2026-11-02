package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    var hasAppeared by remember { mutableStateOf(false) }
    val iconPop = remember { Animatable(1f) }
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

    LaunchedEffect(isFollowing) {
        if (!hasAppeared) {
            hasAppeared = true
            return@LaunchedEffect
        }
        iconPop.snapTo(1.24f)
        iconPop.animateTo(1f, SetikerMotion.DelightSpring)
    }

    PackBottomBarFab(
        icon = if (isFollowing) Icons.Filled.HowToReg else Icons.Default.PersonAdd,
        contentDescription = if (isFollowing) followingContentDescription else followContentDescription,
        onClick = {
            haptics.selection()
            onClick()
        },
        enabled = enabled,
        containerColor = containerColor,
        iconTint = iconTint,
        iconModifier = Modifier.graphicsLayer {
            scaleX = iconPop.value
            scaleY = iconPop.value
        }
    )
}
