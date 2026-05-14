package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.model.LoginRequest
import data.auth.model.toDomainModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager
) : ViewModel() {
    
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()
    
    private val _effect = MutableStateFlow<LoginEffect?>(null)
    val effect: StateFlow<LoginEffect?> = _effect
    
    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> _state.update { it.copy(email = intent.email, isEmailValid = true, error = null) }
            is LoginIntent.UpdatePassword -> _state.update { it.copy(password = intent.password, isPasswordValid = true, error = null) }
            is LoginIntent.Submit -> login()
            is LoginIntent.NavigateToRegister -> { _effect.value = LoginEffect.NavigateToRegister }
            is LoginIntent.DismissError -> { _state.update { it.copy(error = null) } }
        }
    }
    
    private fun login() {
        val currentState = _state.value
        val isEmailValid = currentState.email.contains("@")
        val isPasswordValid = currentState.password.length >= 6
        
        if (!isEmailValid || !isPasswordValid) {
            _state.update { it.copy(isEmailValid = isEmailValid, isPasswordValid = isPasswordValid) }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                 val response = authApiService.login(LoginRequest(email = currentState.email, password = currentState.password))
                 val data = response.data
                 val user = data?.user
                 val accessToken = data?.accessToken
                 val refreshToken = authApiService.getRefreshToken()
                 if (accessToken != null && user != null) {
                     // Server sends refreshToken via HTTP-only cookie.
                     // We extract it from cookie storage and save it.
                     authManager.saveTokens(accessToken, refreshToken ?: "", 3600)
                     authManager.saveUser(user.toDomainModel())
                     _effect.value = LoginEffect.NavigateToHome
                 } else {
                     _state.update { it.copy(isLoading = false, error = "Login failed") }
                 }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Login failed") }
            }
        }
    }
    
    fun clearEffect() { _effect.value = null }
}