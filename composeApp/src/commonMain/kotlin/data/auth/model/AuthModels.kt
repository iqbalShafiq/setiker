package data.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val displayName: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class DeleteAccountRequest(
    val currentPassword: String? = null
)

@Serializable
data class UpdateProfileRequest(
    val displayName: String? = null,
    val username: String? = null
)

@Serializable
data class GoogleIdTokenRequest(
    val idToken: String
)

@Serializable
data class AppleIdTokenRequest(
    val idToken: String
)

@Serializable
data class LinkGoogleWithPasswordRequest(
    val idToken: String,
    val email: String,
    val password: String
)

@Serializable
data class LinkAppleWithPasswordRequest(
    val idToken: String,
    val email: String,
    val password: String
)

@Serializable
data class SetPasswordRequest(
    val newPassword: String
)

@Serializable
data class ForgotPasswordRequest(
    val email: String
)

@Serializable
data class ResetPasswordRequest(
    val token: String,
    val newPassword: String
)

/**
 * Actual API response structure (from real implementation):
 * Login/Register:
 * {
 *   "success": true,
 *   "data": {
 *     "user": {
 *       "id": "...",
 *       "email": "...",
 *       "username": "...",
 *       "displayName": null,
 *       "role": "user"
 *     },
 *     "accessToken": "..."
 *   }
 * }
 *
 * Refresh:
 * {
 *   "success": true,
 *   "data": {
 *     "accessToken": "..."
 *   }
 * }
 */
@Serializable
data class AuthResponse(
    val success: Boolean,
    val data: AuthDataDto? = null
)

@Serializable
data class AuthDataDto(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val expiresIn: Int? = null
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val displayName: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null,
    val emailVerified: Boolean? = null,
    val hasPassword: Boolean? = null,
    val authProviders: List<String>? = null,
    val createdAt: String? = null,
    val followerCount: Int? = null,
    val followingCount: Int? = null,
    val subscriptionTier: String? = null,
    val totalPackDownloads: Int? = null
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val data: UserDto? = null
)
