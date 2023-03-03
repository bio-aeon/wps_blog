package su.wps.blog.models.domain

import java.time.ZonedDateTime

final case class User(
  username: String,
  email: String,
  password: String,
  isActive: Boolean,
  isAdmin: Boolean,
  createdAt: ZonedDateTime,
  id: Option[UserId] = None
)

final case class UserId(value: Int) extends AnyVal
