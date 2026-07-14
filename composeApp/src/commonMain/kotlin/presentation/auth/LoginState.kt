package presentation.auth

import presentation.common.UiText

data class LoginState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val isEmailValid: Boolean = true,
    val isPasswordValid: Boolean = true,
    val googleAvailable: Boolean = false,
    val showLinkGoogleDialog: Boolean = false,
    val pendingGoogleIdToken: String? = null,
    val linkPassword: String = "",
    val linkEmail: String = ""
)

sealed class LoginIntent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data object Submit : LoginIntent()
    data object SignInWithGoogle : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object DismissError : LoginIntent()
    data object DismissLinkGoogleDialog : LoginIntent()
    data class UpdateLinkPassword(val password: String) : LoginIntent()
    data object ConfirmLinkGoogle : LoginIntent()
}
