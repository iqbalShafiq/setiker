package presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import presentation.common.UiText
import presentation.common.resolveLocal
import presentation.components.AppIllustration
import presentation.components.AppIllustrationImage
import presentation.components.AppPasswordTextField
import presentation.components.AppTextField
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalSubtleOnSurface
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.login_email_invalid
import setiker.composeapp.generated.resources.login_email_label
import setiker.composeapp.generated.resources.login_email_placeholder
import setiker.composeapp.generated.resources.login_password_invalid
import setiker.composeapp.generated.resources.login_password_label
import setiker.composeapp.generated.resources.login_password_placeholder
import setiker.composeapp.generated.resources.register_confirm_password_label
import setiker.composeapp.generated.resources.register_confirm_password_placeholder
import setiker.composeapp.generated.resources.register_creating_account
import setiker.composeapp.generated.resources.register_name_label
import setiker.composeapp.generated.resources.register_name_placeholder
import setiker.composeapp.generated.resources.register_password_mismatch
import setiker.composeapp.generated.resources.register_sign_in_cta
import setiker.composeapp.generated.resources.register_submit
import setiker.composeapp.generated.resources.register_subtitle
import setiker.composeapp.generated.resources.register_title
import setiker.composeapp.generated.resources.register_username_label
import setiker.composeapp.generated.resources.register_username_placeholder
import setiker.composeapp.generated.resources.register_username_required

@Composable
fun RegisterScreenRoot(
    viewModel: RegisterViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState()

    LaunchedEffect(effect) {
        when (effect) {
            is RegisterEffect.NavigateToHome -> { viewModel.clearEffect(); onNavigateToHome() }
            is RegisterEffect.NavigateToLogin -> { viewModel.clearEffect(); onNavigateToLogin() }
            null -> {}
        }
    }

    RegisterScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onIntent: (RegisterIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    AuthFormScaffold(
        modifier = modifier,
        title = stringResource(Res.string.register_title),
        isLoading = state.isLoading,
        loadingStatusText = stringResource(Res.string.register_creating_account),
        onPrimaryAction = { onIntent(RegisterIntent.Submit) },
        primaryFabIcon = Icons.Filled.PersonAdd,
        primaryFabContentDescription = stringResource(Res.string.register_submit),
        onSecondaryAction = { onIntent(RegisterIntent.NavigateToLogin) },
        secondaryActionIcon = Icons.Filled.Login,
        secondaryActionContentDescription = stringResource(Res.string.register_sign_in_cta)
    ) {
        AppIllustrationImage(
                illustration = AppIllustration.AuthCloud,
                modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(Res.string.register_title),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = neubrutalOnSurface(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(Res.string.register_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = neubrutalSubtleOnSurface(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(RegisterIntent.UpdateName(it)) },
                    label = stringResource(Res.string.register_name_label),
                    placeholder = stringResource(Res.string.register_name_placeholder),
                    imeAction = ImeAction.Next,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = state.username,
                    onValueChange = { onIntent(RegisterIntent.UpdateUsername(it)) },
                    label = stringResource(Res.string.register_username_label),
                    placeholder = stringResource(Res.string.register_username_placeholder),
                    isError = !state.isUsernameValid,
                    supportingText = if (!state.isUsernameValid) {
                        { Text(stringResource(Res.string.register_username_required), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    imeAction = ImeAction.Next,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = state.email,
                    onValueChange = { onIntent(RegisterIntent.UpdateEmail(it)) },
                    label = stringResource(Res.string.login_email_label),
                    placeholder = stringResource(Res.string.login_email_placeholder),
                    isError = !state.isEmailValid,
                    supportingText = if (!state.isEmailValid) {
                        { Text(stringResource(Res.string.login_email_invalid), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AppPasswordTextField(
                    value = state.password,
                    onValueChange = { onIntent(RegisterIntent.UpdatePassword(it)) },
                    label = stringResource(Res.string.login_password_label),
                    placeholder = stringResource(Res.string.login_password_placeholder),
                    isError = !state.isPasswordValid,
                    supportingText = if (!state.isPasswordValid) {
                        { Text(stringResource(Res.string.login_password_invalid), color = MaterialTheme.colorScheme.error) }
                    } else null,
                    imeAction = ImeAction.Next,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )

                AppPasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = { onIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                    label = stringResource(Res.string.register_confirm_password_label),
                    placeholder = stringResource(Res.string.register_confirm_password_placeholder),
                    imeAction = ImeAction.Done,
                    enabled = !state.isLoading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            AnimatedVisibility(
                visible = state.error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AuthErrorMessageBox(
                    message = state.error?.resolveLocal().orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Preview
@Composable
private fun RegisterScreenPreview() {
    MaterialTheme {
        RegisterScreen(
            state = RegisterState(
                name = "John Doe",
                username = "johndoe",
                email = "john@example.com",
                password = "password123",
                confirmPassword = "password123"
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun RegisterScreenLoadingPreview() {
    MaterialTheme {
        RegisterScreen(
            state = RegisterState(
                name = "John Doe",
                username = "johndoe",
                email = "john@example.com",
                password = "password123",
                confirmPassword = "password123",
                isLoading = true
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun RegisterScreenErrorPreview() {
    MaterialTheme {
        RegisterScreen(
            state = RegisterState(
                email = "invalid-email",
                password = "pass",
                confirmPassword = "different",
                error = UiText.StringRes(Res.string.register_password_mismatch)
            ),
            onIntent = {}
        )
    }
}
