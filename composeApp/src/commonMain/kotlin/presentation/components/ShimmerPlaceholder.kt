package presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import presentation.common.rememberReduceMotion
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalCardSurface

/**
 * The shared shimmer progress for a group of placeholders.
 *
 * A skeleton list has many placeholder blocks but should have one animation, not one per block.
 * Returns null when the platform asks for reduced motion.
 */
@Composable
fun rememberShimmerProgress(): Float? {
    if (rememberReduceMotion()) return null

    val transition = rememberInfiniteTransition(label = "shimmer")
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
        label = "shimmer_progress"
    )
    return progress
}

/**
 * A shimmering placeholder used by images and skeleton rows.
 *
 * Pass [progress] from [rememberShimmerProgress] when several placeholders make up one skeleton;
 * leave it null for a standalone placeholder. When the platform asks for reduced motion the
 * placeholder becomes a flat block instead of a moving band.
 */
@Composable
fun ShimmerPlaceholder(
    modifier: Modifier = Modifier,
    baseColor: Color = neubrutalCardSurface(),
    progress: Float? = null
) {
    val currentProgress = progress ?: rememberShimmerProgress()
    if (currentProgress == null) {
        Box(modifier = modifier.background(baseColor))
        return
    }

    Box(modifier = modifier.background(baseColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val band = size.width * 0.6f
            val travel = size.width + band
            val start = -band + currentProgress * travel
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        baseColor,
                        baseColor.copy(alpha = 0.45f),
                        baseColor
                    ),
                    start = Offset(start, 0f),
                    end = Offset(start + band, size.height)
                )
            )
        }
    }
}
