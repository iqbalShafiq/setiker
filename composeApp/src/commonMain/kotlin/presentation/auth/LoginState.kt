package presentation.auth

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true
)