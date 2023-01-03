package su.wps.blog.models

import java.time.ZonedDateTime

case class Config(
  name: String,
  value: String,
  comment: String,
  createdAt: ZonedDateTime,
  id: Option[ConfigId] = None
)

case class ConfigId(value: Int) extends AnyVal
