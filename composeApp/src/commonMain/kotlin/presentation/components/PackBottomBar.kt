package presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBottomAppBarSurface
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun PackBottomBar(
    actions: @Composable RowScope.() -> Unit,
    floatingActionButton: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    actionStatusText: String? = null
) {
    val border = neubrutalBorderColor()
    val bottomBarSurface = neubrutalBottomAppBarSurface()

    Column(
        modifier = modifier
            .imePadding()
            .fillMaxWidth()
    ) {
        // Neubrutal thick top border
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(NeubrutalBorderWidth)
                .background(border)
        )
        val navBarInsets = WindowInsets.navigationBars.asPaddingValues()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bottomBarSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = navBarInsets.calculateBottomPadding()),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = actionStatusText,
                    label = "bottom_bar_actions_content"
                ) { statusText ->
                    if (statusText.isNullOrBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    } else {
                        PackBottomBarStatusText(text = statusText)
                    }
                }
            }
            if (floatingActionButton != null) {
                floatingActionButton()
            }
        }
    }
}

@Composable
private fun PackBottomBarStatusText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(NeubrutalSmallRadius))
            .background(neubrutalCardSurface())
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(horizontal = 14.dp, vertical = 11.dp),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = neubrutalOnSurface()
    )
}

@Composable
fun PackBottomBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    iconTint: Color = neubrutalOnSurface()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottom_icon_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalSmallRadius)
    val shadowX = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset
    val shadowY = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalSmallRadius,
                color = shadow
            )
            .clip(shape)
            .background(containerColor)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor()
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
            modifier = Modifier.size(22.dp),
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun PackBottomBarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    iconTint: Color = neubrutalOnSurface()
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottom_painter_icon_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalSmallRadius)
    val shadowX = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset
    val shadowY = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalSmallRadius,
                color = shadow
            )
            .clip(shape)
            .background(containerColor)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor()
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
            painter = painter,
            contentDescription = contentDescription,
            modifier = Modifier.size(22.dp),
            tint = if (enabled) iconTint else iconTint.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun PackBottomBarFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    containerColor: Color = AccentCoral,
    iconTint: Color = NeubrutalWhite
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled && !isLoading) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fab_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalButtonRadius)
    val shadowX = if (isPressed && enabled && !isLoading) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val shadowY = if (isPressed && enabled && !isLoading) NeubrutalSmallShadowOffset else NeubrutalShadowOffset
    val isClickable = enabled && !isLoading

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
            .background(
                if (enabled || isLoading) containerColor else containerColor.copy(alpha = 0.4f)
            )
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalButtonRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = isClickable,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = isLoading, label = "fab_loading_content") { loading ->
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                    color = iconTint
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = if (enabled) iconTint else iconTint.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// MARK: - Previews
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
                PackBottomBarIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit 2",
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
