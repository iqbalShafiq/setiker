package data.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String,
    val name: String? = null
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

/**
 * Auth response structure matching OpenAPI spec:
 * {
 *   "success": true,
 *   "data": {
 *     "user": { ... },
 *     "tokens": {
 *       "accessToken": "...",
 *       "refreshToken": "...",
 *       "expiresIn": 3600
 *     }
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
    val user: UserDto,
    val tokens: AuthTokensDto
)

@Serializable
data class AuthTokensDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)

@Serializable
data class UserDto(
    val id: String,
    val email: String,
    val username: String,
    val name: String? = null,
    val roleId: String,
    val role: RoleDto? = null,
    val isActive: Boolean,
    val createdAt: String
)

@Serializable
data class RoleDto(
    val id: String,
    val name: String
)

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val data: UserDto? = null
)
