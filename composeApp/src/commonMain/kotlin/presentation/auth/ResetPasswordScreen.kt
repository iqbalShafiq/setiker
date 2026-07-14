package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
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
import presentation.components.AppPasswordTextField
import presentation.components.AppPrimaryButton
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_invalid_reset_token
import setiker.composeapp.generated.resources.reset_password_confirm_label
import setiker.composeapp.generated.resources.reset_password_submit
import setiker.composeapp.generated.resources.reset_password_subtitle
import setiker.composeapp.generated.resources.reset_password_success
import setiker.composeapp.generated.resources.reset_password_title
import setiker.composeapp.generated.resources.settings_password_mismatch
import setiker.composeapp.generated.resources.login_password_label
import setiker.composeapp.generated.resources.login_password_placeholder

data class ResetPasswordState(
    val token: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: UiText? = null
)

class ResetPasswordViewModel(
    private val authApiService: AuthApiService
) : ViewModel() {
    private val _state = MutableStateFlow(ResetPasswordState())
    val state: StateFlow<ResetPasswordState> = _state.asStateFlow()

    fun setToken(token: String) {
        _state.update { it.copy(token = token) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password, error = null) }
    }

    fun updateConfirmPassword(password: String) {
        _state.update { it.copy(confirmPassword = password, error = null) }
    }

    fun submit() {
        val current = _state.value
        if (current.token.isBlank()) {
            _state.update {
                it.copy(error = UiText.StringRes(Res.string.error_auth_invalid_reset_token))
            }
            return
        }
        if (current.password != current.confirmPassword) {
            _state.update {
                it.copy(error = UiText.StringRes(Res.string.settings_password_mismatch))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching {
                authApiService.resetPassword(current.token, current.password)
            }.onSuccess {
                _state.update { it.copy(isLoading = false, success = true) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.toUiText(Res.string.error_auth_invalid_reset_token)
                    )
                }
            }
        }
    }
}

@Composable
fun ResetPasswordScreenRoot(
    token: String,
    onDone: () -> Unit,
    viewModel: ResetPasswordViewModel = koinViewModel()
) {
    LaunchedEffectToken(token, viewModel)
    val state by viewModel.state.collectAsState()
    ResetPasswordScreen(
        state = state,
        onPasswordChange = viewModel::updatePassword,
        onConfirmChange = viewModel::updateConfirmPassword,
        onSubmit = viewModel::submit,
        onDone = onDone
    )
}

@Composable
private fun LaunchedEffectToken(token: String, viewModel: ResetPasswordViewModel) {
    androidx.compose.runtime.LaunchedEffect(token) {
        viewModel.setToken(token)
    }
}

@Composable
fun ResetPasswordScreen(
    state: ResetPasswordState,
    onPasswordChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(Res.string.reset_password_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = neubrutalOnSurface()
        )
        Text(
            text = stringResource(Res.string.reset_password_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = neubrutalSubtleOnSurface()
        )
        if (state.success) {
            Text(
                text = stringResource(Res.string.reset_password_success),
                style = MaterialTheme.typography.bodyLarge,
                color = neubrutalOnSurface()
            )
            AppPrimaryButton(
                text = "OK",
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            AppPasswordTextField(
                value = state.password,
                onValueChange = onPasswordChange,
                label = stringResource(Res.string.login_password_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                enabled = !state.isLoading,
                imeAction = ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            AppPasswordTextField(
                value = state.confirmPassword,
                onValueChange = onConfirmChange,
                label = stringResource(Res.string.reset_password_confirm_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                enabled = !state.isLoading,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )
            state.error?.let {
                Text(text = it.resolveLocal(), color = MaterialTheme.colorScheme.error)
            }
            AppPrimaryButton(
                text = stringResource(Res.string.reset_password_submit),
                onClick = onSubmit,
                enabled = !state.isLoading &&
                    state.password.length >= 8 &&
                    state.confirmPassword.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
