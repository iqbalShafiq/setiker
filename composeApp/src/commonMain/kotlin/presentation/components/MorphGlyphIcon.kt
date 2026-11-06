package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.common.rememberReduceMotion
import presentation.icons.GlyphMorph
import presentation.icons.SetikerMorphs
import presentation.icons.lerp
import presentation.icons.writeLerpInto
import presentation.theme.AppIconSize
import presentation.theme.IconGridSize
import presentation.theme.IconStrokeWidth
import presentation.theme.NeubrutalBg
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

/**
 * One glyph morphing into a different glyph.
 *
 * This is the other half of the stateful-icon story. [StatefulIcon] keeps one silhouette and fills
 * it; this one changes the silhouette itself — an eye closing, a play turning into a pause. Both
 * read the same clock ([rememberStateProgress]) and both are drawn in the draw phase, so neither
 * recomposes the control around them.
 *
 * Shapes are authored on the [IconGridSize] grid and scaled to the requested size, which keeps them
 * on the same 24 unit contract as every other glyph — including the 2.5 unit stroke.
 */
@Composable
fun MorphGlyphIcon(
    morph: GlyphMorph,
    checked: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = AppIconSize.Bar,
    tint: Color = neubrutalOnSurface(),
    enabled: Boolean = true,
    playOnEnter: Boolean = false,
    /**
     * Whether switching on throws a burst of particles.
     *
     * Opt-in because a burst is a celebration: a play button that throws confetti is noise, while a
     * pack becoming public earns one. The particles come from the same `drawBurst` the heart uses,
     * so there is one particle language in the app, not two.
     */
    burstEnabled: Boolean = false,
    /**
     * How far the ring sits from the centre, as a multiple of the default.
     *
     * A wide glyph swallows a ring sized for a compact one. The ceiling is the host button: its
     * clip cuts anything past the border, so a spread that overflows does not look generous, it
     * looks cropped.
     */
    burstSpread: Float = 1f
) {
    val reduceMotion = rememberReduceMotion()
    val burst = remember { Animatable(1f) }
    var burstArmed by remember { mutableStateOf(false) }

    // Same contract as StatefulIcon: the first composition never celebrates — a screen that opens
    // on an already-public pack should not throw a party at nobody — and reduced motion keeps the
    // morph but drops the particles, because the morph is the part that carries meaning.
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

    MorphGlyphVisual(
        morph = morph,
        progress = rememberStateProgress(checked, playOnEnter = playOnEnter),
        modifier = modifier,
        size = size,
        tint = tint,
        enabled = enabled,
        burst = if (burstEnabled) burst.asState() else null,
        burstSpread = burstSpread
    )
}

/**
 * The stateless half, so a screenshot test can hold a morph at a frame the way it holds a fill.
 */
@Composable
fun MorphGlyphVisual(
    morph: GlyphMorph,
    progress: State<Float>,
    modifier: Modifier = Modifier,
    size: Dp = AppIconSize.Bar,
    tint: Color = neubrutalOnSurface(),
    enabled: Boolean = true,
    burst: State<Float>? = null,
    burstSpread: Float = 1f
) {
    // One Path reused for every subpath on every frame: a morph allocates no geometry.
    val path = remember { Path() }
    val stroke = remember {
        Stroke(width = IconStrokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
    val resolvedTint = if (enabled) tint else neubrutalMutedOnSurface()

    Canvas(modifier = modifier.size(size)) {
        val fraction = progress.value.coerceIn(0f, 1f)
        val unit = this.size.minDimension / IconGridSize

        scale(scaleX = unit, scaleY = unit, pivot = Offset.Zero) {
            for (index in 0 until morph.subpathCount) {
                val from = morph.from.subpaths[index]
                val to = morph.to.subpaths[index]
                val opacity = lerp(from.opacity, to.opacity, fraction)
                if (opacity <= 0.01f) continue
                from.writeLerpInto(target = to, t = fraction, path = path)
                drawPath(path = path, color = resolvedTint, alpha = opacity, style = stroke)
            }
        }

        burst?.let { drawBurst(it.value.coerceIn(0f, 1f), spread = burstSpread) }
    }
}

// MARK: - Previews

@Preview
@Composable
private fun MorphGlyphStatesPreview() {
    Row(
        modifier = Modifier.background(NeubrutalBg).padding(20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MorphGlyphVisual(SetikerMorphs.Eye, remember { mutableStateOf(0f) }, size = 40.dp)
        MorphGlyphVisual(SetikerMorphs.Eye, remember { mutableStateOf(0.45f) }, size = 40.dp)
        MorphGlyphVisual(SetikerMorphs.Eye, remember { mutableStateOf(1f) }, size = 40.dp)
    }
}









