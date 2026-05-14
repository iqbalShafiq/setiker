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
         role = UserRole(
             id = role?.id ?: roleId,
             name = role?.name ?: "user"
         ),
         isActive = isActive,
         createdAt = runCatching { Instant.parse(createdAt).toEpochMilliseconds() }.getOrDefault(0L)
     )
 }
