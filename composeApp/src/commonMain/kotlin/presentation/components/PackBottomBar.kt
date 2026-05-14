package presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import presentation.theme.AccentCoral
import presentation.theme.NeubrutalWhite
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackBottomBar(
    actions: @Composable RowScope.() -> Unit,
    floatingActionButton: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        modifier = modifier,
        containerColor = neubrutalCardSurface(),
        contentColor = neubrutalOnSurface(),
        tonalElevation = 0.dp,
        actions = actions,
        floatingActionButton = floatingActionButton
    )
}

@Composable
fun PackBottomBarIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val tint = neubrutalOnSurface()
    IconButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.4f)
        )
    }
}

@Composable
fun PackBottomBarFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val shape = RoundedCornerShape(16.dp)
    val border = neubrutalBorderColor()
    FloatingActionButton(
        onClick = { if (enabled) onClick() },
        modifier = modifier.border(2.dp, border, shape),
        containerColor = AccentCoral,
        contentColor = NeubrutalWhite,
        shape = shape,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) NeubrutalWhite else NeubrutalWhite.copy(alpha = 0.5f)
        )
    }
}

// MARK: - Previews
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PackBottomBarPreview() {
    MaterialTheme {
        PackBottomBar(
            actions = {
                PackBottomBarIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = "Edit",
                    onClick = {}
                )
            },
            floatingActionButton = {
                PackBottomBarFab(
                    icon = Icons.Default.Add,
                    contentDescription = "Add",
                    onClick = {}
                )
            }
        )
    }
}

@Preview
@Composable
private fun PackBottomBarIconButtonPreview() {
    MaterialTheme {
        PackBottomBarIconButton(
            icon = Icons.Default.Edit,
            contentDescription = "Edit",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun PackBottomBarFabPreview() {
    MaterialTheme {
        PackBottomBarFab(
            icon = Icons.Default.Add,
            contentDescription = "Add",
            onClick = {}
        )
    }
}
