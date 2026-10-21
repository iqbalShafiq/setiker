package presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.theme.AccentCoral
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.page_indicator_cd

/**
 * The page dots.
 *
 * The active dot used to swap colour instantly, which made the indicator jump ahead of the page
 * that was actually on screen. The dot now animates (scale + colour) and the inactive dots keep
 * enough contrast to be perceivable (they used to sit at 18 % opacity on a near-white page, about
 * 1.3:1, far below the 3:1 that non-text UI has to reach).
 */
@Composable
fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    onPageClick: ((Int) -> Unit)? = null
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(pageCount) { index ->
            val selected = index == currentPage
            val dotColor by animateColorAsState(
                targetValue = if (selected) {
                    AccentCoral
                } else {
                    neubrutalOnSurface().copy(alpha = 0.45f)
                },
                animationSpec = SetikerMotion.standard(SetikerMotion.DURATION_BASE_MS),
                label = "page_dot_color"
            )
            val dotScale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.72f,
                animationSpec = SetikerMotion.PressSpring,
                label = "page_dot_scale"
            )
            val dotModifier = Modifier
                .size(12.dp)
                .graphicsLayer {
                    scaleX = dotScale
                    scaleY = dotScale
                }
                .clip(CircleShape)
                .background(dotColor)
                .border(
                    width = 1.dp,
                    color = if (selected) neubrutalBorderColor() else MaterialTheme.colorScheme.outline.copy(alpha = 0.42f),
                    shape = CircleShape
                )

            val pageDescription = stringResource(
                Res.string.page_indicator_cd,
                index + 1,
                pageCount
            )

            Box(
                modifier = Modifier
                    .size(32.dp)
                    // Every dot says which page it is, whether or not it can be tapped. A screen
                    // reader has to be able to tell where it is, and "selected" is what tells it
                    // that this is the page on screen.
                    .semantics {
                        contentDescription = pageDescription
                        this.selected = selected
                    }
                    .then(
                        if (onPageClick != null) {
                            // Each dot is a real target here (onPageClick), so it needs a spoken
                            // label; without it TalkBack announces a nameless button.
                            Modifier.clickable { onPageClick(index) }
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = dotModifier)
            }
        }
    }
}
