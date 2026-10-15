package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import presentation.common.UiText
import presentation.common.resolveLocal
import presentation.common.toUiText
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_password_reset_failed
import setiker.composeapp.generated.resources.forgot_password_sending
import setiker.composeapp.generated.resources.forgot_password_submit
import setiker.composeapp.generated.resources.forgot_password_subtitle
import setiker.composeapp.generated.resources.forgot_password_success
import setiker.composeapp.generated.resources.forgot_password_title
import setiker.composeapp.generated.resources.login_email_label
import setiker.composeapp.generated.resources.login_email_placeholder
import setiker.composeapp.generated.resources.login_forgot_password_hint

data class ForgotPasswordState(
    val email: String = "",
    val isLoading: Boolean = false,
    val sent: Boolean = false,
    val error: UiText? = null
)

class ForgotPasswordViewModel(
    private val authApiService: AuthApiService
) : ViewModel() {
    private val _state = MutableStateFlow(ForgotPasswordState())
    val state: StateFlow<ForgotPasswordState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email, error = null) }
    }

    fun submit() {
        val email = _state.value.email.trim()
        if (!email.contains("@")) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { authApiService.forgotPassword(email) }
                .onSuccess {
                    _state.update { it.copy(isLoading = false, sent = true) }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.toUiText(Res.string.error_auth_password_reset_failed)
                        )
                    }
                }
        }
    }
}

@Composable
fun ForgotPasswordScreenRoot(
    onBack: () -> Unit,
    viewModel: ForgotPasswordViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    ForgotPasswordScreen(
        state = state,
        onEmailChange = viewModel::updateEmail,
        onSubmit = viewModel::submit,
        onBack = onBack
    )
}

@Composable
fun ForgotPasswordScreen(
    state: ForgotPasswordState,
    onEmailChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.forgot_password_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Text(
            text = stringResource(Res.string.forgot_password_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalSubtleOnSurface()
        )
        Text(
            text = stringResource(Res.string.login_forgot_password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface()
        )
        if (state.sent) {
            Text(
                text = stringResource(Res.string.forgot_password_success),
                style = MaterialTheme.typography.bodyLarge,
                color = neubrutalOnSurface()
            )
            AppPrimaryButton(
                text = "OK",
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppTextField(
                value = state.email,
                onValueChange = onEmailChange,
                label = stringResource(Res.string.login_email_label),
                placeholder = stringResource(Res.string.login_email_placeholder),
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Done,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let {
                Text(text = it.resolveLocal(), color = MaterialTheme.colorScheme.error)
            }
            AppPrimaryButton(
                text = if (state.isLoading) {
                    stringResource(Res.string.forgot_password_sending)
                } else {
                    stringResource(Res.string.forgot_password_submit)
                },
                onClick = onSubmit,
                enabled = !state.isLoading && state.email.contains("@"),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
