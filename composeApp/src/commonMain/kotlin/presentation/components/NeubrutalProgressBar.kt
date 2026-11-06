package presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import presentation.common.rememberReduceMotion
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

private val ProgressBarHeight = 18.dp
private val ProgressBarShape = RoundedCornerShape(NeubrutalSmallRadius)

/**
 * A neubrutal progress bar. Pass a value in `0f..1f`, or `null` for an indeterminate job.
 *
 * It replaces the Material `LinearProgressIndicator`, whose rounded track and built-in motion
 * were the only Material-looking surfaces left on the AI-job and processing cards.
 */
@Composable
fun NeubrutalProgressBar(
    progress: Float?,
    modifier: Modifier = Modifier,
    height: Dp = ProgressBarHeight,
    fillColor: Color = AccentCoral
) {
    val animatedProgress = if (progress == null) {
        0f
    } else {
        val value by animateFloatAsState(
            targetValue = progress.coerceIn(0f, 1f),
            animationSpec = SetikerMotion.standard(),
            label = "neubrutal_progress"
        )
        value
    }

    val semanticsModifier = if (progress == null) {
        modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate }
    } else {
        modifier.semantics {
            progressBarRangeInfo = ProgressBarRangeInfo(
                current = animatedProgress,
                range = 0f..1f
            )
        }
    }

    Box(
        modifier = semanticsModifier
            .fillMaxWidth()
            .height(height)
            .neubrutalShadow(
                offsetX = NeubrutalSmallShadowOffset,
                offsetY = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalSmallRadius,
                color = neubrutalShadowColor()
            )
            .clip(ProgressBarShape)
            .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = NeubrutalSmallRadius)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                width = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalSmallRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(2.dp)
    ) {
        if (progress == null) {
            IndeterminateProgressFill(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                color = fillColor
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(ProgressBarShape)
                    .neubrutalFill(color = fillColor, cornerRadius = NeubrutalSmallRadius)
            )
        }
    }
}

/**
 * A progress bar with a label and an animated percentage.
 *
 * The percentage is derived from the same animated value as the fill, so the number counts up
 * instead of jumping from one server update to the next.
 */
@Composable
fun NeubrutalProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    label: String? = null,
    showPercentage: Boolean = true
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = SetikerMotion.standard(),
        label = "neubrutal_progress_value"
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (label != null || showPercentage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = neubrutalMutedOnSurface(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (showPercentage) {
                    Text(
                        text = "${(animated * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = neubrutalOnSurface()
                    )
                }
            }
        }
        NeubrutalProgressBar(progress = animated)
    }
}

@Composable
private fun IndeterminateProgressFill(
    modifier: Modifier = Modifier,
    color: Color
) {
    val reduceMotion = rememberReduceMotion()
    if (reduceMotion) {
        Canvas(modifier = modifier) {
            drawRect(color = color.copy(alpha = 0.45f))
        }
        return
    }

    val transition = rememberInfiniteTransition(label = "indeterminate_progress")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SetikerMotion.DURATION_SHIMMER_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "indeterminate_progress_band"
    )

    Canvas(modifier = modifier) {
        val band = size.width * 0.32f
        val start = -band + progress * (size.width + band)
        drawRect(
            color = color,
            topLeft = Offset(start, 0f),
            size = androidx.compose.ui.geometry.Size(band, size.height)
        )
    }
}
