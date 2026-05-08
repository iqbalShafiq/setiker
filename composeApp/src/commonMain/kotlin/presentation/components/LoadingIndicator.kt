package presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalBlack
import presentation.theme.neubrutalShadow
import androidx.compose.animation.core.Animatable

@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                NeubrutalBouncingDot(index = index)
            }
        }
    }
}

@Composable
private fun NeubrutalBouncingDot(index: Int) {
    val delayMs = index * 150
    val animatable = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        while (true) {
            animatable.animateTo(
                targetValue = 1.4f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = FastOutSlowInEasing)
            )
            delay(300)
        }
    }

    Box(
        modifier = Modifier
            .size(14.dp)
            .scale(animatable.value)
            .clip(CircleShape)
            .background(AccentCoral)
            .border(
                width = 2.dp,
                color = NeubrutalBlack,
                shape = CircleShape
            )
            .neubrutalShadow(
                offsetX = 2.dp,
                offsetY = 2.dp,
                cornerRadius = 7.dp,
                color = NeubrutalBlack
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
