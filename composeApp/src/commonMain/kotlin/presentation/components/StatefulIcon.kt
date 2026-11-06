package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import presentation.common.rememberReduceMotion
import presentation.icons.MorphIcon
import presentation.icons.SetikerIcons
import presentation.theme.AccentCoral
import presentation.theme.AppIconSize
import presentation.theme.IconGridSize
import presentation.theme.NeubrutalBg
import presentation.theme.SetikerMotion
import presentation.theme.WarningYellow
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

/**
 * The stateless half of a stateful icon: it draws whatever progress it is handed.
 *
 * Splitting it out is not decoration. A screenshot test cannot reason about a running animation,
 * and the repo already holds its snackbar timer still for exactly this reason — the reviewable
 * thing is the frame, so the frame has to be addressable. [StatefulIcon] owns the clock; this owns
 * the ink.
 *
 * Everything is read inside the draw lambda, so a fill never recomposes the button around it.
 *
 * @param progress 0 = outline only, 1 = fully filled. Clipped bottom-up so the ink rises into the
 *   shape instead of the shape growing.
 * @param burst 0 at the moment the icon switches on, 1 when the particles have died. `null` draws
 *   no burst at all.
 */
@Composable
fun MorphIconVisual(
    icon: MorphIcon,
    progress: State<Float>,
    modifier: Modifier = Modifier,
    size: Dp = AppIconSize.Bar,
    outlineTint: Color = neubrutalOnSurface(),
    solidTint: Color = AccentCoral,
    burst: State<Float>? = null
) {
    val outlinePainter = rememberVectorPainter(icon.outline)
    val solidPainter = rememberVectorPainter(icon.solid)
    val outlineFilter = remember(outlineTint) { ColorFilter.tint(outlineTint) }
    val solidFilter = remember(solidTint) { ColorFilter.tint(solidTint) }

    Canvas(modifier = modifier.size(size)) {
        val fill = progress.value.coerceIn(0f, 1f)
        // Named so it does not collide with the `size: Dp` parameter of this composable.
        val canvasSize = this.size

        if (fill > 0f) {
            clipRect(top = canvasSize.height * (1f - fill)) {
                with(solidPainter) {
                    draw(size = canvasSize, alpha = 1f, colorFilter = solidFilter)
                }
            }
        }

        // The outline is drawn last and is never clipped, so the border stays 2.5 dp black at every
        // point of the fill — the thing that keeps a filled Setiker icon neubrutal.
        with(outlinePainter) {
            draw(size = canvasSize, alpha = 1f, colorFilter = outlineFilter)
        }

        if (burst != null) {
            drawBurst(burst.value.coerceIn(0f, 1f))
        }
    }
}

/**
 * The stateful half: owns the clock, decides how long filling and draining take, and drops the
 * movement when the platform asks for reduced motion while keeping the fill itself.
 *
 * The first composition never animates. A screen that opens on an already-liked pack should show a
 * filled heart, not a heart that fills in front of you.
 */
@Composable
fun StatefulIcon(
    icon: MorphIcon,
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = AppIconSize.Bar,
    outlineTint: Color = neubrutalOnSurface(),
    solidTint: Color = AccentCoral,
    burstEnabled: Boolean = true,
    enabled: Boolean = true
) {
    // The clock is shared with MorphGlyphIcon: same asymmetry, same reduced-motion rule, same
    // "a screen that opens on an already-checked state does not replay it" rule.
    val progress = rememberStateProgress(checked)
    val reduceMotion = rememberReduceMotion()
    val burst = remember { Animatable(1f) }
    var burstArmed by remember { mutableStateOf(false) }

    // Same rule as AppIcon: a disabled glyph goes muted once, here, instead of every caller
    // remembering to fade it.
    val resolvedOutline = if (enabled) outlineTint else neubrutalMutedOnSurface()
    val resolvedSolid = if (enabled) solidTint else neubrutalMutedOnSurface()

    // The burst outlives the fill, so it keeps its own effect; reduced motion kills the particles
    // and leaves the fill, which is the part that carries meaning.
    LaunchedEffect(checked, reduceMotion, burstEnabled) {
        if (!burstArmed) {
            burstArmed = true
            return@LaunchedEffect
        }
        if (reduceMotion || !checked || !burstEnabled) {
            burst.snapTo(1f)
            return@LaunchedEffect
        }
        burst.snapTo(0f)
        burst.animateTo(
            targetValue = 1f,
            animationSpec = tween(SetikerMotion.ICON_BURST_MS, easing = LinearEasing)
        )
    }

    MorphIconVisual(
        icon = icon,
        progress = progress,
        modifier = modifier,
        size = size,
        outlineTint = resolvedOutline,
        solidTint = resolvedSolid,
        burst = if (burstEnabled) burst.asState() else null
    )
}

/**
 * The semantic half: a 48 dp target whose state is announced, not just drawn.
 *
 * `toggleable` is used instead of `clickable` because a like is a boolean state, and Compose
 * derives `ToggleableState` plus a spoken "checked / not checked" from it for free. Animation is
 * never the only carrier of meaning: with animations off, a screen reader, or a screenshot, the
 * state still reads.
 *
 * @param contentDescription names the action ("Like"). The on/off part comes from the toggle,
 *   so it must not be baked into the label — otherwise it contradicts the real state.
 */
@Composable
fun StatefulIconToggle(
    icon: MorphIcon,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    iconSize: Dp = AppIconSize.Bar,
    pressedScale: Float = SetikerMotion.PRESS_SCALE_SMALL,
    label: String = "stateful_icon_scale"
) {
    val pressFeedback = rememberPressFeedback(
        pressedScale = pressedScale,
        enabled = enabled,
        label = label
    )

    Box(
        modifier = modifier
            .size(size)
            .pressScale(pressFeedback)
            .toggleable(
                value = checked,
                interactionSource = pressFeedback.interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        StatefulIcon(
            icon = icon,
            checked = checked,
            size = iconSize,
            burstEnabled = enabled
        )
    }
}

private const val BurstParticleCount = 8

/** Viewport-space origin of the burst: the heart's visual centre, not the grid centre. */
private const val BurstOriginX = 12f
private const val BurstOriginY = 13.4f

/**
 * Eight particles leaving the glyph and dying.
 *
 * Drawn as one pass inside the same [DrawScope] as the icon: no composables, no allocation per
 * frame, no state. The angles carry an 11° offset so the ring does not read as a mechanical star,
 * and the distance is capped inside the 44 dp button that hosts a 22 dp bar icon.
 */
internal fun DrawScope.drawBurst(burst: Float, spread: Float = 1f) {
    if (burst <= 0f || burst >= 1f) return

    val unit = size.minDimension / IconGridSize
    val ease = 1f - (1f - burst) * (1f - burst) * (1f - burst)
    // Starts clear of the silhouette: a particle that begins at the centre spends its brightest
    // frames on top of the ink it is supposed to announce. `spread` widens the ring for a glyph
    // wide enough to swallow it — the closed eye's lash reaches 8.4 units each way, so a ring
    // sized for a compact heart is drawn inside the ink instead of around it.
    val distance = (5f + 8f * ease) * spread
    val radius = (1.15f - 0.72f * ease) * unit
    // Fades in over the first sixth and out over the rest, so nothing pops into existence.
    val fadeIn = (burst / 0.16f).coerceAtMost(1f)
    val alpha = fadeIn * (1f - burst) * (1f - burst * 0.35f)

    for (index in 0 until BurstParticleCount) {
        val angle = (-90.0 + index * (360.0 / BurstParticleCount) + 11.0) * (Math.PI / 180.0)
        val x = (BurstOriginX + distance * cos(angle).toFloat()) * unit
        val y = (BurstOriginY + distance * sin(angle).toFloat()) * unit
        drawCircle(
            color = if (index % 3 == 0) WarningYellow else AccentCoral,
            radius = radius,
            center = Offset(x, y),
            alpha = alpha
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun StatefulIconStatesPreview() {
    Row(
        modifier = Modifier.background(NeubrutalBg).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphIconVisual(
            icon = SetikerIcons.HeartMorph,
            progress = remember { mutableStateOf(0f) },
            size = 44.dp
        )
        MorphIconVisual(
            icon = SetikerIcons.HeartMorph,
            progress = remember { mutableStateOf(0.55f) },
            size = 44.dp,
            burst = remember { mutableStateOf(0.35f) }
        )
        MorphIconVisual(
            icon = SetikerIcons.HeartMorph,
            progress = remember { mutableStateOf(1f) },
            size = 44.dp
        )
        StatefulIconToggle(
            icon = SetikerIcons.HeartMorph,
            checked = true,
            onCheckedChange = {},
            contentDescription = "Like",
            size = 44.dp
        )
    }
}
