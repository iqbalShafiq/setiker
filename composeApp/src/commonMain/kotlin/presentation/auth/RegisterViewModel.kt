package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.RegisterRequest
import data.auth.model.toDomainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class RegisterIntent {
    data class UpdateName(val name: String) : RegisterIntent()
    data class UpdateUsername(val username: String) : RegisterIntent()
    data class UpdateEmail(val email: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data object Submit : RegisterIntent()
    data object NavigateToLogin : RegisterIntent()
}

sealed class RegisterEffect {
    data object NavigateToHome : RegisterEffect()
    data object NavigateToLogin : RegisterEffect()
}

class RegisterViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(RegisterState())
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    private val _effect = MutableStateFlow<RegisterEffect?>(null)
    val effect: StateFlow<RegisterEffect?> = _effect
    
    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateName -> _state.update { it.copy(name = intent.name) }
            is RegisterIntent.UpdateUsername -> _state.update { it.copy(username = intent.username) }
            is RegisterIntent.UpdateEmail -> _state.update { it.copy(email = intent.email) }
            is RegisterIntent.UpdatePassword -> _state.update { it.copy(password = intent.password) }
            is RegisterIntent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = intent.confirmPassword) }
            is RegisterIntent.Submit -> register()
            is RegisterIntent.NavigateToLogin -> _effect.value = RegisterEffect.NavigateToLogin
        }
    }
    
    private fun register() {
        val s = _state.value
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(error = "Passwords don't match") }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                 val response = authApiService.register(
                     RegisterRequest(
                         email = s.email,
                         username = s.username,
                         password = s.password,
                         name = s.name.takeIf { it.isNotBlank() }
                     )
                 )
                 val tokens = response.data?.tokens
                 val user = response.data?.user
                 if (tokens != null && user != null) {
                     authManager.saveTokens(tokens.accessToken, tokens.refreshToken, tokens.expiresIn)
                     authManager.saveUser(user.toDomainModel())
                     _effect.value = RegisterEffect.NavigateToHome
                 } else {
                     _state.update { it.copy(isLoading = false, error = "Registration failed") }
                 }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Registration failed") }
            }
        }
    }
    
    fun clearEffect() { _effect.value = null }
}
