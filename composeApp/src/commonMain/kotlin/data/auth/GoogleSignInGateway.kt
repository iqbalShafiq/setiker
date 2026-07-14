package data.auth

data class GoogleIdTokenResult(
    val idToken: String
)

interface GoogleSignInGateway {
    fun isAvailable(): Boolean
    suspend fun signIn(): Result<GoogleIdTokenResult>
}
