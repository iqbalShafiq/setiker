package presentation.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.common.ContentStateAnimations
import presentation.icons.GlyphMorph
import presentation.icons.MorphIcon
import presentation.icons.SetikerMorphs
import presentation.icons.SetikerIcons
import presentation.theme.AccentCoral
import presentation.theme.AppIconSize
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalBottomAppBarSurface
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalFill
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadowColor
import presentation.theme.NeubrutalBorderRole
import presentation.theme.neubrutalBorderFor
import presentation.theme.neubrutalDisabledFill

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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bottomBarSurface)
                .packBottomBarOuterPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = actionStatusText,
                    transitionSpec = { ContentStateAnimations.fadeOnly() },
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
            .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = NeubrutalSmallRadius)
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
    iconTint: Color = neubrutalOnSurface(),
    iconModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    NeubrutalSurfaceIcon(
        modifier = modifier,
        size = 44.dp,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = SetikerMotion.PRESS_SCALE_SMALL,
        label = "bottom_icon_scale",
        onClick = onClick
    ) {
        AppIcon(
            glyph = icon,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = iconTint,
            enabled = enabled
        )
    }
}

/**
 * A bar button whose glyph has two states.
 *
 * The icon keeps the surface, the press spring and the hard shadow of every other bar button; only
 * the ink changes. It is a real toggle, so the state is announced by Compose rather than by a
 * hand-written description that could drift away from the value it claims to describe.
 */
@Composable
fun PackBottomBarIconButton(
    morph: MorphIcon,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    modifier: Modifier = Modifier
) {
    NeubrutalSurfaceToggle(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        size = 44.dp,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = SetikerMotion.PRESS_SCALE_SMALL,
        label = "bottom_morph_toggle_scale"
    ) {
        StatefulIcon(
            icon = morph,
            checked = checked,
            enabled = enabled
        )
    }
}

/**
 * A bar button whose glyph *changes shape* rather than filling - play to pause, for example.
 *
 * Separate from the [MorphIcon] overload because the two mechanisms differ: that one keeps one
 * silhouette and wipes ink into it, this one interpolates between two silhouettes.
 */
@Composable
fun PackBottomBarIconButton(
    glyphMorph: GlyphMorph,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    modifier: Modifier = Modifier
) {
    NeubrutalSurfaceToggle(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.semantics { this.contentDescription = contentDescription },
        size = 44.dp,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = SetikerMotion.PRESS_SCALE_SMALL,
        label = "bottom_glyph_morph_toggle_scale"
    ) {
        MorphGlyphIcon(
            morph = glyphMorph,
            checked = checked,
            enabled = enabled
        )
    }
}
/**
 * Painter variant, for the WhatsApp brand mark. Kept as a second overload rather than a second
 * render path: it shares the same surface, the same press spring and the same [AppIcon] door.
 */
@Composable
fun PackBottomBarIconButton(
    painter: Painter,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    containerColor: Color = neubrutalCardSurface(),
    iconTint: Color = neubrutalOnSurface(),
    iconModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    NeubrutalSurfaceIcon(
        modifier = modifier,
        size = 44.dp,
        enabled = enabled,
        containerColor = containerColor,
        pressedScale = SetikerMotion.PRESS_SCALE_SMALL,
        label = "bottom_painter_icon_scale",
        onClick = onClick
    ) {
        AppIcon(
            painter = painter,
            contentDescription = contentDescription,
            modifier = iconModifier,
            tint = iconTint,
            enabled = enabled
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
    /** The action finished successfully: the busy dots become a tick. */
    isComplete: Boolean = false,
    containerColor: Color = AccentCoral,
    iconTint: Color = NeubrutalWhite,
    iconModifier: Modifier = Modifier
) {
    NeubrutalFabSurface(
        contentDescription = contentDescription,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        isLoading = isLoading,
        containerColor = containerColor
    ) {
        val glyph = when {
            isComplete -> FabGlyph.Complete
            isLoading -> FabGlyph.Busy
            else -> FabGlyph.Idle
        }
        AnimatedContent(
            targetState = glyph,
            transitionSpec = { ContentStateAnimations.fadeOnly() },
            label = "fab_loading_content"
        ) { current ->
            when (current) {
                FabGlyph.Busy -> LoadingDots(dotSize = 6.dp, color = iconTint)

                // The busy dots collapse into one pip, and that pip becomes the tick. `playOnEnter`
                // is what makes it animate: this branch is mounted *because* the work finished.
                FabGlyph.Complete -> MorphGlyphIcon(
                    morph = SetikerMorphs.Loaded,
                    checked = true,
                    playOnEnter = true,
                    size = AppIconSize.Fab,
                    tint = iconTint
                )

                FabGlyph.Idle -> AppIcon(
                    glyph = icon,
                    contentDescription = null,
                    modifier = iconModifier,
                    tint = iconTint,
                    size = AppIconSize.Fab,
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * The same primary action, showing a glyph that morphs with its state.
 *
 * Follow is the case: the tick arrives by folding the plus away, so the button reports what
 * changed instead of swapping two unrelated icons.
 */
@Composable
fun PackBottomBarMorphFab(
    morph: GlyphMorph,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = AccentCoral,
    iconTint: Color = NeubrutalWhite,
    iconModifier: Modifier = Modifier
) {
    NeubrutalFabSurface(
        contentDescription = contentDescription,
        onClick = { onCheckedChange(!checked) },
        modifier = modifier,
        enabled = enabled,
        isLoading = false,
        containerColor = containerColor
    ) {
        MorphGlyphIcon(
            morph = morph,
            checked = checked,
            modifier = iconModifier,
            size = AppIconSize.Fab,
            tint = iconTint,
            enabled = enabled
        )
    }
}

/**
 * The round action surface, without an opinion about the glyph inside it.
 *
 * Both FABs share this so the border, the shadow, the focus ring and the disabled treatment stay
 * one implementation. The label lives here too: it belongs to the control, not to whichever
 * branch happens to be showing.
 */
@Composable
private fun NeubrutalFabSurface(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    isLoading: Boolean,
    containerColor: Color,
    content: @Composable BoxScope.() -> Unit
) {
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_COMPACT,
        enabled = enabled && !isLoading,
        label = "fab_scale"
    )

    val border = neubrutalBorderFor(
        if (enabled || isLoading) NeubrutalBorderRole.Container else NeubrutalBorderRole.Disabled
    )
    val shape = RoundedCornerShape(NeubrutalButtonRadius)

    Box(
        modifier = modifier
            .size(56.dp)
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalButtonRadius,
                // A raised shadow on a disabled control promises a press it cannot deliver.
                color = if (enabled || isLoading) {
                    neubrutalShadowColor()
                } else {
                    neubrutalShadowColor().copy(alpha = 0.4f)
                }
            )
            .clip(shape)
            .neubrutalFill(
                // Alpha here would let the black shadow show through and turn a disabled coral
                // FAB brown; compositing against the surface keeps it a muted coral.
                color = if (enabled || isLoading) {
                    containerColor
                } else {
                    neubrutalDisabledFill(containerColor)
                },
                cornerRadius = NeubrutalButtonRadius
            )
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalButtonRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            )
            .clickableNoRipple(
                interactionSource = pressFeedback.interactionSource,
                enabled = enabled && !isLoading,
                // Coral on a coral fill is invisible, so a filled action takes the light ring.
                focusRingColor = NeubrutalWhite,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = content
    )
}

/** What the primary action currently shows. */
private enum class FabGlyph { Idle, Busy, Complete }

// MARK: - Previews
@Preview
@Composable
private fun PackBottomBarPreview() {
    MaterialTheme {
        PackBottomBar(
            actions = {
                PackBottomBarIconButton(
                    icon = SetikerIcons.Edit,
                    contentDescription = "Edit",
                    onClick = {}
                )
                PackBottomBarIconButton(
                    icon = SetikerIcons.Edit,
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
            icon = SetikerIcons.Edit,
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


