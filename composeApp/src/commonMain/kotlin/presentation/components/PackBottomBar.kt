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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackBottomBar(
    actions: @Composable RowScope.() -> Unit,
    floatingActionButton: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = 0.dp,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = 0.dp,
                color = shadow
            )
    ) {
        // Neubrutal thick top border
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(NeubrutalBorderWidth)
                .background(border)
        )
        BottomAppBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = neubrutalCardSurface(),
            contentColor = neubrutalOnSurface(),
            tonalElevation = 0.dp,
            actions = actions,
            floatingActionButton = floatingActionButton
        )
    }
}

@Composable
fun PackBottomBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = neubrutalOnSurface()
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun PackBottomBarFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalButtonRadius)
    val shadowX = if (isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shadowY = if (isPressed && enabled) NeubrutalSmallShadowOffset else NeubrutalShadowOffset

    Box(
        modifier = modifier
            .size(56.dp)
            .scale(scale)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalButtonRadius,
                color = shadow
            )
            .clip(shape)
            .background(if (enabled) AccentCoral else AccentCoral.copy(alpha = 0.4f))
            .border(
                width = NeubrutalBorderWidth,
                color = border,
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) NeubrutalWhite else NeubrutalWhite.copy(alpha = 0.5f)
        )
    }
}

// MARK: - Previews
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PackBottomBarPreview() {
    MaterialTheme {
        PackBottomBar(
            actions = {
                PackBottomBarIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = {}
                )
            },
            floatingActionButton = {
                PackBottomBarFab(
                    icon = Icons.Default.Add,
                    contentDescription = "Add",
                    onClick = {}
                )
            }
        )
    }
}

@Preview
@Composable
private fun PackBottomBarIconButtonPreview() {
    MaterialTheme {
        PackBottomBarIconButton(
            icon = Icons.Default.Edit,
            contentDescription = "Edit",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun PackBottomBarFabPreview() {
    MaterialTheme {
        PackBottomBarFab(
            icon = Icons.Default.Add,
            contentDescription = "Add",
            onClick = {}
        )
    }
}
