package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import presentation.common.rememberReduceMotion
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalThinBorderWidth
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    label: String? = null,
    illustration: AppIllustration? = null
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (illustration != null) {
                AppIllustrationImage(
                    illustration = illustration,
                    modifier = Modifier.height(180.dp)
                )
                Spacer(modifier = Modifier.height(18.dp))
            }
            LoadingDots()
            if (!label.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalMutedOnSurface()
                )
            }
        }
    }
}

/**
 * The three-dot brand loader.
 *
 * It is shared by the full-screen loading state and by inline places (a paginating list, a
 * button that is working) so the app has one loading language. When the platform asks for reduced
 * motion the dots stay still instead of looping forever.
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 14.dp,
    color: Color = AccentCoral,
    contentDescription: String? = null
) {
    val reduceMotion = rememberReduceMotion()
    val semanticsModifier = if (contentDescription == null) {
        modifier
    } else {
        modifier.semantics { this.contentDescription = contentDescription }
    }

    Row(
        modifier = semanticsModifier,
        horizontalArrangement = Arrangement.spacedBy(if (dotSize >= 10.dp) 10.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            LoadingDot(
                index = index,
                dotSize = dotSize,
                color = color,
                animated = !reduceMotion
            )
        }
    }
}

@Composable
private fun LoadingDot(
    index: Int,
    dotSize: Dp,
    color: Color,
    animated: Boolean
) {
    val delayMs = index * SetikerMotion.LOADER_STAGGER_MS
    val animatable = remember { Animatable(1f) }
    val border = neubrutalBorderColor()
    val shadow = neubrutalShadowColor()

    LaunchedEffect(animated) {
        if (!animated) {
            animatable.snapTo(1f)
            return@LaunchedEffect
        }
        delay(delayMs.milliseconds)
        while (true) {
            animatable.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(SetikerMotion.DURATION_LOADER_MS)
            )
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(SetikerMotion.DURATION_LOADER_MS)
            )
            delay(SetikerMotion.LOADER_HOLD_MS.milliseconds)
        }
    }

    Box(
        modifier = Modifier
            .size(dotSize)
            // Read the animated value in the draw phase. Reading it in the composition made the
            // whole loading indicator recompose on every frame of the animation.
            .graphicsLayer {
                scaleX = animatable.value
                scaleY = animatable.value
            }
            .neubrutalShadow(
                offsetX = if (dotSize >= 10.dp) NeubrutalSmallShadowOffset else 1.dp,
                offsetY = if (dotSize >= 10.dp) NeubrutalSmallShadowOffset else 1.dp,
                cornerRadius = NeubrutalSmallRadius,
                color = shadow
            )
            .clip(CircleShape)
            .neubrutalFill(color = color, cornerRadius = 99.dp)
            .neubrutalBorderWithGloss(
                color = border,
                width = if (dotSize >= 10.dp) NeubrutalThinBorderWidth * 1.6f else NeubrutalThinBorderWidth,
                cornerRadius = NeubrutalSmallRadius,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            )
    )
}

// MARK: - Preview

@Preview
@Composable
private fun LoadingIndicatorPreview() {
    MaterialTheme {
        LoadingIndicator()
    }
}
