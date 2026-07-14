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
    val appleAvailable: Boolean = false,
    val oneTapAttempted: Boolean = false,
    val showLinkGoogleDialog: Boolean = false,
    val showLinkAppleDialog: Boolean = false,
    val pendingGoogleIdToken: String? = null,
    val pendingAppleIdToken: String? = null,
    val linkPassword: String = "",
    val linkEmail: String = ""
)

sealed class LoginIntent {
    data class UpdateEmail(val email: String) : LoginIntent()
    data class UpdatePassword(val password: String) : LoginIntent()
    data object Submit : LoginIntent()
    data object SignInWithGoogle : LoginIntent()
    data object SignInWithApple : LoginIntent()
    data object TryOneTap : LoginIntent()
    data object NavigateToRegister : LoginIntent()
    data object NavigateToForgotPassword : LoginIntent()
    data object DismissError : LoginIntent()
    data object DismissLinkGoogleDialog : LoginIntent()
    data object DismissLinkAppleDialog : LoginIntent()
    data class UpdateLinkPassword(val password: String) : LoginIntent()
    data object ConfirmLinkGoogle : LoginIntent()
    data object ConfirmLinkApple : LoginIntent()
}

sealed class LoginEffect {
    data object NavigateToHome : LoginEffect()
    data object NavigateToRegister : LoginEffect()
    data object NavigateToForgotPassword : LoginEffect()
}
