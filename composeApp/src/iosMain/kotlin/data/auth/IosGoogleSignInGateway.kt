package data.auth

import platform.Foundation.NSBundle

class IosGoogleSignInGateway : GoogleSignInGateway {

    override fun isAvailable(): Boolean =
        IosGoogleSignInIntegration.isHandlerRegistered() && googleClientIdsConfigured()

    override suspend fun signIn(mode: GoogleSignInMode): Result<GoogleIdTokenResult> {
        if (!isAvailable()) {
            return Result.failure(
                IllegalStateException("Google Sign-In is not configured (GIDClientID / GIDServerClientID in Info.plist)")
            )
        }
        val idToken = IosGoogleSignInIntegration.signIn()
            ?: return Result.failure(IllegalStateException("Google Sign-In was cancelled or failed"))
        return Result.success(GoogleIdTokenResult(idToken))
    }

    private fun googleClientIdsConfigured(): Boolean {
        val bundle = NSBundle.mainBundle
        val clientId = bundle.objectForInfoDictionaryKey("GIDClientID") as? String
        val serverClientId = bundle.objectForInfoDictionaryKey("GIDServerClientID") as? String
        return !clientId.isNullOrBlank() && !serverClientId.isNullOrBlank()
    }
}
