package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import presentation.icons.GlyphMorph
import presentation.icons.SetikerIcons
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalTopAppBarSurface
import presentation.theme.neubrutalFill
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.my_stickers_title
import setiker.composeapp.generated.resources.pack_details_title
import setiker.composeapp.generated.resources.settings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    titleContent: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val appBarSurface = neubrutalTopAppBarSurface()

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
                if (titleContent != null) {
                    Box(
                        modifier = Modifier.padding(start = if (onBackClick != null) 12.dp else 0.dp)
                    ) {
                        titleContent()
                    }
                } else {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = if (onBackClick != null) 12.dp else 0.dp),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = TextUnit.Unspecified
                        ),
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        NeubrutalIconButton(
                            icon = SetikerIcons.Back,
                            contentDescription = stringResource(Res.string.back_content_description),
                            onClick = onBackClick
                        )
                    }
                }
            },
            actions = {
                if (actions != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        actions.invoke(this)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = appBarSurface,
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

@Composable
fun RowScope.AppTopBarActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconTint: Color = neubrutalOnSurface(),
    iconModifier: Modifier = Modifier
) {
    NeubrutalIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        enabled = enabled,
        iconTint = iconTint,
        iconModifier = iconModifier
    )
}

/**
 * A top-bar action whose glyph morphs between two shapes.
 *
 * Same surface and press language as [AppTopBarActionIcon]; only the ink differs. The state is
 * announced explicitly because this is a plain clickable, not a toggleable.
 */
@Composable
fun RowScope.AppTopBarMorphActionIcon(
    morph: GlyphMorph,
    checked: Boolean,
    contentDescription: String,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    iconTint: Color = neubrutalOnSurface(),
    iconModifier: Modifier = Modifier,
    /**
     * Throw a burst when the action switches on.
     *
     * Off by default: most top-bar toggles are navigation, and a burst on a menu button is noise.
     * A pack becoming public is the one that earns it.
     */
    celebration: Boolean = false,
    /**
     * The ring's spread when [celebration] is on. 1.25 keeps the widest part of the ring just
     * inside the 40 dp surface's clip; a larger value gets cut by the border.
     */
    celebrationSpread: Float = 1.25f
) {
    NeubrutalSurfaceToggle(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.semantics { this.contentDescription = contentDescription },
        size = 40.dp,
        enabled = enabled,
        pressedScale = SetikerMotion.PRESS_SCALE_SMALL,
        label = "top_morph_toggle_scale"
    ) {
        MorphGlyphIcon(
            morph = morph,
            checked = checked,
            modifier = iconModifier,
            tint = iconTint,
            enabled = enabled,
            burstEnabled = celebration,
            burstSpread = celebrationSpread
        )
    }
}

/**
 * Top-bar action with optional neubrutal counter badge (e.g. pending AI jobs / drafts).
 */
@Composable
fun RowScope.AppTopBarBadgedActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    enabled: Boolean = true,
    maxDisplayCount: Int = 99
) {
    Box(modifier = Modifier.size(40.dp)) {
        NeubrutalIconButton(
            icon = icon,
            contentDescription = contentDescription,
            onClick = onClick,
            enabled = enabled
        )
        if (badgeCount > 0) {
            NeubrutalCounterBadge(
                count = badgeCount,
                maxDisplayCount = maxDisplayCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-6).dp)
            )
        }
    }
}

@Composable
fun NeubrutalCounterBadge(
    count: Int,
    modifier: Modifier = Modifier,
    maxDisplayCount: Int = 99
) {
    val label = if (count > maxDisplayCount) "$maxDisplayCount+" else count.toString()
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalSmallRadius)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .neubrutalShadow(
                offsetX = 1.dp,
                offsetY = 1.dp,
                cornerRadius = NeubrutalSmallRadius,
                color = shadow
            )
            .clip(shape)
            .neubrutalFill(color = AccentCoral, cornerRadius = NeubrutalSmallRadius)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = NeubrutalWhite,
            maxLines = 1
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
                    icon = Icons.Outlined.Settings,
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
            icon = Icons.Outlined.Settings,
            contentDescription = "Settings",
            onClick = {}
        )
    }
}
