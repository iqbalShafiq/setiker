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
                modifier = Modifier.padding(top = 32.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = neubrutalOnSurface(),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Join us and start creating your sticker packs",
                    style = MaterialTheme.typography.bodyLarge,
                    color = neubrutalSubtleOnSurface(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Form
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AppTextField(
                    value = state.name,
                    onValueChange = { onIntent(RegisterIntent.UpdateName(it)) },
                    label = "Name (Optional)",
                    placeholder = "Your name",
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = state.username,
                    onValueChange = { onIntent(RegisterIntent.UpdateUsername(it)) },
                    label = "Username",
                    placeholder = "Choose a username",
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AppTextField(
                    value = state.email,
                    onValueChange = { onIntent(RegisterIntent.UpdateEmail(it)) },
                    label = "Email",
                    placeholder = "your@email.com",
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AppPasswordTextField(
                    value = state.password,
                    onValueChange = { onIntent(RegisterIntent.UpdatePassword(it)) },
                    label = "Password",
                    placeholder = "Min 6 characters",
                    imeAction = ImeAction.Next,
                    modifier = Modifier.fillMaxWidth()
                )

                AppPasswordTextField(
                    value = state.confirmPassword,
                    onValueChange = { onIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                    label = "Confirm Password",
                    placeholder = "Re-enter password",
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
                text = if (state.isLoading) "Creating account..." else "Create Account",
                onClick = { onIntent(RegisterIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            TextButton(
                onClick = { onIntent(RegisterIntent.NavigateToLogin) },
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = neubrutalSubtleOnSurface()
                )
                Text(
                    text = "Sign In",
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
                error = "Passwords don't match"
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
