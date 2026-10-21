package presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.rememberAsyncImagePainter
import coil3.compose.rememberConstraintsSizeResolver
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Scale
import presentation.theme.NeubrutalBg
import presentation.theme.neubrutalCardSurface

/**
 * An image that shows a shimmer while it loads, and decodes at the size it is displayed.
 *
 * Two problems it solves:
 *
 * **The loading state.** Without a placeholder, a sticker cover renders as a blank square until the
 * bitmap arrives, which reads as "broken" rather than "loading". Coil 3 has no colour painter, so
 * the placeholder is built here on top of Coil's painter state: the shimmer is driven by Compose
 * (so it actually animates) and Coil only paints the pixels once they are ready.
 *
 * **The decode size.** `rememberAsyncImagePainter` loads an image at its *original* dimensions
 * unless a size resolver is supplied, and Coil's default precision keeps the source size when the
 * source is larger than the view (its downsample is a power of two, so 512 px into a 302 px cell
 * is not downsampled at all). A pack of sixteen stickers therefore held 63 MB of graphics memory
 * for a grid that only shows 302 px squares. Passing the constraints resolver together with
 * [Precision.EXACT] decodes at the size on screen.
 */
@Composable
fun NeubrutalAsyncImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    precision: Precision = Precision.EXACT,
    placeholderColor: Color = neubrutalCardSurface(),
    errorColor: Color = NeubrutalBg
) {
    val sizeResolver = rememberConstraintsSizeResolver()
    val context = LocalPlatformContext.current
    val request = remember(model, context, sizeResolver, contentScale, precision) {
        ImageRequest.Builder(context)
            .data(model)
            .size(sizeResolver)
            .precision(precision)
            .scale(if (contentScale == ContentScale.Crop) Scale.FILL else Scale.FIT)
            .build()
    }
    val painter = rememberAsyncImagePainter(
        model = request,
        error = SolidColorPainter(errorColor)
    )
    val state by painter.state.collectAsState()
    val isLoading = state is coil3.compose.AsyncImagePainter.State.Empty ||
        state is coil3.compose.AsyncImagePainter.State.Loading

    Box(modifier = modifier) {
        if (isLoading) {
            Shimmer(base = placeholderColor)
        }
        Image(
            painter = painter,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier
                .fillMaxSize()
                .then(sizeResolver)
        )
    }
}

/**
 * The loading shimmer.
 *
 * The infinite animation only exists while an image is actually loading, so a screen full of
 * already-loaded images does not keep a per-frame animation alive.
 */
@Composable
private fun Shimmer(
    base: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "image_shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "image_shimmer_progress"
    )

    Box(modifier = modifier.background(base)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val band = size.width * 0.6f
            val travel = size.width + band
            val start = -band + progress * travel
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        base,
                        base.copy(alpha = 0.45f),
                        base
                    ),
                    start = Offset(start, 0f),
                    end = Offset(start + band, size.height)
                )
            )
        }
    }
}
