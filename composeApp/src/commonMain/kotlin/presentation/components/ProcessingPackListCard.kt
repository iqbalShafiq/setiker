package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import presentation.home.HomeProcessingPack
import presentation.theme.NeubrutalCardRadius
import presentation.theme.NeubrutalShadowOffset
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.SetikerMotion
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadowColor
import presentation.theme.neubrutalFill

@Composable
fun ProcessingPackListCard(
    pack: HomeProcessingPack,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val border = neubrutalBorderColor()
    val surface = neubrutalCardSurface()
    val shadow = neubrutalShadowColor()
    val shape = RoundedCornerShape(NeubrutalCardRadius)
    val pressFeedback = rememberPressFeedback(
        pressedScale = SetikerMotion.PRESS_SCALE_SUBTLE,
        enabled = onClick != null,
        label = "processing_pack_card_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .pressShadow(
                feedback = pressFeedback,
                restOffset = NeubrutalShadowOffset,
                pressedOffset = NeubrutalSmallShadowOffset,
                cornerRadius = NeubrutalCardRadius,
                color = shadow
            )
            .clip(shape)
            .neubrutalFill(color = surface, cornerRadius = NeubrutalCardRadius)
            .neubrutalBorderWithGloss(
                color = border,
                cornerRadius = NeubrutalCardRadius,
                highlightColor = neubrutalGlossyHighlightColor()
            )
            .then(
                if (onClick != null) {
                    Modifier.clickableNoRipple(
                        interactionSource = pressFeedback.interactionSource,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pack.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = pack.progressLabel,
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            if (!pack.isFailed) {
                NeubrutalProgress(
                    progress = pack.progressFraction.coerceIn(0f, 1f),
                    showPercentage = true,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
            pack.failureMessage?.takeIf { pack.isFailed }?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.labelSmall,
                    color = neubrutalMutedOnSurface(),
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}
