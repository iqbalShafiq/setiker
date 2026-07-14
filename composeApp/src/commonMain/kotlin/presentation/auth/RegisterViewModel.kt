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
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_register_failed
import setiker.composeapp.generated.resources.register_password_mismatch

data class RegisterState(
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val isUsernameValid: Boolean = true,
    val googleAvailable: Boolean = false,
    val appleAvailable: Boolean = false,
    val oneTapAttempted: Boolean = false
)

sealed class RegisterIntent {
    data class UpdateName(val name: String) : RegisterIntent()
    data class UpdateUsername(val username: String) : RegisterIntent()
    data class UpdateEmail(val email: String) : RegisterIntent()
    data class UpdatePassword(val password: String) : RegisterIntent()
    data class UpdateConfirmPassword(val confirmPassword: String) : RegisterIntent()
    data object Submit : RegisterIntent()
    data object SignInWithGoogle : RegisterIntent()
    data object SignInWithApple : RegisterIntent()
    data object TryOneTap : RegisterIntent()
    data object NavigateToLogin : RegisterIntent()
}

sealed class RegisterEffect {
    data object NavigateToHome : RegisterEffect()
    data object NavigateToLogin : RegisterEffect()
}

class RegisterViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager,
    private val googleSignInGateway: data.auth.GoogleSignInGateway,
    private val appleSignInGateway: data.auth.AppleSignInGateway
) : ViewModel() {
    
    private val _state = MutableStateFlow(
        RegisterState(
            googleAvailable = googleSignInGateway.isAvailable(),
            appleAvailable = appleSignInGateway.isAvailable()
        )
    )
    val state: StateFlow<RegisterState> = _state.asStateFlow()
    private val _effect = MutableStateFlow<RegisterEffect?>(null)
    val effect: StateFlow<RegisterEffect?> = _effect
    
    fun onIntent(intent: RegisterIntent) {
        when (intent) {
            is RegisterIntent.UpdateName -> _state.update { it.copy(name = intent.name, error = null) }
            is RegisterIntent.UpdateUsername -> _state.update { it.copy(username = intent.username, error = null, isUsernameValid = true) }
            is RegisterIntent.UpdateEmail -> _state.update { it.copy(email = intent.email, error = null, isEmailValid = true) }
            is RegisterIntent.UpdatePassword -> _state.update { it.copy(password = intent.password, error = null, isPasswordValid = true) }
            is RegisterIntent.UpdateConfirmPassword -> _state.update { it.copy(confirmPassword = intent.confirmPassword, error = null) }
            is RegisterIntent.Submit -> register()
            is RegisterIntent.SignInWithGoogle -> signInWithGoogle(data.auth.GoogleSignInMode.Button)
            is RegisterIntent.SignInWithApple -> signInWithApple()
            is RegisterIntent.TryOneTap -> tryOneTap()
            is RegisterIntent.NavigateToLogin -> _effect.value = RegisterEffect.NavigateToLogin
        }
    }

    private fun tryOneTap() {
        if (!googleSignInGateway.isAvailable() || _state.value.oneTapAttempted) return
        _state.update { it.copy(oneTapAttempted = true) }
        viewModelScope.launch {
            val tokenResult = googleSignInGateway.signIn(data.auth.GoogleSignInMode.OneTap)
            tokenResult.onFailure { return@launch }
            val idToken = tokenResult.getOrNull()?.idToken ?: return@launch
            _state.update { it.copy(isLoading = true, error = null) }
            completeOAuthGoogle(idToken)
        }
    }

    private fun signInWithGoogle(mode: data.auth.GoogleSignInMode) {
        if (!googleSignInGateway.isAvailable()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tokenResult = googleSignInGateway.signIn(mode)
            tokenResult.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.toUiText(Res.string.error_auth_register_failed))
                }
                return@launch
            }
            val idToken = tokenResult.getOrNull()?.idToken ?: return@launch
            completeOAuthGoogle(idToken)
        }
    }

    private suspend fun completeOAuthGoogle(idToken: String) {
        try {
            val response = authApiService.loginWithGoogle(idToken)
            persistAuth(response)
        } catch (e: Exception) {
            _state.update {
                it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_register_failed))
            }
        }
    }

    private fun signInWithApple() {
        if (!appleSignInGateway.isAvailable()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tokenResult = appleSignInGateway.signIn()
            tokenResult.onFailure { error ->
                _state.update {
                    it.copy(isLoading = false, error = error.toUiText(Res.string.error_auth_register_failed))
                }
                return@launch
            }
            val idToken = tokenResult.getOrNull()?.idToken ?: return@launch
            try {
                val response = authApiService.loginWithApple(idToken)
                persistAuth(response)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_register_failed))
                }
            }
        }
    }

    private suspend fun persistAuth(response: data.auth.model.AuthResponse) {
        val data = response.data
        val user = data?.user
        val accessToken = data?.accessToken
        val refreshToken = authApiService.getRefreshToken()
        if (accessToken != null && user != null) {
            authManager.saveTokens(accessToken, refreshToken ?: "", (data.expiresIn ?: 3600).toLong())
            authManager.saveUser(user.toDomainModel())
            _state.update { it.copy(isLoading = false) }
            _effect.value = RegisterEffect.NavigateToHome
        } else {
            _state.update {
                it.copy(isLoading = false, error = UiText.StringRes(Res.string.error_auth_register_failed))
            }
        }
    }
    
    private fun register() {
        val s = _state.value
        val isEmailValid = s.email.contains("@")
        val isPasswordValid = s.password.length >= 6
        val isUsernameValid = s.username.isNotBlank()
        if (!isEmailValid || !isPasswordValid || !isUsernameValid) {
            _state.update {
                it.copy(
                    isEmailValid = isEmailValid,
                    isPasswordValid = isPasswordValid,
                    isUsernameValid = isUsernameValid
                )
            }
            return
        }
        if (s.password != s.confirmPassword) {
            _state.update { it.copy(error = UiText.StringRes(Res.string.register_password_mismatch)) }
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
                        displayName = s.name.takeIf { it.isNotBlank() }
                    )
                )
                val data = response.data
                val user = data?.user
                val accessToken = data?.accessToken
                val refreshToken = authApiService.getRefreshToken()
                if (accessToken != null && user != null) {
                    val expiresIn = (data.expiresIn ?: 3600).toLong()
                    authManager.saveTokens(accessToken, refreshToken ?: "", expiresIn)
                    authManager.saveUser(user.toDomainModel())
                    _effect.value = RegisterEffect.NavigateToHome
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = UiText.StringRes(Res.string.error_auth_register_failed)
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_register_failed))
                }
            }
        }
    }
    
    fun clearEffect() { _effect.value = null }
}
