package data.auth

class IosAppleSignInGateway : AppleSignInGateway {

    override fun isAvailable(): Boolean = IosAppleSignInIntegration.isHandlerRegistered()

    override suspend fun signIn(): Result<AppleIdTokenResult> {
        if (!isAvailable()) {
            return Result.failure(IllegalStateException("Apple Sign-In handler is not registered"))
        }
        val idToken = IosAppleSignInIntegration.signIn()
            ?: return Result.failure(IllegalStateException("Apple Sign-In was cancelled or failed"))
        return Result.success(AppleIdTokenResult(idToken))
    }
}
