package data.auth.model

import domain.model.User
import domain.model.UserRole
import kotlinx.datetime.Instant

 fun UserDto.toDomainModel(): User {
     return User(
         id = id,
         email = email,
         username = username,
         name = displayName,
         role = UserRole(
             id = "", // Role ID not provided in auth response
             name = role ?: "user"
         ),
         isActive = isActive ?: true,
         createdAt = createdAt?.let { runCatching { Instant.parse(it).toEpochMilliseconds() }.getOrDefault(0L) } ?: 0L,
         followerCount = followerCount ?: 0,
         followingCount = followingCount ?: 0,
         totalPackDownloads = totalPackDownloads ?: 0
     )
 }
