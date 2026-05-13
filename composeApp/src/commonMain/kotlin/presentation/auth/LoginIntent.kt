package presentation.auth

sealed class LoginIntent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data object Submit : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object DismissError : LoginIntent()
}