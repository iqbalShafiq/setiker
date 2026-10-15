package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import presentation.components.AppPrimaryButton
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.settings_apple_connected
import setiker.composeapp.generated.resources.settings_apple_not_connected
import setiker.composeapp.generated.resources.settings_connected_accounts
import setiker.composeapp.generated.resources.settings_google_connected
import setiker.composeapp.generated.resources.settings_google_not_connected
import setiker.composeapp.generated.resources.settings_hide_my_email_hint
import setiker.composeapp.generated.resources.settings_link_apple
import setiker.composeapp.generated.resources.settings_link_google
import setiker.composeapp.generated.resources.settings_password_status_missing
import setiker.composeapp.generated.resources.settings_password_status_set
import setiker.composeapp.generated.resources.settings_set_password
import setiker.composeapp.generated.resources.settings_unlink_apple
import setiker.composeapp.generated.resources.settings_unlink_google

data class ConnectedAccountsUiState(
    val hasPassword: Boolean = true,
    val hasGoogle: Boolean = false,
    val hasApple: Boolean = false,
    val googleAvailable: Boolean = false,
    val appleAvailable: Boolean = false,
    val isBusy: Boolean = false
)

@Composable
fun ConnectedAccountsSection(
    state: ConnectedAccountsUiState,
    onLinkGoogle: () -> Unit,
    onUnlinkGoogle: () -> Unit,
    onLinkApple: () -> Unit = {},
    onUnlinkApple: () -> Unit = {},
    onSetPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.settings_connected_accounts),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = neubrutalOnSurface()
        )
        Text(
            text = if (state.hasPassword) {
                stringResource(Res.string.settings_password_status_set)
            } else {
                stringResource(Res.string.settings_password_status_missing)
            },
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalMutedOnSurface()
        )
        Text(
            text = if (state.hasGoogle) {
                stringResource(Res.string.settings_google_connected)
            } else {
                stringResource(Res.string.settings_google_not_connected)
            },
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalMutedOnSurface()
        )
        if (state.appleAvailable || state.hasApple) {
            Text(
                text = if (state.hasApple) {
                    stringResource(Res.string.settings_apple_connected)
                } else {
                    stringResource(Res.string.settings_apple_not_connected)
                },
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }
        if (state.googleAvailable) {
            if (state.hasGoogle) {
                AppPrimaryButton(
                    text = stringResource(Res.string.settings_unlink_google),
                    onClick = onUnlinkGoogle,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy && (state.hasPassword || state.hasApple)
                )
            } else {
                AppPrimaryButton(
                    text = stringResource(Res.string.settings_link_google),
                    onClick = onLinkGoogle,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy
                )
            }
        }
        if (state.appleAvailable) {
            if (state.hasApple) {
                AppPrimaryButton(
                    text = stringResource(Res.string.settings_unlink_apple),
                    onClick = onUnlinkApple,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy && (state.hasPassword || state.hasGoogle)
                )
            } else {
                AppPrimaryButton(
                    text = stringResource(Res.string.settings_link_apple),
                    onClick = onLinkApple,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isBusy
                )
            }
        }
        if (!state.hasPassword) {
            AppPrimaryButton(
                text = stringResource(Res.string.settings_set_password),
                onClick = onSetPassword,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isBusy
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.settings_hide_my_email_hint),
                style = MaterialTheme.typography.bodySmall,
                color = neubrutalMutedOnSurface()
            )
        }
    }
}
