package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

@Composable
fun CheckerboardBackground(
    modifier: Modifier = Modifier,
    checkerColor: Color = Color.LightGray,
    squareSize: Float = 20f
) {
    Box(
        modifier = modifier
            .background(Color.White)
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
