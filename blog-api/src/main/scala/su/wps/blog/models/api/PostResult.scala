package su.wps.blog.models.api

import java.time.ZonedDateTime

final case class PostResult(name: String, text: String, created_at: ZonedDateTime)
