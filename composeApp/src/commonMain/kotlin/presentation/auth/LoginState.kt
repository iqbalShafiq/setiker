package presentation.auth

import presentation.common.UiText

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true
)