package presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import presentation.components.AppPasswordTextField
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.theme.ErrorRed
import presentation.theme.neubrutalBorderColor
import presentation.theme.neubrutalCardSurface
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground
import presentation.theme.neubrutalSubtleOnSurface
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun LoginScreenRoot(
    viewModel: LoginViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val effect by viewModel.effect.collectAsState()

    LaunchedEffect(effect) {
        when (effect) {
            is LoginEffect.NavigateToHome -> { viewModel.clearEffect(); onNavigateToHome() }
            is LoginEffect.NavigateToRegister -> { viewModel.clearEffect(); onNavigateToRegister() }
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
    Scaffold(
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = neubrutalOnSurface(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sign in to continue creating awesome stickers",
                    style = MaterialTheme.typography.bodyLarge,
                    color = neubrutalSubtleOnSurface(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppTextField(
                    value = state.email,
                    onValueChange = { onIntent(LoginIntent.UpdateEmail(it)) },
                    label = "Email",
                    placeholder = "your@email.com",
                    isError = !state.isEmailValid,
                    supportingText = if (!state.isEmailValid) {
                        { Text("Please enter a valid email address", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AppPasswordTextField(
                    value = state.password,
                    onValueChange = { onIntent(LoginIntent.UpdatePassword(it)) },
                    label = "Password",
                    placeholder = "Min 6 characters",
                    isError = !state.isPasswordValid,
                    supportingText = if (!state.isPasswordValid) {
                        { Text("Password must be at least 6 characters", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    imeAction = ImeAction.Done,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Error Message
            AnimatedVisibility(
                visible = state.error != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                ErrorMessageBox(
                    message = state.error ?: "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppPrimaryButton(
                text = if (state.isLoading) "Signing in..." else "Sign In",
                onClick = { onIntent(LoginIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = { onIntent(LoginIntent.NavigateToRegister) },
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalSubtleOnSurface()
                )
                Text(
                    text = "Register",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// MARK: - Previews

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

@Preview
@Composable
private fun LoginScreenLoadingPreview() {
    MaterialTheme {
        LoginScreen(
            state = LoginState(
                email = "user@example.com",
                password = "password123",
                isLoading = true
            ),
            onIntent = {}
        )
    }
}

@Preview
@Composable
private fun LoginScreenErrorPreview() {
    MaterialTheme {
        LoginScreen(
            state = LoginState(
                email = "invalid-email",
                password = "short",
                isEmailValid = false,
                isPasswordValid = false,
                error = "Invalid credentials"
            ),
            onIntent = {}
        )
    }
}

@Composable
private fun ErrorMessageBox(
    message: String,
    modifier: Modifier = Modifier
) {
    val surfaceColor = neubrutalCardSurface()
    val borderColor = neubrutalBorderColor()
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(surfaceColor)
            .border(
                width = 2.dp,
                color = ErrorRed.copy(alpha = 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = ErrorRed,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = ErrorRed,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
