package data.auth

data class AppleIdTokenResult(
    val idToken: String
)

interface AppleSignInGateway {
    fun isAvailable(): Boolean
    suspend fun signIn(): Result<AppleIdTokenResult>
}
