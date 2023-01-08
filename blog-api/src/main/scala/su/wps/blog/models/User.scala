package su.wps.blog.models

import java.time.ZonedDateTime

case class User(
  username: String,
  email: String,
  password: String,
  isActive: Boolean,
  isAdmin: Boolean,
  createdAt: ZonedDateTime,
  id: Option[UserId] = None
)

case class UserId(value: Int) extends AnyVal
