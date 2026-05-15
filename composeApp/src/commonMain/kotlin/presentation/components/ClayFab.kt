package presentation.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalDialogRadius
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.create_new_pack

@Composable
fun ClayFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )

    val border = neubrutalBorderColor()
    Box(
        modifier = modifier
            .size(64.dp)
            .scale(scale)
            .neubrutalShadow(
                offsetX = if (isPressed) 1.dp else 4.dp,
                offsetY = if (isPressed) 1.dp else 4.dp,
                cornerRadius = NeubrutalDialogRadius,
                color = neubrutalShadowColor()
            )
            .clip(RoundedCornerShape(NeubrutalDialogRadius))
            .background(AccentCoral)
            .border(
                width = NeubrutalBorderWidth,
                color = border,
                shape = RoundedCornerShape(NeubrutalDialogRadius)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = stringResource(Res.string.create_new_pack),
            tint = NeubrutalWhite,
            modifier = Modifier.size(28.dp)
        )
    }
}

// MARK: - Previews
@Preview
@Composable
private fun ClayFabPreview() {
    MaterialTheme {
        ClayFab(onClick = {})
    }
}
