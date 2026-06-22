package presentation.auth

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import presentation.components.AppTopBar
import presentation.components.InteractionBlockedBox
import presentation.components.PackBottomBar
import presentation.components.PackBottomBarFab
import presentation.components.PackBottomBarIconButton
import presentation.theme.neubrutalScreenBackground

@Composable
fun AuthFormScaffold(
    title: String,
    isLoading: Boolean,
    loadingStatusText: String,
    onPrimaryAction: () -> Unit,
    primaryFabIcon: ImageVector,
    primaryFabContentDescription: String,
    onSecondaryAction: () -> Unit,
    secondaryActionIcon: ImageVector,
    secondaryActionContentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = title,
                onBackClick = null
            )
        },
        bottomBar = {
            PackBottomBar(
                actionStatusText = if (isLoading) loadingStatusText else null,
                actions = {
                    PackBottomBarIconButton(
                        icon = secondaryActionIcon,
                        contentDescription = secondaryActionContentDescription,
                        onClick = onSecondaryAction,
                        enabled = !isLoading
                    )
                },
                floatingActionButton = {
                    PackBottomBarFab(
                        icon = primaryFabIcon,
                        contentDescription = primaryFabContentDescription,
                        onClick = onPrimaryAction,
                        enabled = !isLoading,
                        isLoading = isLoading
                    )
                }
            )
        },
        containerColor = neubrutalScreenBackground()
    ) { padding ->
        InteractionBlockedBox(
            blocked = isLoading,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AuthFormScrollColumn(
                modifier = Modifier.fillMaxSize(),
                content = content
            )
        }
    }
}
