package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

/**
 * The app's card surface: hard shadow, thick border, glossy bevel.
 *
 * It exists so a screen does not have to rebuild that chain or fall back to Material's `Card`.
 */
@Composable
fun NeubrutalCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = NeubrutalCardRadius,
    containerColor: Color = neubrutalCardSurface(),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = cornerRadius,
                color = neubrutalShadowColor()
            )
            .clip(RoundedCornerShape(cornerRadius))
            .neubrutalFill(color = containerColor, cornerRadius = cornerRadius)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = cornerRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            ),
        content = content
    )
}
