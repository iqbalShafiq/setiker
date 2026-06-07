package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import domain.model.ResolvedTextDecorationStyle
import domain.model.ResolvedTextEffectLayer
import kotlin.math.roundToInt

@Composable
fun ArchedDecorationText(
    text: String,
    resolvedStyle: ResolvedTextDecorationStyle,
    baseStyle: TextStyle,
    arcIntensity: Float,
    modifier: Modifier = Modifier,
    fitContainer: Boolean = false,
    layoutSpec: ArchedTextLayoutSpec? = null
) {
    val intensity = arcIntensity.coerceIn(-1f, 1f)
    if (text.isEmpty() || intensity == 0f) {
        StyledDecorationText(
            text = text,
            resolvedStyle = resolvedStyle,
            baseStyle = baseStyle,
            maxLines = Int.MAX_VALUE,
            modifier = modifier
        )
        return
    }

    val textMeasurer = rememberTextMeasurer()
    val textSizePx = with(LocalDensity.current) { baseStyle.fontSize.toPx() }
    val spec = layoutSpec ?: computeArchedTextLayoutSpec(
        text = text,
        baseStyle = baseStyle,
        resolvedStyle = resolvedStyle,
        arcIntensity = intensity,
        textSizePx = textSizePx,
        textMeasurer = textMeasurer
    )

    if (fitContainer) {
        ArchedTextContainerLayout(
            modifier = modifier,
            spec = spec,
            baseStyle = baseStyle
        )
    } else {
        Box(modifier = Modifier.wrapContentSize(), contentAlignment = Alignment.Center) {
            ArchedTextWrapContentLayout(
                spec = spec,
                baseStyle = baseStyle
            )
        }
    }
}

@Composable
private fun ArchedTextContainerLayout(
    spec: ArchedTextLayoutSpec,
    baseStyle: TextStyle,
    modifier: Modifier = Modifier
) {
    Layout(
        modifier = modifier,
        content = {
            spec.placements.forEach { placement ->
                ArchedCharLayer(
                    char = placement.char,
                    layer = placement.layer,
                    baseStyle = baseStyle
                )
            }
        }
    ) { measurables, constraints ->
        val containerWidth = constraints.maxWidth
        val containerHeight = constraints.maxHeight
        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints())
        }

        val contentWidth = spec.widthPx.roundToInt().coerceAtLeast(1)
        val contentHeight = spec.heightPx.roundToInt().coerceAtLeast(1)
        val offsetX = ((containerWidth - contentWidth) / 2f).coerceAtLeast(0f)
        val offsetY = ((containerHeight - contentHeight) / 2f).coerceAtLeast(0f)

        layout(containerWidth, containerHeight) {
            placeables.forEachIndexed { index, placeable ->
                val placement = spec.placements[index]
                placeable.placeRelative(
                    x = (offsetX + placement.xPx).roundToInt(),
                    y = (offsetY + placement.yPx).roundToInt()
                )
            }
        }
    }
}

@Composable
private fun ArchedTextWrapContentLayout(
    spec: ArchedTextLayoutSpec,
    baseStyle: TextStyle
) {
    Layout(
        content = {
            spec.placements.forEach { placement ->
                ArchedCharLayer(
                    char = placement.char,
                    layer = placement.layer,
                    baseStyle = baseStyle
                )
            }
        }
    ) { measurables, _ ->
        val placeables = measurables.map { measurable ->
            measurable.measure(Constraints())
        }
        val width = spec.widthPx.roundToInt().coerceAtLeast(1)
        val height = spec.heightPx.roundToInt().coerceAtLeast(1)

        layout(width, height) {
            placeables.forEachIndexed { index, placeable ->
                val placement = spec.placements[index]
                placeable.placeRelative(
                    x = placement.xPx.roundToInt(),
                    y = placement.yPx.roundToInt()
                )
            }
        }
    }
}

@Composable
private fun ArchedCharLayer(
    char: String,
    layer: ResolvedTextEffectLayer,
    baseStyle: TextStyle
) {
    Text(
        text = char,
        style = decorationLayerTextStyle(layer, baseStyle),
        textAlign = TextAlign.Center
    )
}
