package presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.jetbrains.compose.resources.stringResource
import presentation.common.resolveLocal
import presentation.common.UiText
import presentation.theme.neubrutalMutedOnSurface
import presentation.theme.neubrutalOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.settings_delete_confirm_action
import setiker.composeapp.generated.resources.settings_delete_confirm_message
import setiker.composeapp.generated.resources.settings_delete_confirm_title
import setiker.composeapp.generated.resources.settings_delete_password_label
import setiker.composeapp.generated.resources.settings_delete_phrase_hint
import setiker.composeapp.generated.resources.settings_delete_phrase_label
import setiker.composeapp.generated.resources.settings_deleting_account

const val DELETE_ACCOUNT_PHRASE = "Delete Account"

@Composable
fun DeleteAccountConfirmDialog(
    password: String,
    confirmPhrase: String,
    onPasswordChange: (String) -> Unit,
    onPhraseChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false,
    error: UiText? = null
) {
    val phraseMatches = confirmPhrase == DELETE_ACCOUNT_PHRASE
    val canConfirm = password.isNotBlank() && phraseMatches && !isLoading

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        AppDialogWidthContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
            ) {
                Text(
                    text = stringResource(Res.string.settings_delete_confirm_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = neubrutalOnSurface()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(Res.string.settings_delete_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalMutedOnSurface()
                )
                Spacer(modifier = Modifier.height(16.dp))
                AppPasswordTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = stringResource(Res.string.settings_delete_password_label),
                    placeholder = stringResource(Res.string.settings_delete_password_label),
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(12.dp))
                AppTextField(
                    value = confirmPhrase,
                    onValueChange = onPhraseChange,
                    label = stringResource(Res.string.settings_delete_phrase_label),
                    placeholder = stringResource(Res.string.settings_delete_phrase_hint),
                    enabled = !isLoading,
                    isError = confirmPhrase.isNotBlank() && !phraseMatches,
                    supportingText = if (confirmPhrase.isNotBlank() && !phraseMatches) {
                        { Text(stringResource(Res.string.settings_delete_phrase_hint)) }
                    } else {
                        null
                    }
                )
                error?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it.resolveLocal(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                AppPrimaryButton(
                    text = if (isLoading) {
                        stringResource(Res.string.settings_deleting_account)
                    } else {
                        stringResource(Res.string.settings_delete_confirm_action)
                    },
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = canConfirm
                )
                Spacer(modifier = Modifier.height(10.dp))
                AppSecondaryButton(
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        }
    }
}
