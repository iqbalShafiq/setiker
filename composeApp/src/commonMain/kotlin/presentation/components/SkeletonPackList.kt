package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

/**
 * A skeleton of the pack list, shown while the first page is loading.
 *
 * A long list is better served by the shape of the content that is about to arrive than by a
 * full-screen spinner: the layout does not jump when the real rows replace the placeholders.
 */
@Composable
fun SkeletonPackList(
    modifier: Modifier = Modifier,
    rows: Int = 4
) {
    val shimmer = rememberShimmerProgress()
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(rows) {
            SkeletonPackRow(shimmer = shimmer)
        }
    }
}

@Composable
private fun SkeletonPackRow(
    shimmer: Float?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val thumbShape = RoundedCornerShape(NeubrutalSmallRadius)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .neubrutalShadow(
                offsetX = NeubrutalShadowOffset,
                offsetY = NeubrutalShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = neubrutalShadowColor()
            )
            .clip(shape)
            .neubrutalFill(color = neubrutalCardSurface(), cornerRadius = NeubrutalCardRadius)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerPlaceholder(
            modifier = Modifier
                .size(64.dp)
                .clip(thumbShape),
            progress = shimmer
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth(0.62f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp)),
                progress = shimmer
            )
            ShimmerPlaceholder(
                modifier = Modifier
                    .fillMaxWidth(0.38f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(4.dp)),
                progress = shimmer
            )
        }
    }
}
