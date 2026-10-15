package presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import setiker.composeapp.generated.resources.cancel
import setiker.composeapp.generated.resources.login_apple
import setiker.composeapp.generated.resources.login_email_invalid
import setiker.composeapp.generated.resources.login_email_label
import setiker.composeapp.generated.resources.login_email_placeholder
import setiker.composeapp.generated.resources.login_forgot_password
import setiker.composeapp.generated.resources.login_forgot_password_hint
import setiker.composeapp.generated.resources.login_google
import setiker.composeapp.generated.resources.login_google_or
import setiker.composeapp.generated.resources.login_link_google_confirm
import setiker.composeapp.generated.resources.login_link_apple_message
import setiker.composeapp.generated.resources.login_link_apple_title
import setiker.composeapp.generated.resources.login_link_google_message
import setiker.composeapp.generated.resources.login_link_google_title
import setiker.composeapp.generated.resources.login_password_invalid
import setiker.composeapp.generated.resources.login_password_label
import setiker.composeapp.generated.resources.login_password_placeholder
import setiker.composeapp.generated.resources.login_register_cta
import setiker.composeapp.generated.resources.login_signing_in
import setiker.composeapp.generated.resources.login_submit
import setiker.composeapp.generated.resources.login_welcome_subtitle
import setiker.composeapp.generated.resources.login_welcome_title

@Composable
fun LoginScreenRoot(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onIntent(LoginIntent.TryOneTap)
    }

    LaunchedEffect(effect) {
        when (effect) {
            is LoginEffect.NavigateToHome -> { viewModel.clearEffect(); onNavigateToHome() }
            is LoginEffect.NavigateToRegister -> { viewModel.clearEffect(); onNavigateToRegister() }
            is LoginEffect.NavigateToForgotPassword -> {
                viewModel.clearEffect()
                onNavigateToForgotPassword()
            }
            null -> {}
        }
    }

    LoginScreen(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier
    )
}

@Composable
fun LoginScreen(
    state: LoginState,
    onIntent: (LoginIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    AuthFormScaffold(
        modifier = modifier,
        title = stringResource(Res.string.login_submit),
        isLoading = state.isLoading,
        loadingStatusText = stringResource(Res.string.login_signing_in),
        onPrimaryAction = { onIntent(LoginIntent.Submit) },
        primaryFabIcon = Icons.Filled.Login,
        primaryFabContentDescription = stringResource(Res.string.login_submit),
        onSecondaryAction = { onIntent(LoginIntent.NavigateToRegister) },
        secondaryActionIcon = Icons.Filled.PersonAdd,
        secondaryActionContentDescription = stringResource(Res.string.login_register_cta)
    ) {
        AppIllustrationImage(
            illustration = AppIllustration.AuthCloud,
            modifier = Modifier.padding(top = 24.dp, bottom = 20.dp)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 28.dp)
        ) {
            Text(
                text = stringResource(Res.string.login_welcome_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = neubrutalOnSurface(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.login_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = neubrutalSubtleOnSurface(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (state.appleAvailable) {
            OutlinedButton(
                onClick = { onIntent(LoginIntent.SignInWithApple) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = stringResource(Res.string.login_apple))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (state.googleAvailable) {
            OutlinedButton(
                onClick = { onIntent(LoginIntent.SignInWithGoogle) },
                enabled = !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = stringResource(Res.string.login_google))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.login_google_or),
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalSubtleOnSurface()
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppTextField(
                value = state.email,
                onValueChange = { onIntent(LoginIntent.UpdateEmail(it)) },
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
                onValueChange = { onIntent(LoginIntent.UpdatePassword(it)) },
                label = stringResource(Res.string.login_password_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                isError = !state.isPasswordValid,
                supportingText = if (!state.isPasswordValid) {
                    { Text(stringResource(Res.string.login_password_invalid), color = MaterialTheme.colorScheme.error) }
                } else null,
                imeAction = ImeAction.Done,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }

        TextButton(
            onClick = { onIntent(LoginIntent.NavigateToForgotPassword) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(Res.string.login_forgot_password))
        }
        Text(
            text = stringResource(Res.string.login_forgot_password_hint),
            style = MaterialTheme.typography.bodySmall,
            color = neubrutalSubtleOnSurface(),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

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

    if (state.showLinkGoogleDialog) {
        LinkPasswordDialog(
            title = stringResource(Res.string.login_link_google_title),
            message = stringResource(Res.string.login_link_google_message),
            email = state.linkEmail,
            password = state.linkPassword,
            confirmLabel = stringResource(Res.string.login_link_google_confirm),
            isLoading = state.isLoading,
            onPasswordChange = { onIntent(LoginIntent.UpdateLinkPassword(it)) },
            onConfirm = { onIntent(LoginIntent.ConfirmLinkGoogle) },
            onDismiss = { onIntent(LoginIntent.DismissLinkGoogleDialog) }
        )
    }

    if (state.showLinkAppleDialog) {
        LinkPasswordDialog(
            title = stringResource(Res.string.login_link_apple_title),
            message = stringResource(Res.string.login_link_apple_message),
            email = state.linkEmail,
            password = state.linkPassword,
            confirmLabel = stringResource(Res.string.login_link_google_confirm),
            isLoading = state.isLoading,
            onPasswordChange = { onIntent(LoginIntent.UpdateLinkPassword(it)) },
            onConfirm = { onIntent(LoginIntent.ConfirmLinkApple) },
            onDismiss = { onIntent(LoginIntent.DismissLinkAppleDialog) }
        )
    }
}

@Composable
private fun LinkPasswordDialog(
    title: String,
    message: String,
    email: String,
    password: String,
    confirmLabel: String,
    isLoading: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = email,
                style = MaterialTheme.typography.bodyMedium,
                color = neubrutalSubtleOnSurface()
            )
            AppPasswordTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = stringResource(Res.string.login_password_label),
                placeholder = stringResource(Res.string.login_password_placeholder),
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedButton(
                onClick = onConfirm,
                enabled = password.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(confirmLabel)
            }
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.cancel))
            }
        }
    }
}

@Preview
@Composable
private fun LoginScreenPreview() {
    MaterialTheme {
        LoginScreen(
            state = LoginState(
                email = "user@example.com",
                password = "password123"
            ),
            onIntent = {}
        )
    }
}
