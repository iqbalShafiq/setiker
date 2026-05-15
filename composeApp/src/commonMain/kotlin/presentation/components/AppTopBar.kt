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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.pack_details_title
import setiker.composeapp.generated.resources.settings

/**
 * Neubrutal-styled icon button for use in top/bottom bars.
 * Small shadow + border + press animation.
 */
@Composable
fun NeubrutalIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_button_scale"
    )

    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalSmallRadius)
    val shadowX = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset
    val shadowY = if (isPressed && enabled) 1.dp else NeubrutalSmallShadowOffset

    Box(
        modifier = modifier
            .size(40.dp)
            .scale(scale)
            .neubrutalShadow(
                offsetX = shadowX,
                offsetY = shadowY,
                cornerRadius = NeubrutalSmallRadius,
                color = shadow
            )
            .clip(shape)
            .background(neubrutalCardSurface())
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
            modifier = Modifier.size(22.dp),
            tint = if (enabled) neubrutalOnSurface() else neubrutalOnSurface().copy(alpha = 0.4f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
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
        TopAppBar(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
                    ),
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                if (onBackClick != null) {
                    NeubrutalIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.back_content_description),
                        onClick = onBackClick
                    )
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        // Neubrutal thick bottom border
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(NeubrutalBorderWidth)
                .background(border)
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun AppTopBarPreview() {
    MaterialTheme {
        AppTopBar(title = stringResource(Res.string.my_stickers_title))
    }
}

@Preview
@Composable
private fun AppTopBarWithBackPreview() {
    MaterialTheme {
        AppTopBar(title = stringResource(Res.string.pack_details_title), onBackClick = {})
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AppTopBarWithActionsPreview() {
    MaterialTheme {
        AppTopBar(
            title = stringResource(Res.string.settings),
            onBackClick = {},
            actions = {
                NeubrutalIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = null,
                    onClick = {}
                )
            }
        )
    }
}

@Preview
@Composable
private fun NeubrutalIconButtonPreview() {
    MaterialTheme {
        NeubrutalIconButton(
            icon = Icons.Default.Settings,
            contentDescription = "Settings",
            onClick = {}
        )
    }
}
