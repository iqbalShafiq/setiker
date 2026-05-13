package data.auth.model

import domain.model.User
import domain.model.UserRole
import kotlinx.datetime.Instant

 fun UserDto.toDomainModel(): User {
     return User(
         id = id,
         email = email,
         username = username,
         name = name,
         role = UserRole(id = roleId, name = role ?: "user"),
         isActive = isActive,
         createdAt = Instant.parse(createdAt).toEpochMilliseconds()
     )
 }
