package su.wps.blog.models.domain

import java.time.ZonedDateTime

final case class Page(
  url: String,
  title: String,
  content: String,
  createdAt: ZonedDateTime,
  id: Option[PageId] = None
)

final case class PageId(value: Int) extends AnyVal
