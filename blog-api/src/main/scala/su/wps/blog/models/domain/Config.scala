package su.wps.blog.models.domain

import java.time.ZonedDateTime

final case class Config(
  name: String,
  value: String,
  comment: String,
  createdAt: ZonedDateTime,
  id: Option[ConfigId] = None
)

final case class ConfigId(value: Int) extends AnyVal
