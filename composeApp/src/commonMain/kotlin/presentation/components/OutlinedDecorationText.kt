package presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt

@Composable
fun OutlinedDecorationText(
    text: String,
    style: TextStyle,
    borderColor: Color,
    borderWidthPx: Float,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val strokePx = borderWidthPx.coerceAtLeast(0f)
    if (strokePx <= 0f) {
        Text(
            text = text,
            style = style,
            modifier = modifier,
            textAlign = TextAlign.Center,
            maxLines = maxLines
        )
        return
    }

    val radiusPx = strokePx.roundToInt().coerceAtLeast(1)
    val offsets = listOf(
        Offset(-radiusPx.toFloat(), 0f),
        Offset(radiusPx.toFloat(), 0f),
        Offset(0f, -radiusPx.toFloat()),
        Offset(0f, radiusPx.toFloat()),
        Offset(-radiusPx.toFloat(), -radiusPx.toFloat()),
        Offset(radiusPx.toFloat(), -radiusPx.toFloat()),
        Offset(-radiusPx.toFloat(), radiusPx.toFloat()),
        Offset(radiusPx.toFloat(), radiusPx.toFloat())
    )

    Box(modifier = modifier) {
        offsets.forEach { shift ->
            Text(
                text = text,
                style = style.copy(color = borderColor),
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(shift.x.roundToInt(), shift.y.roundToInt()) },
                textAlign = TextAlign.Center,
                maxLines = maxLines
            )
        }
        Text(
            text = text,
            style = style,
            modifier = Modifier.fillMaxSize(),
            textAlign = TextAlign.Center,
            maxLines = maxLines
        )
    }
}
