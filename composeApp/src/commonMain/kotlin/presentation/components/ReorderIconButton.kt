package presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface

/**
 * Compact icon button for sticker reorder rows.
 *
 * Deliberately avoids [androidx.compose.material3.IconButton] because its 48dp minimum touch
 * target overflows a sticker grid cell (~110dp) when three buttons sit side by side, which
 * clipped the trailing arrow and pushed the drag handle off-center. A fixed 32dp box keeps the
 * row symmetric; callers give each button an equal-weight cell so the middle control is exactly
 * centered under the sticker.
 */
@Composable
fun ReorderIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.height(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(22.dp),
                tint = if (enabled) neubrutalOnSurface() else neubrutalMutedOnSurface()
            )
        }
    }
}
