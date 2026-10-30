package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.theme.ErrorRed
import presentation.theme.NeubrutalBlack
import presentation.theme.NeubrutalGray
import presentation.theme.NeubrutalSmallShadowOffset
import presentation.theme.NeubrutalWhite
import presentation.theme.SuccessGreen
import presentation.theme.WarningYellow
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalBorderWithGloss
import presentation.theme.neubrutalGlossyHighlightColor
import presentation.theme.neubrutalShadow
import presentation.theme.neubrutalShadowColor

/**
 * Sync status indicator badge that shows the current sync state of an item.
 * Uses Neubrutal design with bold borders, hard shadow and solid colors.
 */
@Composable
fun SyncStatusIndicator(
    syncState: String,
    modifier: Modifier = Modifier
) {
    // Palette colours instead of one-off Material greys, and a border that follows the theme: the
    // previous hard-coded black ring disappeared against the dark surfaces.
    val (icon, color, contentColor) = when (syncState) {
        "SYNCED" -> Triple(Icons.Default.CloudDone, SuccessGreen, NeubrutalBlack)
        "PENDING" -> Triple(Icons.Default.CloudQueue, WarningYellow, NeubrutalBlack)
        "FAILED" -> Triple(Icons.Default.CloudOff, ErrorRed, NeubrutalBlack)
        else -> Triple(Icons.Default.Cloud, NeubrutalGray, NeubrutalWhite)
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .neubrutalShadow(
                offsetX = NeubrutalSmallShadowOffset,
                offsetY = NeubrutalSmallShadowOffset,
                cornerRadius = 14.dp,
                color = neubrutalShadowColor()
            )
            .clip(CircleShape)
            .background(color)
            .neubrutalBorderWithGloss(
                color = neubrutalBorderColor(),
                cornerRadius = 14.dp,
                shape = CircleShape,
                highlightColor = neubrutalGlossyHighlightColor(onFilledSurface = true)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Sync status: $syncState",
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
    }
}

// MARK: - Previews
@Preview
@Composable
private fun SyncStatusIndicatorSyncedPreview() {
    MaterialTheme {
        SyncStatusIndicator(syncState = "SYNCED")
    }
}

@Preview
@Composable
private fun SyncStatusIndicatorPendingPreview() {
    MaterialTheme {
        SyncStatusIndicator(syncState = "PENDING")
    }
}

@Preview
@Composable
private fun SyncStatusIndicatorFailedPreview() {
    MaterialTheme {
        SyncStatusIndicator(syncState = "FAILED")
    }
}

@Preview
@Composable
private fun SyncStatusIndicatorUnknownPreview() {
    MaterialTheme {
        SyncStatusIndicator(syncState = "UNKNOWN")
    }
}
