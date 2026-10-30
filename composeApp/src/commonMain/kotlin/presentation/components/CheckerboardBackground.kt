package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalCheckerSquare

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    checkerColor: Color = neubrutalCheckerSquare(),
    squareSize: Float = 20f
) {
    Box(
        modifier = modifier
            .background(neubrutalCardSurface())
            .drawBehind {
                val numSquaresX = (size.width / squareSize).toInt() + 1
                val numSquaresY = (size.height / squareSize).toInt() + 1

                for (x in 0 until numSquaresX) {
                    for (y in 0 until numSquaresY) {
                        if ((x + y) % 2 == 0) {
                            drawRect(
                                color = checkerColor,
                                topLeft = Offset(x * squareSize, y * squareSize),
                                size = Size(squareSize, squareSize)
                            )
                        }
                    }
                }
            }
    )
}

// MARK: - Previews
@Preview
@Composable
private fun CheckerboardBackgroundPreview() {
    MaterialTheme {
        CheckerboardBackground(modifier = Modifier.fillMaxSize())
    }
}
