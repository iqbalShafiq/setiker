package presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import data.auth.AuthApiService
import data.auth.AuthManager
import data.auth.GoogleSignInGateway
import data.auth.model.LoginRequest
import data.auth.model.toDomainModel
import data.remote.ApiException
import domain.error.AppErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import presentation.common.UiText
import presentation.common.toUiText
import setiker.composeapp.generated.resources.Res
import setiker.composeapp.generated.resources.error_auth_login_failed

class LoginViewModel(
    private val authApiService: AuthApiService,
    private val authManager: AuthManager,
    private val googleSignInGateway: GoogleSignInGateway
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginState(googleAvailable = googleSignInGateway.isAvailable())
    )
    val state: StateFlow<LoginState> = _state.asStateFlow()

    private val _effect = MutableStateFlow<LoginEffect?>(null)
    val effect: StateFlow<LoginEffect?> = _effect

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.UpdateEmail -> _state.update {
                it.copy(email = intent.email, isEmailValid = true, error = null)
            }
            is LoginIntent.UpdatePassword -> _state.update {
                it.copy(password = intent.password, isPasswordValid = true, error = null)
            }
            is LoginIntent.Submit -> login()
            is LoginIntent.SignInWithGoogle -> signInWithGoogle()
            is LoginIntent.NavigateToRegister -> {
                _effect.value = LoginEffect.NavigateToRegister
            }
            is LoginIntent.DismissError -> _state.update { it.copy(error = null) }
            is LoginIntent.DismissLinkGoogleDialog -> _state.update {
                it.copy(
                    showLinkGoogleDialog = false,
                    pendingGoogleIdToken = null,
                    linkPassword = "",
                    isLoading = false
                )
            }
            is LoginIntent.UpdateLinkPassword -> _state.update { it.copy(linkPassword = intent.password) }
            is LoginIntent.ConfirmLinkGoogle -> confirmLinkGoogle()
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
                val response = authApiService.login(
                    LoginRequest(email = currentState.email, password = currentState.password)
                )
                persistAuth(response)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_login_failed))
                }
            }
        }
    }

    private fun signInWithGoogle() {
        if (!googleSignInGateway.isAvailable()) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val tokenResult = googleSignInGateway.signIn()
            tokenResult.onFailure { error ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = error.toUiText(Res.string.error_auth_login_failed)
                    )
                }
                return@launch
            }
            val idToken = tokenResult.getOrNull()?.idToken ?: return@launch
            try {
                val response = authApiService.loginWithGoogle(idToken)
                persistAuth(response)
            } catch (e: Exception) {
                if (e is ApiException &&
                    (e.code == AppErrorCode.AuthAccountExistsPassword ||
                        e.subcode == "ACCOUNT_EXISTS_PASSWORD")
                ) {
                    val googleEmail = decodeEmailFromJwt(idToken).orEmpty()
                    _state.update {
                        it.copy(
                            isLoading = false,
                            showLinkGoogleDialog = true,
                            pendingGoogleIdToken = idToken,
                            linkEmail = googleEmail.ifBlank { it.email },
                            email = googleEmail.ifBlank { it.email },
                            error = e.toUiText(Res.string.error_auth_login_failed)
                        )
                    }
                } else {
                    _state.update {
                        it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_login_failed))
                    }
                }
            }
        }
    }

    private fun decodeEmailFromJwt(idToken: String): String? {
        return runCatching {
            val payload = idToken.split('.').getOrNull(1) ?: return null
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            val decoded = padded
                .replace('-', '+')
                .replace('_', '/')
                .let { encoded ->
                    val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
                    val cleaned = encoded.filter { it != '=' }
                    val bytes = mutableListOf<Byte>()
                    var buffer = 0
                    var bits = 0
                    for (ch in cleaned) {
                        val value = alphabet.indexOf(ch)
                        if (value < 0) continue
                        buffer = (buffer shl 6) or value
                        bits += 6
                        if (bits >= 8) {
                            bits -= 8
                            bytes.add(((buffer shr bits) and 0xFF).toByte())
                        }
                    }
                    bytes.toByteArray().decodeToString()
                }
            val emailKey = "\"email\""
            val idx = decoded.indexOf(emailKey)
            if (idx < 0) return null
            val start = decoded.indexOf('"', idx + emailKey.length + 1) + 1
            val end = decoded.indexOf('"', start)
            if (start <= 0 || end <= start) null else decoded.substring(start, end)
        }.getOrNull()
    }

    private fun confirmLinkGoogle() {
        val current = _state.value
        val idToken = current.pendingGoogleIdToken ?: return
        val email = current.linkEmail.ifBlank { current.email }
        if (email.isBlank() || current.linkPassword.isBlank()) return

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val response = authApiService.linkGoogleWithPassword(
                    idToken = idToken,
                    email = email,
                    password = current.linkPassword
                )
                _state.update {
                    it.copy(
                        showLinkGoogleDialog = false,
                        pendingGoogleIdToken = null,
                        linkPassword = ""
                    )
                }
                persistAuth(response)
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = e.toUiText(Res.string.error_auth_login_failed))
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
            val expiresIn = (data.expiresIn ?: 3600).toLong()
            authManager.saveTokens(accessToken, refreshToken ?: "", expiresIn)
            authManager.saveUser(user.toDomainModel())
            _state.update { it.copy(isLoading = false) }
            _effect.value = LoginEffect.NavigateToHome
        } else {
            _state.update {
                it.copy(isLoading = false, error = UiText.StringRes(Res.string.error_auth_login_failed))
            }
        }
    }

    fun clearEffect() {
        _effect.value = null
    }
}
