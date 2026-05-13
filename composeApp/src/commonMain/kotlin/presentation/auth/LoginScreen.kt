package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import presentation.components.AppPasswordTextField
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@Composable
fun LoginScreen(
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
    
    Scaffold(
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            AppTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(LoginIntent.UpdateEmail(it)) },
                label = "Email",
                placeholder = "your@email.com",
                isError = !state.isEmailValid,
                supportingText = if (!state.isEmailValid) {
                    { Text("Invalid email", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            AppPasswordTextField(
                value = state.password,
                onValueChange = { viewModel.onIntent(LoginIntent.UpdatePassword(it)) },
                label = "Password",
                placeholder = "Min 6 characters",
                isError = !state.isPasswordValid,
                supportingText = if (!state.isPasswordValid) {
                    { Text("Min 6 characters", color = MaterialTheme.colorScheme.error) }
                } else null,
                imeAction = ImeAction.Done,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (state.error != null) {
                Text(
                    text = state.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            AppPrimaryButton(
                text = if (state.isLoading) "Logging in..." else "Login",
                onClick = { viewModel.onIntent(LoginIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { viewModel.onIntent(LoginIntent.NavigateToRegister) }) {
                Text(
                    text = "Don't have an account? Register",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
