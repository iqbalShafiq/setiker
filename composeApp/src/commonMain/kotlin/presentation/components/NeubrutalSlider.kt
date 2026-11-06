package presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

/**
 * The app's slider: a hard-shadow track and a chunky, bordered thumb.
 *
 * The gesture, keyboard support and semantics still come from Material's [Slider]; only the
 * thumb and the track are replaced. That keeps the control accessible while removing the last
 * generic Material control from the editors.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NeubrutalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }

    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        interactionSource = interactionSource,
        thumb = {
            NeubrutalSliderThumb(
                interactionSource = interactionSource,
                enabled = enabled
            )
        },
        track = { state ->
            NeubrutalSliderTrack(
                fraction = state.coercedValueAsFraction,
                enabled = enabled
            )
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NeubrutalRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null
) {
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }

    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = {
            NeubrutalSliderThumb(
                interactionSource = startInteractionSource,
                enabled = enabled
            )
        },
        endThumb = {
            NeubrutalSliderThumb(
                interactionSource = endInteractionSource,
                enabled = enabled
            )
        },
        track = {
            NeubrutalRangeSliderTrack(
                value = value,
                valueRange = valueRange,
                enabled = enabled
            )
        }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun NeubrutalSliderThumb(
    interactionSource: MutableInteractionSource,
    enabled: Boolean
) {
    val pressed by interactionSource.collectIsPressedAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val scale by animateFloatAsState(
        targetValue = if (enabled && (pressed || dragged)) 0.86f else 1f,
        animationSpec = SetikerMotion.PressSpring,
        label = "slider_thumb_scale"
    )
    val border = neubrutalBorderColor()

    Box(
        modifier = Modifier
            .size(28.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .neubrutalShadow(
                offsetX = NeubrutalSmallShadowOffset,
                offsetY = NeubrutalSmallShadowOffset,
                cornerRadius = 99.dp,
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .neubrutalFill(color = AccentCoral.copy(alpha = if (enabled) 1f else 0.4f), cornerRadius = 99.dp)
            .border(
                width = NeubrutalBorderWidth,
                color = border.copy(alpha = if (enabled) 1f else 0.4f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .neubrutalFill(color = NeubrutalWhite.copy(alpha = if (enabled) 1f else 0.4f), cornerRadius = 99.dp)
        )
    }
}

@Composable
private fun NeubrutalSliderTrack(
    fraction: Float,
    enabled: Boolean
) {
    val surface = neubrutalCardSurface()
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val radius = size.height / 2f
        val offset = NeubrutalSmallShadowOffset.toPx()
        val alpha = if (enabled) 1f else 0.4f

        drawRoundRect(
            color = shadow.copy(alpha = alpha),
            topLeft = Offset(offset, offset),
            size = Size(size.width - offset, size.height - offset),
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = surface,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        if (fraction > 0f) {
            drawRoundRect(
                color = AccentCoral.copy(alpha = alpha),
                size = Size(size.width * fraction.coerceIn(0f, 1f), size.height),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
        drawRoundRect(
            color = border.copy(alpha = alpha),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = NeubrutalBorderWidth.toPx())
        )
    }
}

@Composable
private fun NeubrutalRangeSliderTrack(
    value: ClosedFloatingPointRange<Float>,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean
) {
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val start = ((value.start - valueRange.start) / span).coerceIn(0f, 1f)
    val end = ((value.endInclusive - valueRange.start) / span).coerceIn(0f, 1f)

    val surface = neubrutalCardSurface()
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
    ) {
        val radius = size.height / 2f
        val offset = NeubrutalSmallShadowOffset.toPx()
        val alpha = if (enabled) 1f else 0.4f

        drawRoundRect(
            color = shadow.copy(alpha = alpha),
            topLeft = Offset(offset, offset),
            size = Size(size.width - offset, size.height - offset),
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = surface,
            size = size,
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = AccentCoral.copy(alpha = alpha),
            topLeft = Offset(size.width * start, 0f),
            size = Size(size.width * (end - start).coerceAtLeast(0f), size.height),
            cornerRadius = CornerRadius(radius, radius)
        )
        drawRoundRect(
            color = border.copy(alpha = alpha),
            size = size,
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = NeubrutalBorderWidth.toPx())
        )
    }
}
