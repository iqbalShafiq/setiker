package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Sync status indicator badge that shows the current sync state of an item.
 * Uses Neubrutal design with bold borders and solid colors.
 */
@Composable
fun SyncStatusIndicator(
    syncState: String,
    modifier: Modifier = Modifier
) {
    val (icon, color, contentColor) = when (syncState) {
        "SYNCED" -> Triple(Icons.Default.CloudDone, Color(0xFF4CAF50), Color.White)
        "PENDING" -> Triple(Icons.Default.CloudQueue, Color(0xFFFF9800), Color.White)
        "FAILED" -> Triple(Icons.Default.CloudOff, Color(0xFFF44336), Color.White)
        else -> Triple(Icons.Default.Cloud, Color(0xFF9E9E9E), Color.White)
    }

    Box(
        modifier = modifier
            .size(28.dp)
            .shadow(4.dp, CircleShape)
            .background(color, CircleShape)
            .border(2.dp, Color.Black, CircleShape),
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
