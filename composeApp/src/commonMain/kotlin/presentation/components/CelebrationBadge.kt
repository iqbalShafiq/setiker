package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * A short celebration: a check badge that pops in with a spring.
 *
 * Saving a sticker is the moment the app has been building towards, and it used to end with the
 * screen simply changing. The badge is the one place in the app where a **bouncy** spring is
 * allowed: it happens once, on a rare, high-emotion action, and it never blocks interaction.
 */
@Composable
fun CelebrationBadge(
    visible: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = AccentCoral,
    contentDescription: String? = null,
    onFinished: (() -> Unit)? = null
) {
    val scaleAnim = remember { Animatable(0f) }
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (!visible) {
            scaleAnim.snapTo(0f)
            alphaAnim.snapTo(0f)
            return@LaunchedEffect
        }
        alphaAnim.animateTo(1f, tween(durationMillis = SetikerMotion.DURATION_FAST_MS))
        scaleAnim.animateTo(1f, SetikerMotion.DelightSpring)
        onFinished?.invoke()
    }

    if (alphaAnim.value <= 0f && !visible) return

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scaleAnim.value
            scaleY = scaleAnim.value
            alpha = alphaAnim.value
        }
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .neubrutalShadow(
                    offsetX = NeubrutalShadowOffset,
                    offsetY = NeubrutalShadowOffset,
                    cornerRadius = NeubrutalCardRadius,
                    color = neubrutalShadowColor()
                )
                .clip(RoundedCornerShape(NeubrutalCardRadius))
                .background(tint)
                .border(
                    width = NeubrutalBorderWidth,
                    color = neubrutalBorderColor(),
                    shape = RoundedCornerShape(NeubrutalCardRadius)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = contentDescription,
                modifier = Modifier.size(30.dp),
                tint = Color.White
            )
        }
    }
}
