package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

/**
 * Pull-to-refresh with the app's own indicator.
 *
 * Material's default indicator is a circular arrow. This one is a small hard-shadow pill with the
 * brand dots; while the user pulls it follows the gesture, and while refreshing the dots bounce.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeubrutalPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        indicator = {
            val fraction = state.distanceFraction.coerceIn(0f, 1f)
            val visible = isRefreshing || fraction > 0.01f
            val travel = if (isRefreshing) 14.dp else (-44.dp + 58.dp * fraction)
            val alpha = if (visible) 1f else 0f

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .graphicsLayer {
                        translationY = travel.toPx()
                        this.alpha = alpha
                    }
                    .semantics {
                        contentDescription = if (isRefreshing) "Refreshing" else "Pull to refresh"
                    }
            ) {
                Row(
                    modifier = Modifier
                        .neubrutalShadow(
                            offsetX = NeubrutalSmallShadowOffset,
                            offsetY = NeubrutalSmallShadowOffset,
                            cornerRadius = 99.dp,
                            color = neubrutalShadowColor()
                        )
                        .clip(CircleShape)
                        .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = 99.dp)
                        .neubrutalBorderWithGloss(
                            color = neubrutalBorderColor(),
                            cornerRadius = 99.dp,
                            shape = CircleShape,
                            highlightColor = neubrutalGlossyHighlightColor()
                        )
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LoadingDots(
                        dotSize = 7.dp,
                        color = AccentCoral,
                        contentDescription = null
                    )
                }
            }
        },
        content = {
            // Move the content down as the user pulls, so the indicator sits in the gap instead
            // of on top of the first row or search field.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val fraction = if (isRefreshing) {
                            1f
                        } else {
                            state.distanceFraction.coerceIn(0f, 1f)
                        }
                        translationY = fraction * 56.dp.toPx()
                    }
            ) {
                content()
            }
        }
    )
}
