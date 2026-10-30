package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.theme.NeubrutalButtonRadius
import presentation.theme.NeubrutalDark
import presentation.theme.NeubrutalSmallRadius
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = NeubrutalWhite
) {
    val borderColor = neubrutalBorderColor()
    val pressFeedback = rememberPressFeedback(
        pressedScale = 0.98f,
        label = "menu_item_scale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(pressFeedback)
            .clickable(
                interactionSource = pressFeedback.interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .neubrutalShadow(
                        offsetX = NeubrutalSmallShadowOffset,
                        offsetY = NeubrutalSmallShadowOffset,
                        cornerRadius = NeubrutalSmallRadius,
                        color = neubrutalShadowColor()
                    )
                    .clip(RoundedCornerShape(NeubrutalSmallRadius))
                    .background(iconBackgroundColor)
                    .neubrutalBorderWithGloss(
                        color = borderColor,
                        cornerRadius = NeubrutalSmallRadius,
                        highlightColor = neubrutalGlossyHighlightColor()
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = NeubrutalDark
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = neubrutalOnSurface()
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = neubrutalMutedOnSurface()
        )
    }
}

// MARK: - Previews

@Preview
@Composable
private fun ProfileMenuItemPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(NeubrutalWhite)
                .neubrutalBorderWithGloss(
                    color = neubrutalBorderColor(),
                    cornerRadius = NeubrutalButtonRadius,
                    highlightColor = neubrutalGlossyHighlightColor()
                )
                .neubrutalShadow(
                    cornerRadius = NeubrutalButtonRadius,
                    color = neubrutalShadowColor()
                )
                .clip(RoundedCornerShape(NeubrutalButtonRadius))
        ) {
            ProfileMenuItem(
                icon = Icons.Filled.Image,
                label = "My Stickers",
                onClick = {}
            )
            ProfileMenuItem(
                icon = Icons.Filled.Favorite,
                label = "Liked Stickers",
                onClick = {}
            )
        }
    }
}
