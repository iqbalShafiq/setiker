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

@Serializable
data class AuthResponse(
    val success: Boolean,
    val data: UserDto? = null,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val expiresIn: Long? = null
)

@Serializable
 data class UserDto(
     val id: String,
     val email: String,
     val username: String,
     val name: String? = null,
     val roleId: String,
     val role: String? = null,
     val isActive: Boolean,
     val createdAt: String
 )

@Serializable
data class UserProfileResponse(
    val success: Boolean,
    val data: UserDto? = null
)
