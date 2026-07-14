package data.auth

class UnavailableGoogleSignInGateway : GoogleSignInGateway {
    override fun isAvailable(): Boolean = false

    override suspend fun signIn(mode: GoogleSignInMode): Result<GoogleIdTokenResult> =
        Result.failure(UnsupportedOperationException("Google Sign-In is not available on this platform"))
}
