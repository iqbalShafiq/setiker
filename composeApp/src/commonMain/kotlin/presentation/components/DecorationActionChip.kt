package presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface

/**
 * Neubrutal pill used for "add text / emoji / image" actions on editor surfaces.
 */
@Composable
fun DecorationActionChip(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(neubrutalCardSurface())
            .border(2.dp, neubrutalBorderColor(), CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = AccentCoral)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalOnSurface()
        )
    }
}

// MARK: - Previews
@Preview
@Composable
private fun DecorationActionChipPreview() {
    MaterialTheme {
        DecorationActionChip(
            icon = Icons.Default.Add,
            label = "Add Text",
            onClick = {}
        )
    }
}
