package su.wps.blog.models.api

import su.wps.blog.models.domain.PostId

import java.time.ZonedDateTime

final case class ListPostResult(
  id: PostId,
  name: String,
  shortText: String,
  createdAt: ZonedDateTime
)
