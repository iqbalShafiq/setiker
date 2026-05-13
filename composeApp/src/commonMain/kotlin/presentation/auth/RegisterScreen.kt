package presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import presentation.components.AppPasswordTextField
import presentation.components.AppPrimaryButton
import presentation.components.AppTextField
import presentation.theme.neubrutalOnSurface
import presentation.theme.neubrutalScreenBackground

@Composable
fun RegisterScreen(
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
    
    Scaffold(
        containerColor = neubrutalScreenBackground()
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create Account",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = neubrutalOnSurface(),
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            AppTextField(
                value = state.name,
                onValueChange = { viewModel.onIntent(RegisterIntent.UpdateName(it)) },
                label = "Name (Optional)",
                placeholder = "Your name",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppTextField(
                value = state.username,
                onValueChange = { viewModel.onIntent(RegisterIntent.UpdateUsername(it)) },
                label = "Username",
                placeholder = "Choose a username",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppTextField(
                value = state.email,
                onValueChange = { viewModel.onIntent(RegisterIntent.UpdateEmail(it)) },
                label = "Email",
                placeholder = "your@email.com",
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppPasswordTextField(
                value = state.password,
                onValueChange = { viewModel.onIntent(RegisterIntent.UpdatePassword(it)) },
                label = "Password",
                placeholder = "Min 6 characters",
                imeAction = androidx.compose.ui.text.input.ImeAction.Next,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            AppPasswordTextField(
                value = state.confirmPassword,
                onValueChange = { viewModel.onIntent(RegisterIntent.UpdateConfirmPassword(it)) },
                label = "Confirm Password",
                placeholder = "Re-enter password",
                imeAction = androidx.compose.ui.text.input.ImeAction.Done,
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
                text = if (state.isLoading) "Creating account..." else "Register",
                onClick = { viewModel.onIntent(RegisterIntent.Submit) },
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(onClick = { viewModel.onIntent(RegisterIntent.NavigateToLogin) }) {
                Text(
                    text = "Already have an account? Login",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
