package domain.model

import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val name: String?,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: Long,
    val followerCount: Int = 0,
    val followingCount: Int = 0
)

@Serializable
data class UserRole(
    val id: String,
    val name: String
)

enum class AuthState {
    UNKNOWN,
    UNAUTHENTICATED,
    AUTHENTICATED
}
