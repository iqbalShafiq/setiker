package data.auth

/** Android does not offer Sign in with Apple. */
class UnavailableAppleSignInGateway : AppleSignInGateway {
    override fun isAvailable(): Boolean = false

    override suspend fun signIn(): Result<AppleIdTokenResult> =
        Result.failure(UnsupportedOperationException("Apple Sign-In is not available on Android"))
}
