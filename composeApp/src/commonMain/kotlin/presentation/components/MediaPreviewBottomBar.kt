package presentation.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.pause_preview
import setiker.composeapp.generated.resources.play_preview

/**
 * Reusable bottom app bar for screens that play a media preview and need a
 * primary "continue/apply/save" action plus a cancel.
 *
 * Mirrors the look used by `AnimatedEditorScreen`:
 *   [Cancel ✕] [Play/Pause] [extra icons]      [Primary FAB]
 *
 * Built on top of [PackBottomBar] so spacing, container colour, and elevation
 * stay consistent app-wide and dark mode is handled for free.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPreviewBottomBar(
    primaryIcon: ImageVector,
    primaryDescription: String,
    onPrimary: () -> Unit,
    onCancel: () -> Unit,
    primaryEnabled: Boolean = true,
    cancelEnabled: Boolean = true,
    isPlaying: Boolean = false,
    onTogglePlay: (() -> Unit)? = null,
    playEnabled: Boolean = true,
    cancelDescription: String? = null,
    extraActions: @Composable (RowScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val resolvedCancelDescription = cancelDescription ?: stringResource(Res.string.cancel)
    PackBottomBar(
        modifier = modifier,
        actions = {
            PackBottomBarIconButton(
                icon = Icons.Filled.Close,
                contentDescription = resolvedCancelDescription,
                onClick = onCancel,
                enabled = cancelEnabled
            )
            if (onTogglePlay != null) {
                PackBottomBarIconButton(
                    icon = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(
                        if (isPlaying) Res.string.pause_preview else Res.string.play_preview
                    ),
                    onClick = onTogglePlay,
                    enabled = playEnabled
                )
            }
            extraActions?.invoke(this)
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = primaryIcon,
                contentDescription = primaryDescription,
                onClick = onPrimary,
                enabled = primaryEnabled
            )
        }
    )
}
