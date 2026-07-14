package data.auth

data class GoogleIdTokenResult(
    val idToken: String
)

enum class GoogleSignInMode {
    /** Credential Manager bottomsheet for returning/authorized accounts. */
    OneTap,
    /** Explicit Sign in with Google button flow. */
    Button
}

interface GoogleSignInGateway {
    fun isAvailable(): Boolean
    suspend fun signIn(mode: GoogleSignInMode = GoogleSignInMode.Button): Result<GoogleIdTokenResult>
}
