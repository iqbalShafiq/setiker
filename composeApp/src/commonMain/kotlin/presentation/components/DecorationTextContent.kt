package presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import domain.model.DecorationRenderSpec
import domain.model.TextDecoration
import domain.model.TextDecorationLayout

@Composable
fun DecorationTextContent(
    decoration: TextDecoration,
    minDim: Float,
    scale: Float,
    modifier: Modifier = Modifier,
    fitContainer: Boolean = false,
    boxMetrics: TextDecorationBoxMetrics? = null
) {
    val metrics = boxMetrics ?: rememberTextDecorationBoxMetrics(
        decoration = decoration,
        minDim = minDim,
        canvasWidthPx = minDim,
        scale = scale
    )

    when (decoration.layout) {
        TextDecorationLayout.Arched -> ArchedDecorationText(
            text = decoration.text,
            resolvedStyle = metrics.resolvedStyle,
            baseStyle = metrics.baseStyle,
            arcIntensity = decoration.arcIntensity,
            modifier = modifier,
            fitContainer = fitContainer,
            layoutSpec = metrics.archedLayoutSpec
        )
        else -> StyledDecorationText(
            text = decoration.text,
            resolvedStyle = metrics.resolvedStyle,
            baseStyle = metrics.baseStyle,
            maxLines = Int.MAX_VALUE,
            modifier = modifier
        )
    }
}
