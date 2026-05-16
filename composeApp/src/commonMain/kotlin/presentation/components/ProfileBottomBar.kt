package presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.back_content_description
import setiker.composeapp.generated.resources.logout
import setiker.composeapp.generated.resources.settings

@Composable
fun ProfileBottomBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    PackBottomBar(
        actions = {
            PackBottomBarIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(Res.string.back_content_description),
                onClick = onBackClick
            )
            PackBottomBarIconButton(
                icon = Icons.Default.Settings,
                contentDescription = stringResource(Res.string.settings),
                onClick = onSettingsClick
            )
        },
        floatingActionButton = {
            PackBottomBarFab(
                icon = Icons.Default.Logout,
                contentDescription = stringResource(Res.string.logout),
                onClick = onLogoutClick
            )
        }
    )
}
