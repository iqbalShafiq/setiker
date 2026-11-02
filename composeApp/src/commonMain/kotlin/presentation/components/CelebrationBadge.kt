package presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBorderWidth
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface

/**
 * A short celebration: a status pill that pops in with a spring.
 *
 * Saving a sticker is the moment the app has been building towards, and it used to end with the
 * screen simply changing. The badge is the one place in the app where a **bouncy** spring is
 * allowed: it happens once, on a rare, high-emotion action, and it never blocks interaction.
 *
 * It is deliberately a labelled pill rather than a filled square: the square read as a second
 * save button next to the real one in the bottom bar. [contentDescription] is the visible label.
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
        },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(neubrutalCardSurface())
                .border(
                    width = NeubrutalBorderWidth,
                    color = tint,
                    shape = CircleShape
                )
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = tint
            )
            if (!contentDescription.isNullOrBlank()) {
                Text(
                    text = contentDescription,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
            }
        }
    }
}
