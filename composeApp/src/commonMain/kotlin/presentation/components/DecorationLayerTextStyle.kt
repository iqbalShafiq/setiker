package presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import domain.model.ResolvedTextEffectLayer

fun decorationLayerTextStyle(
    layer: ResolvedTextEffectLayer,
    baseStyle: TextStyle
): TextStyle {
    val layerColor = Color(layer.colorArgb.toInt())
    return when {
        layer.isFill && layer.gradientStartArgb != null && layer.gradientEndArgb != null -> {
            baseStyle.copy(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(layer.gradientStartArgb.toInt()),
                        Color(layer.gradientEndArgb.toInt())
                    )
                )
            )
        }
        !layer.isFill && layer.strokeWidthPx > 0f -> baseStyle.copy(
            color = layerColor,
            drawStyle = Stroke(
                width = layer.strokeWidthPx,
                join = StrokeJoin.Round,
                cap = StrokeCap.Round
            )
        )
        layer.shadowBlurPx > 0f -> baseStyle.copy(
            color = layerColor,
            shadow = Shadow(
                color = layerColor.copy(alpha = layerColor.alpha * 0.85f),
                offset = Offset(0f, layer.shadowOffsetYPx),
                blurRadius = layer.shadowBlurPx
            )
        )
        else -> baseStyle.copy(color = layerColor)
    }
}
