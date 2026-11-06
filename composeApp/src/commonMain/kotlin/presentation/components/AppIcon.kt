package presentation.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import presentation.theme.AppIconSize
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

/**
 * The only place an icon is drawn.
 *
 * Before this, 54 call sites each wrote their own `Icon(...)`, which is how the app ended up with
 * three fill levels, twelve ad-hoc sizes and a FAB glyph that inherited Compose's implicit 24 dp.
 * Everything now picks a step from [AppIconSize], so size stays on one ladder, and the disabled
 * treatment lives here instead of being re-invented as a `.copy(alpha = ...)` per call site.
 *
 * There is deliberately no "tone" enum. Every real call site either animates its colour (a like
 * button tinting coral, a follow button crossfading) or passes a plain token, so a five-value
 * "why is it drawn" enum would have had one user and four unused branches. [tint] is the honest
 * surface, and [enabled] covers the only state the icon system owns.
 *
 * [contentDescription] has no default on purpose. An icon that is a button must be labelled and an
 * icon that only decorates must say so with `null`; making the choice implicit is how unlabelled
 * controls get shipped.
 *
 * The disabled tint is deliberately *darker* than a disabled frame: the glyph says what the control
 * is, the frame says it cannot be used. Fading both by the same amount makes an unavailable control
 * unidentifiable rather than unavailable.
 */
@Composable
fun AppIcon(
    glyph: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = neubrutalOnSurface(),
    size: Dp = AppIconSize.Bar,
    enabled: Boolean = true
) {
    Icon(
        imageVector = glyph,
        contentDescription = contentDescription,
        modifier = Modifier.size(size).then(modifier),
        tint = if (enabled) tint else neubrutalMutedOnSurface()
    )
}

/**
 * Painter overload for the one icon the app does not draw itself: the WhatsApp brand mark, which
 * ships as a vector drawable resource. It goes through the same door so a brand mark still obeys
 * the size ladder.
 */
@Composable
fun AppIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = neubrutalOnSurface(),
    size: Dp = AppIconSize.Bar,
    enabled: Boolean = true
) {
    Icon(
        painter = painter,
        contentDescription = contentDescription,
        modifier = Modifier.size(size).then(modifier),
        tint = if (enabled) tint else neubrutalMutedOnSurface()
    )
}
